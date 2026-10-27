package org.sgx.classes.item;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.Arz_classes;

import java.util.ArrayList;
import java.util.List;

/**
 * Книги классов и кольца опыта в слотах Trinkets при смерти. Предметы из слотов Trinkets могли
 * пропадать (моды-«трупы» их не видят), поэтому прямо перед смертью переносим их так:
 * 1) в обычный инвентарь (оттуда их заберёт мод трупов либо они выпадут как обычный лут);
 * 2) если в инвентаре нет места — выбрасываем рядом с игроком.
 * Слот при этом очищается, предмет не дублируется и не теряется.
 */
public final class ClassBookDeathGuard {

    private ClassBookDeathGuard() {
    }

    public static void init() {
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof ServerPlayer player) {
                try {
                    moveTrinketsOut(player);
                } catch (Exception e) {
                    Arz_classes.LOGGER.error("Не удалось перенести тринкеты при смерти {}", player.getName().getString(), e);
                }
            }
            return true;
        });
    }

    private static boolean isProtected(ItemStack stack) {
        return stack.getItem() instanceof ClassBookItem
                || stack.is(ModItems.RING_IRON)
                || stack.is(ModItems.RING_GOLD)
                || stack.is(ModItems.RING_NETHERITE);
    }

    private static void moveTrinketsOut(ServerPlayer player) {
        List<TrinketSlotAccess> slots = new ArrayList<>();
        for (TrinketSlotAccess access : TrinketsApi.getAttachment(player).equipped(ClassBookDeathGuard::isProtected, false)) {
            slots.add(access);
        }
        for (TrinketSlotAccess access : slots) {
            ItemStack stack = access.get();
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack copy = stack.copy();
            access.set(ItemStack.EMPTY);
            if (!player.getInventory().add(copy)) {
                // add() мог положить часть стека; остаток (если есть) выбрасываем рядом
                if (!copy.isEmpty()) {
                    player.drop(copy, true);
                }
            }
        }
    }
}
