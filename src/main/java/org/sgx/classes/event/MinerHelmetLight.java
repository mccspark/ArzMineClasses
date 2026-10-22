package org.sgx.classes.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.item.ModItems;
import org.sgx.classes.profession.ProfessionType;

/**
 * Каска шахтёра: носить может только Шахтёр — у остальных каска снимается с головы в инвентарь.
 * Сам свет каски даёт мод динамического освещения через ресурс
 * assets/arz_classes/dynamiclights/item/miner_helmet.json (в коде от мода зависимости нет, блоки света не ставятся).
 * <p>
 * Разрядка: когда прочность каски падает до 1 (она при этом не ломается — см. MinerHelmetNoBreakMixin),
 * предмет превращается в «разряженную» версию miner_helmet_off, для которой файла света нет, и каска перестаёт светить.
 * Лампочка (HelmetBulbItem) или любой ремонт возвращает заряженную версию.
 */
public final class MinerHelmetLight {

    private MinerHelmetLight() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(MinerHelmetLight::tick);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
            boolean wearing = head.is(ModItems.MINER_HELMET) || head.is(ModItems.MINER_HELMET_OFF);
            if (wearing && ModAttachments.get(player).currentProfession() != ProfessionType.MINER) {
                takeOff(player);
                continue;
            }
            if (player.tickCount % 10 == 0) {
                syncCharge(player);
            }
        }
    }

    private static boolean isEmpty(ItemStack stack) {
        int max = stack.getMaxDamage();
        return max > 0 && stack.getDamageValue() >= max - 1;
    }

    /** Заряженная с прочностью 1 → разряженная; разряженная с прочностью выше 1 → снова заряженная. */
    private static ItemStack synced(ItemStack stack) {
        if (stack.is(ModItems.MINER_HELMET) && isEmpty(stack)) {
            return stack.transmuteCopy(ModItems.MINER_HELMET_OFF);
        }
        if (stack.is(ModItems.MINER_HELMET_OFF) && !isEmpty(stack)) {
            return stack.transmuteCopy(ModItems.MINER_HELMET);
        }
        return null;
    }

    private static void syncCharge(ServerPlayer player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack swapped = synced(head);
        if (swapped != null) {
            player.setItemSlot(EquipmentSlot.HEAD, swapped);
            if (swapped.is(ModItems.MINER_HELMET_OFF)) {
                player.sendSystemMessage(Component.translatable("message.arz_classes.miner_helmet_discharged"), true);
            }
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = synced(inv.getItem(i));
            if (s != null) {
                inv.setItem(i, s);
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
