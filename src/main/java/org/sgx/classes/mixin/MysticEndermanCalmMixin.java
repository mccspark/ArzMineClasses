package org.sgx.classes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.EnderMan;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MysticEndermanCalmMixin {

    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void arzClasses$calmForMystic(LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (!(self instanceof EnderMan) || !(target instanceof ServerPlayer player)) {
            return;
        }
        ProfessionData data = ModAttachments.get(player);
        if (data.currentProfession() != ProfessionType.MYSTIC || !data.hasAbility("mystic_enderman_calm")) {
            return;
        }
        if (self.getTarget() == player || self.getLastHurtByMob() == player) {
            return;
        }
        ci.cancel();
    }
}