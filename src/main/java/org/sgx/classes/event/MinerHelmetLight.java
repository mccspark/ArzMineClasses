package org.sgx.classes.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.item.ModItems;
import org.sgx.classes.profession.ProfessionType;

/**
 * Каска шахтёра: носить может только Шахтёр — у остальных каска снимается с головы в инвентарь.
 * Сам свет каски даёт мод динамического освещения через ресурс
 * assets/arz_classes/dynamiclights/item/miner_helmet.json (в коде от мода зависимости нет, блоки света не ставятся).
 */
public final class MinerHelmetLight {

    private MinerHelmetLight() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(MinerHelmetLight::tick);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            boolean wearing = player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MINER_HELMET);
            if (wearing && ModAttachments.get(player).currentProfession() != ProfessionType.MINER) {
                takeOff(player);
            }
        }
    }

    private static void takeOff(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD).copy();
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        if (!player.getInventory().add(helmet)) {
            player.drop(helmet, false);
        }
        player.sendSystemMessage(Component.translatable("message.arz_classes.miner_helmet_only_miner"), true);
    }
}