package org.sgx.classes.profession;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.ItemRarity;

/**
 * Бонус прочности от редкости — в ПРОЦЕНТАХ от максимальной (тот же % что и у статов: statBonusPercent()).
 * Пересчёт идёт отношением старого и нового множителя, поэтому при апгрейде редкости прочность
 * не накапливается дублями, а плавно переходит с Rare на Epic и т.д.
 */
public final class RarityDurability {

    private RarityDurability() {
    }

    private static double multiplier(ItemRarity rarity) {
        return 1.0 + (rarity == null ? 0.0 : Math.max(0.0, rarity.statBonusPercent()));
    }

    /** Меняет максимальную прочность при смене редкости from -> to. from == null — у предмета редкости ещё не было. */
    public static void change(ItemStack stack, ItemRarity from, ItemRarity to) {
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return;
        }
        double fromMul = multiplier(from);
        double toMul = multiplier(to);
        if (fromMul == toMul) {
            return;
        }
        int newMax = Math.max(1, (int) Math.round(max * toMul / fromMul));
        stack.set(DataComponents.MAX_DAMAGE, newMax);
    }
}