package org.sgx.classes.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.block.RarityUpgradeTableBlockEntity;
import org.sgx.classes.block.RarityUpgradeTableMenu;
import org.sgx.classes.profession.RarityUpgradeHandler;

public final class RarityUpgradeNetworking {

    private RarityUpgradeNetworking() {
    }

    public static void registerCommon() {
        PayloadTypeRegistry.serverboundPlay().register(StartRarityUpgradePayload.TYPE, StartRarityUpgradePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenRarityUpgradeWheelPayload.TYPE, OpenRarityUpgradeWheelPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(StartRarityUpgradePayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    if (!(player.containerMenu instanceof RarityUpgradeTableMenu menu)) {
                        return;
                    }
                    Container container = menu.container();
                    ItemStack itemStack = container.getItem(RarityUpgradeTableBlockEntity.SLOT_ITEM);
                    ItemStack shardStack = container.getItem(RarityUpgradeTableBlockEntity.SLOT_SHARD);

                    RarityUpgradeHandler.Result check = RarityUpgradeHandler.check(itemStack, shardStack);
                    if (!check.canAttempt()) {
                        return;
                    }

                    shardStack.shrink(1);
                    boolean success = RarityUpgradeHandler.attempt(itemStack, check.target(), check.chance());

                    if (success) {
                        org.sgx.classes.item.ClassBookHooks.onRarityUpgraded(player, check.target());
                    }

                    container.setItem(RarityUpgradeTableBlockEntity.SLOT_ITEM, ItemStack.EMPTY);
                    container.setItem(RarityUpgradeTableBlockEntity.SLOT_OUTPUT, itemStack);
                    container.setChanged();

                    player.closeContainer();
                    ServerPlayNetworking.send(player, new OpenRarityUpgradeWheelPayload(
                            (float) check.chance(), success, check.target().ordinal()));
                }));
    }
}