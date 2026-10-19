package org.sgx.classes.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.sgx.classes.profession.RarityRangedBonuses;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Скорость натяжения лука: бонус редкости ускоряет набор силы выстрела. */
@Mixin(BowItem.class)
public abstract class BowItemMixin {

    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void arzClasses$fasterDraw(ItemStack stack, Level level, LivingEntity entity, int remainingTime,
                                       CallbackInfoReturnable<Boolean> cir,
                                       @Local(argsOnly = true) LocalIntRef remaining) {
        double bonus = RarityRangedBonuses.bonusFor(stack);
        if (bonus <= 0) {
            return;
        }
        int total = 72000; // BowItem#getUseDuration
        int held = total - remainingTime;
        remaining.set(total - (int) Math.round(held * (1.0 + bonus)));
    }
}