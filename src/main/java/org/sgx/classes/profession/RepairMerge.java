package org.sgx.classes.profession;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.item.ModDataComponents;

import java.util.ArrayList;
import java.util.List;

/**
 * Объединение двух одинаковых повреждённых предметов в сетке крафта (ванильный "ремонт крафтом").
 *
 * БАГ, который это чинит: ванильный рецепт собирает НОВЫЙ стек (без нашей редкости и бонусов заточки),
 * ставит ему MAX_DAMAGE = max из двух входов, а при взятии результата RarityCraftHandler бросал редкость
 * как для свежесозданной вещи — и снова накладывал бонус кузнеца и множитель редкости на уже
 * увеличенный максимум. С каждым объединением максимальная прочность росла.
 *
 * Теперь результат — копия «лучшего» входа (с бОльшим максимумом, при равенстве — с редкостью выше):
 * его редкость, бонусы атрибутов, имя заточки и максимум прочности переносятся как есть, а
 * оставшаяся прочность считается по ванильной формуле (остаток1 + остаток2 + 5%), но не выше максимума.
 * Зачарования остаются ванильными (их собрал сам рецепт).
 */
public final class RepairMerge {

    private RepairMerge() {
    }

    /**
     * Если текущий результат крафта — объединение двух одинаковых предметов, исправляет его
     * (один раз; повторный вызов на том же стеке безопасен) и возвращает true.
     * Иначе ничего не делает и возвращает false.
     */
    public static boolean tryMerge(Player player, ItemStack result) {
        if (result.isEmpty() || player.containerMenu == null) {
            return false;
        }
        List<ItemStack> inputs = new ArrayList<>(2);
        for (Slot slot : player.containerMenu.slots) {
            if (!(slot.container instanceof CraftingContainer)) {
                continue;
            }
            ItemStack in = slot.getItem();
            if (in.isEmpty()) {
                continue;
            }
            if (inputs.size() == 2) {
                return false; // больше двух предметов в сетке — это не объединение
            }
            inputs.add(in);
        }
        if (inputs.size() != 2) {
            return false;
        }
        ItemStack a = inputs.get(0);
        ItemStack b = inputs.get(1);
        if (!ItemStack.isSameItem(a, b) || !ItemStack.isSameItem(a, result)
                || a.getMaxDamage() <= 0 || b.getMaxDamage() <= 0) {
            return false;
        }

        ItemStack best = pickBest(a, b);
        int bestMax = best.getMaxDamage();
        int remainingA = a.getMaxDamage() - a.getDamageValue();
        int remainingB = b.getMaxDamage() - b.getDamageValue();

        ItemRarity rarity = best.get(ModDataComponents.ITEM_RARITY);
        if (rarity != null) {
            result.set(ModDataComponents.ITEM_RARITY, rarity);
        } else {
            result.remove(ModDataComponents.ITEM_RARITY);
        }
        if (best.has(DataComponents.ATTRIBUTE_MODIFIERS)) {
            result.set(DataComponents.ATTRIBUTE_MODIFIERS, best.get(DataComponents.ATTRIBUTE_MODIFIERS));
        }
        if (best.has(DataComponents.CUSTOM_NAME)) {
            result.set(DataComponents.CUSTOM_NAME, best.get(DataComponents.CUSTOM_NAME));
        }

        result.set(DataComponents.MAX_DAMAGE, bestMax);
        int restored = remainingA + remainingB + bestMax * 5 / 100;
        result.setDamageValue(Math.max(0, bestMax - Math.min(bestMax, restored)));
        return true;
    }

    private static ItemStack pickBest(ItemStack a, ItemStack b) {
        int maxA = a.getMaxDamage();
        int maxB = b.getMaxDamage();
        if (maxA != maxB) {
            return maxA > maxB ? a : b;
        }
        return rank(a) >= rank(b) ? a : b;
    }

    private static int rank(ItemStack stack) {
        ItemRarity rarity = stack.get(ModDataComponents.ITEM_RARITY);
        return rarity == null ? -1 : rarity.ordinal();
    }
}
