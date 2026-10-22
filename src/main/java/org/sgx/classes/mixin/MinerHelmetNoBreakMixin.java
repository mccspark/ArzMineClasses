package org.sgx.classes.mixin;

import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Каска шахтёра не ломается от урона: износ останавливается на 1 прочности (это и есть «разряд»).
 * Подменяем величину урона у всех вариантов hurtAndBreak: больше, чем осталось до 1 прочности, не списывается;
 * нулевой урон ваниль игнорирует, поэтому предмет не ломается.
 */
@Mixin(ItemStack.class)
public abstract class MinerHelmetNoBreakMixin {

    @ModifyVariable(method = "hurtAndBreak", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private int arzClasses$keepHelmetAlive(int amount) {
        ItemStack self = (ItemStack) (Object) this;
        if (self.is(ModItems.MINER_HELMET) || self.is(ModItems.MINER_HELMET_OFF)) {
            int usable = self.getMaxDamage() - self.getDamageValue() - 1;
            return Math.min(amount, Math.max(0, usable));
        }
        return amount;
    }
}
