package org.sgx.classes.item;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionData;

public final class BonePouchItem extends Item {

    public static final int SLOT_COUNT = 9;
    public static final int SLOT_CAPACITY = 64;
    public static final int MAX_CONTENT = SLOT_COUNT * SLOT_CAPACITY;

    public BonePouchItem(Properties properties) {
        super(properties);
    }

    private static NonNullList<ItemStack> readContents(ItemStack pouch) {
        ItemContainerContents contents = pouch.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        contents.copyInto(items);
        return items;
    }

    private static void writeContents(ItemStack pouch, NonNullList<ItemStack> items) {
        pouch.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
    }

    public static int storedAmount(ItemStack stack) {
        int total = 0;
        for (ItemStack s : readContents(stack)) {
            total += s.getCount();
        }
        return total;
    }

    private static boolean consume(ItemStack pouch, int amount) {
        NonNullList<ItemStack> items = readContents(pouch);
        int remaining = amount;
        for (int i = 0; i < items.size() && remaining > 0; i++) {
            ItemStack s = items.get(i);
            if (s.isEmpty()) {
                continue;
            }
            int take = Math.min(remaining, s.getCount());
            s.shrink(take);
            remaining -= take;
        }
        if (remaining > 0) {
            return false;
        }
        writeContents(pouch, items);
        return true;
    }

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof BonePouchItem)) {
                return InteractionResult.PASS;
            }
            if (!player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            openGui(serverPlayer, hand);
            return InteractionResult.SUCCESS;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (hand != InteractionHand.MAIN_HAND || player.isShiftKeyDown()) {
                return InteractionResult.PASS;
            }
            ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof BonePouchItem)) {
                return InteractionResult.PASS;
            }
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS;
            }
            useOnArea(serverPlayer, (ServerLevel) world, stack, hitResult.getBlockPos());
            return InteractionResult.SUCCESS;
        });
    }

    public static void useOnArea(ServerPlayer player, ServerLevel world, ItemStack pouch, BlockPos clickedPos) {
        ProfessionData data = ModAttachments.get(player);
        int length = (int) data.customScalarTotal("farmer.bone_pouch_range"); // 1 / 3 / 5 по уровню мешочка
        if (length <= 0) {
            return;
        }
        boolean instantGrowth = data.hasAbility("farmer_bonemeal_instant");

        // Ряд длиной length вперёд от кликнутого блока, в сторону взгляда игрока
        Direction forward = player.getDirection();
        int grown = 0;

        for (int f = 0; f < length; f++) {
            BlockPos pos = clickedPos.relative(forward, f);
            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof BonemealableBlock target)
                    || !target.isValidBonemealTarget(world, pos, state)) {
                continue;
            }
            if (!consume(pouch, 1)) {
                break;
            }

            if (instantGrowth && state.getBlock() instanceof CropBlock crop) {
                world.setBlockAndUpdate(pos, crop.getStateForAge(crop.getMaxAge()));
            } else {
                target.performBonemeal(world, world.getRandom(), pos, state);
            }

            world.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.2, 0.2, 0.2, 0.0);
            world.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);
            grown++;
        }

        if (grown > 0) {
            world.playSound(null, player.blockPosition(), SoundEvents.CROP_PLANTED,
                    SoundSource.BLOCKS, 1.0f, 1.0f);
        }
    }
    private static void openGui(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        NonNullList<ItemStack> items = readContents(stack);
        SimpleContainer container = new SimpleContainer(SLOT_COUNT);
        for (int i = 0; i < SLOT_COUNT; i++) {
            container.setItem(i, items.get(i));
        }

        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.translatable("item.arz_classes.bone_pouch");
            }

            @Nullable
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player p) {
                return new org.sgx.classes.item.BonePouchMenu(containerId, inventory, container, hand);
            }
        });
    }
}