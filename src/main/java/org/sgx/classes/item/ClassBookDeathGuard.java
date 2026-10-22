package org.sgx.classes.item;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Книги в слоте Trinkets при смерти. Моды-«трупы» (Corpse и подобные) забирают вещи из инвентаря и
 * ванильных дропов, а предметы из слотов Trinkets они могут не увидеть — книга тогда пропадает.
 * Поэтому прямо перед смертью переносим книги из слота в обычный инвентарь: оттуда их заберёт
 * любой мод трупов (а без такого мода они выпадут как обычный лут, как и раньше).
 * Если в инвентаре нет места — книга остаётся в слоте (поведение Trinkets по умолчанию).
 */
public final class ClassBookDeathGuard {

    private ClassBookDeathGuard() {
    }

    public static void init() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) {
                moveBooksToInventory(player);
            }
            return true;
        });
    }

    private static void moveBooksToInventory(ServerPlayer player) {
        List<TrinketSlotAccess> slots = new ArrayList<>();
        for (TrinketSlotAccess access : TrinketsApi.getAttachment(player)
                .equipped(s -> s.getItem() instanceof ClassBookItem, false)) {
            slots.add(access);
        }
        for (TrinketSlotAccess access : slots) {
            ItemStack book = access.get();
            if (book.isEmpty()) {
                continue;
            }
            ItemStack copy = book.copy();
            if (player.getInventory().add(copy)) {
                access.set(ItemStack.EMPTY);
            }
        }
    }
}
