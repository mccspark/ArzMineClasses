package org.sgx.classes.profession.impl;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.item.HarvestBookItem;
import org.sgx.classes.item.ModItems;
import org.sgx.classes.profession.ProfessionBehavior;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionFeedback;
import org.sgx.classes.profession.ProfessionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FarmerBehavior implements ProfessionBehavior {

    public static final int XP_PER_HARVEST = 20;
    private static final int XP_PER_ANIMAL_KILL = 30;
    private static final float BONE_FROM_ANIMAL_CHANCE = 0.5f;

    static {
        UseBlockCallback.EVENT.register(FarmerBehavior::onUseBlock);
    }

    private static Map<Block, Item> goldenCropMap;

    private static Map<Block, Item> goldenCropMap() {
        if (goldenCropMap == null) {
            Map<Block, Item> map = new HashMap<>();
            map.put(Blocks.WHEAT, ModItems.GOLDEN_WHEAT);
            map.put(Blocks.CARROTS, Items.GOLDEN_CARROT);
            map.put(Blocks.POTATOES, ModItems.GOLDEN_POTATO);
            map.put(Blocks.BEETROOTS, ModItems.GOLDEN_BEETROOT);

            putIfBlockPresent(map, "farmersdelight", "cabbages", ModItems.GOLDEN_CABBAGE);
            putIfBlockPresent(map, "farmersdelight", "onions", ModItems.GOLDEN_ONION);

            goldenCropMap = map;
        }
        return goldenCropMap;
    }

    private static void putIfBlockPresent(Map<Block, Item> map, String namespace, String path, Item goldenItem) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        BuiltInRegistries.BLOCK.get(id).ifPresent(reference -> map.put(reference.value(), goldenItem));
    }

    @Override
    public ProfessionType type() {
        return ProfessionType.FARMER;
    }

    @Override
    public int onBlockBroken(ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block == Blocks.BROWN_MUSHROOM || block == Blocks.RED_MUSHROOM) {
            // обычные грибы: только в счётчик книги, опыта не даём
            HarvestBookItem.recordCrop(player, block);
            return 0;
        }
        if (!(block instanceof CropBlock crop) || !crop.isMaxAge(state)) {
            return 0;
        }

        applyHarvestBonuses(player, world, pos, state, ModAttachments.get(player));
        HarvestBookItem.recordCrop(player, block);
        HarvestBookItem.recordNearbyDrops(player, world, pos);
        return XP_PER_HARVEST;
    }

    /** ПКМ по созревшей культуре: собрать урожай и оставить грядку засаженной (навык farmer_autoplant). */
    private static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || level.isClientSide() || player.isShiftKeyDown()
                || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel world)) {
            return InteractionResult.PASS;
        }

        BlockPos pos = hit.getBlockPos();
        BlockState state = world.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(state)) {
            return InteractionResult.PASS;
        }

        ProfessionData data = ModAttachments.get(serverPlayer);
        if (!data.hasAbility("farmer_autoplant")) {
            return InteractionResult.PASS;
        }

        // базовый дроп культуры (как при обычной ломке; семена возвращаются, поэтому грядка остаётся засаженной)
        LootParams.Builder params = new LootParams.Builder(world)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.TOOL, serverPlayer.getMainHandItem())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, serverPlayer);
        List<ItemStack> drops = state.getDrops(params);
        for (ItemStack drop : drops) {
            dropStack(world, pos, drop);
        }

        applyHarvestBonuses(serverPlayer, world, pos, state, data);
        HarvestBookItem.recordCrop(serverPlayer, state.getBlock());
        HarvestBookItem.recordNearbyDrops(serverPlayer, world, pos);

        world.setBlockAndUpdate(pos, crop.getStateForAge(0));
        world.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);

        ProfessionFeedback.grantXp(serverPlayer, ProfessionType.FARMER, XP_PER_HARVEST);
        return InteractionResult.SUCCESS;
    }

    /** Бонусный дроп (+N% урожая) и золотая версия культуры — общие для ломки и ПКМ-сбора. */
    private static void applyHarvestBonuses(ServerPlayer player, ServerLevel world, BlockPos pos,
                                            BlockState state, ProfessionData data) {
        Block block = state.getBlock();

        double yieldPercent = data.customScalarTotal("farmer.crop_yield_percent");
        if (yieldPercent > 0) {
            float bonusChance = (float) Math.min(0.75, yieldPercent / 100.0);
            if (world.getRandom().nextFloat() < bonusChance) {
                LootParams.Builder params = new LootParams.Builder(world)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                        .withOptionalParameter(LootContextParams.THIS_ENTITY, player);
                for (ItemStack extra : state.getDrops(params)) {
                    dropStack(world, pos, extra);
                }
            }
        }

        // Золотая версия культуры — узел farmer_rare_crop_drop, любая культура из goldenCropMap()
        double rareChance = data.customScalarTotal("farmer.rare_crop_drop_chance_percent");
        if (rareChance > 0) {
            Item goldenVersion = goldenCropMap().get(block);
            if (goldenVersion != null) {
                float chance = (float) Math.min(0.5, rareChance / 100.0);
                if (world.getRandom().nextFloat() < chance) {
                    dropStack(world, pos, new ItemStack(goldenVersion));
                    HarvestBookItem.recordGolden(player, goldenVersion);
                }
            }
        }
    }

    private static void dropStack(ServerLevel world, BlockPos pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        world.addFreshEntity(entity);
    }

    @Override
    public int onPassiveMobKilled(ServerPlayer player, ServerLevel world, LivingEntity mob) {
        ProfessionData data = ModAttachments.get(player);

        double dropPercent = data.customScalarTotal("farmer.animal_kill_drop_percent");
        if (dropPercent > 0) {
            float bonusChance = (float) Math.min(0.75, dropPercent / 100.0);
            if (world.getRandom().nextFloat() < bonusChance) {
                var lootTableKey = mob.getType().getDefaultLootTable();
                if (lootTableKey.isPresent()) {
                    LootParams.Builder params = new LootParams.Builder(world)
                            .withParameter(LootContextParams.ORIGIN, mob.position())
                            .withParameter(LootContextParams.DAMAGE_SOURCE, world.damageSources().playerAttack(player))
                            .withOptionalParameter(LootContextParams.THIS_ENTITY, mob)
                            .withOptionalParameter(LootContextParams.ATTACKING_ENTITY, player);

                    LootTable lootTable = world.getServer().reloadableRegistries().getLootTable(lootTableKey.get());
                    for (ItemStack extra : lootTable.getRandomItems(params.create(LootContextParamSets.ENTITY))) {
                        ItemEntity entity = new ItemEntity(world, mob.getX(), mob.getY(), mob.getZ(), extra);
                        world.addFreshEntity(entity);
                    }
                }
            }
        }

        if (data.hasAbility("cook_bone_from_animals") && world.getRandom().nextFloat() < BONE_FROM_ANIMAL_CHANCE) {
            ItemEntity boneEntity = new ItemEntity(world, mob.getX(), mob.getY(), mob.getZ(), new ItemStack(Items.BONE));
            world.addFreshEntity(boneEntity);
        }

        return XP_PER_ANIMAL_KILL;
    }
}