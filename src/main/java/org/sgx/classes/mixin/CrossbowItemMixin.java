package org.sgx.classes.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.profession.RarityRangedBonuses;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Скорость перезарядки арбалета: бонус редкости сокращает время зарядки. */
@Mixin(CrossbowItem.class)
public abstract class CrossbowItemMixin {

    @ModifyReturnValue(method = "getChargeDuration", at = @At("RETURN"))
    private static int arzClasses$fasterReload(int original, ItemStack crossbow, LivingEntity user) {
        double bonus = RarityRangedBonuses.bonusFor(crossbow);
        if (bonus <= 0) {
            return original;
        }
        return Math.max(1, (int) Math.round(original / (1.0 + bonus)));
    }
}