package org.sgx.classes.event;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.sgx.classes.item.ModItems;

/**
 * Любой игрок (без привязки к классу) может расколоть алмаз молотом: алмаз в оффхенде,
 * молот (обычный или медный) в главной руке — ПКМ в воздух. Молот теряет 1 прочности
 * через штатный hurtAndBreak (а не ручное manipulation setDamageValue, как было раньше
 * в SharpeningTrigger/DiamondShardCrafting v1 — эта версия чище).
 */
public final class DiamondShardCrafting {

    private static final int SHARDS_PER_DIAMOND = 9;
    private static final int HAMMER_DAMAGE_PER_USE = 1;

    private DiamondShardCrafting() {
    }

    public static void init() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide() || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            boolean mainIsHammer = mainHand.is(ModItems.BLACKSMITH_HAMMER) || mainHand.is(ModItems.COPPER_HAMMER);
            if (!mainIsHammer || !offHand.is(Items.DIAMOND)) {
                return InteractionResult.PASS;
            }

            offHand.shrink(1);

            ItemStack shards = new ItemStack(ModItems.DIAMOND_SHARD, SHARDS_PER_DIAMOND);
            if (!player.getInventory().add(shards)) {
                player.drop(shards, false);
            }

            mainHand.hurtAndBreak(HAMMER_DAMAGE_PER_USE, (ServerLevel) player.level(), serverPlayer, item ->
                    serverPlayer.sendSystemMessage(Component.translatable("message.arz_classes.hammer_broke")));

            return InteractionResult.SUCCESS;
        });
    }
}