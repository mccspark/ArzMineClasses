package org.sgx.classes.profession;

import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.ModDataComponents;

/**
 * Общие штрафы за неудачную заточку. Прочность считается от ТЕКУЩЕГО максимума
 * предмета (getMaxDamage), предмет никогда не опускается ниже 1 прочности и не исчезает.
 */
public final class SharpeningPenalty {

    public static final int MEDIUM_LOSS_PERCENT = 25;
    public static final int BAD_LOSS_PERCENT = 75;

    private SharpeningPenalty() {
    }

    /** Снимает percent% от максимальной прочности, но оставляет минимум 1. */
    public static void loseDurabilityPercent(ItemStack stack, int percent) {
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return; // предмет без прочности
        }
        int loss = Math.max(1, maxDamage * percent / 100);
        int newDamage = Math.min(maxDamage - 1, stack.getDamageValue() + loss);
        stack.setDamageValue(newDamage);
    }

    /** Промах: прочность падает ровно до 1. */
    public static void dropToOneDurability(ItemStack stack) {
        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return;
        }
        stack.setDamageValue(maxDamage - 1);
    }

    /**
     * Старые предметы с флагом PERMANENTLY_BROKEN (система "навсегда сломан" удалена):
     * снимаем флаг и оставляем 1 прочности, чтобы их можно было чинить и затачивать.
     */
    public static void clearLegacyBroken(ItemStack stack) {
        if (Boolean.TRUE.equals(stack.get(ModDataComponents.PERMANENTLY_BROKEN))) {
            stack.remove(ModDataComponents.PERMANENTLY_BROKEN);
            dropToOneDurability(stack);
        }
    }
}