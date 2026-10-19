package org.sgx.classes.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.sgx.classes.profession.impl.MysticXpBonus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class MysticMobXpMixin {

    @Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
    private void arzClasses$mysticXpBonus(ServerLevel level, Entity killer, CallbackInfoReturnable<Integer> cir) {
        int base = cir.getReturnValue();
        if (base <= 0 || !(killer instanceof ServerPlayer player)) {
            return;
        }
        cir.setReturnValue(MysticXpBonus.apply(player, base, ((LivingEntity) (Object) this).getRandom()));
    }
}