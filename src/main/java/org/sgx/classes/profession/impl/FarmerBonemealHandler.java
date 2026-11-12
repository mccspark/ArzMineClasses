package org.sgx.classes.profession.impl;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.gameevent.GameEvent;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FarmerBonemealHandler {

    private static final int MAX_STEPS = 32;

    private FarmerBonemealHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Items.BONE_MEAL)) {
                return InteractionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            if (!isGrowable(state.getBlock())) {
                return InteractionResult.PASS;
            }

            ProfessionData data = ModAttachments.get(serverPlayer);
            if (!data.hasAbility("farmer_bonemeal_instant")) {
                return InteractionResult.PASS;
            }

            ServerLevel serverLevel = (ServerLevel) world;
            boolean done = applyInstant(serverLevel, serverPlayer, pos, state);
            if (!done) {
                return InteractionResult.PASS;
            }

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS;
        });
    }

    /** Культуры, которые «мгновенный рост» растит целиком (не трава, не ламинария и т.п.). */
    public static boolean isGrowable(Block block) {
        if (block instanceof CropBlock || block instanceof StemBlock) {
            return true;
        }
        if (!(block instanceof BonemealableBlock)) {
            return false;
        }
        return "farmersdelight".equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())
                && hasAgeProperty(block.defaultBlockState());
    }

    private static boolean hasAgeProperty(BlockState state) {
        for (Property<?> p : state.getProperties()) {
            if (p instanceof IntegerProperty && p.getName().equals("age")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Мгновенный рост одной костной муки: растит до конца (рис, томат, ростки тыквы/арбуза, обычные культуры),
     * а если росток тыквы/арбуза уже вырос — даёт плод рядом. Возвращает false, если делать было нечего.
     */
    public static boolean applyInstant(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        boolean changed = false;

        if (state.getBlock() instanceof StemBlock && isStemMature(state)) {
            changed = spawnFruit(level, pos, state);
        } else {
            changed = growFully(level, pos);
            // двухблочные культуры (рис: низ + метёлка) — добираем верхний блок тоже
            BlockPos above = pos.above();
            if (isGrowable(level.getBlockState(above).getBlock())) {
                changed |= growFully(level, above);
            }
            BlockPos below = pos.below();
            if (isGrowable(level.getBlockState(below).getBlock())) {
                changed |= growFully(level, below);
            }
        }

        if (changed) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    8, 0.3, 0.3, 0.3, 0.0);
            level.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
        }
        return changed;
    }

    /** Повторяет ванильную костную муку, пока блок остаётся валидной целью (с предохранителем). */
    public static boolean growFully(ServerLevel level, BlockPos pos) {
        boolean changed = false;
        for (int i = 0; i < MAX_STEPS; i++) {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BonemealableBlock target)
                    || !isGrowable(state.getBlock())
                    || !target.isValidBonemealTarget(level, pos, state)) {
                break;
            }
            target.performBonemeal(level, level.getRandom(), pos, state);
            changed = true;
        }
        return changed;
    }

    private static boolean isStemMature(BlockState state) {
        return state.getValue(StemBlock.AGE) >= StemBlock.MAX_AGE;
    }

    /** Плод тыквы/арбуза рядом с выросшим ростком (как при случайном тике), ПКМ костной мукой. */
    public static boolean spawnFruit(ServerLevel level, BlockPos stemPos, BlockState stemState) {
        String path = BuiltInRegistries.BLOCK.getKey(stemState.getBlock()).getPath();
        Block fruit;
        Block attached;
        if (path.equals("pumpkin_stem")) {
            fruit = Blocks.PUMPKIN;
            attached = Blocks.ATTACHED_PUMPKIN_STEM;
        } else if (path.equals("melon_stem")) {
            fruit = Blocks.MELON;
            attached = Blocks.ATTACHED_MELON_STEM;
        } else {
            return false;
        }

        List<Direction> dirs = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
        Collections.shuffle(dirs);
        for (Direction dir : dirs) {
            BlockPos target = stemPos.relative(dir);
            if (!level.getBlockState(target).isAir()) {
                continue;
            }
            BlockState ground = level.getBlockState(target.below());
            if (!(ground.is(Blocks.FARMLAND) || ground.is(BlockTags.DIRT))) {
                continue;
            }
            level.setBlockAndUpdate(target, fruit.defaultBlockState());
            level.setBlockAndUpdate(stemPos, attached.defaultBlockState().setValue(AttachedStemBlock.FACING, dir));
            return true;
        }
        return false;
    }
}
