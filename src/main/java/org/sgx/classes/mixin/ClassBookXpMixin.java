package org.sgx.classes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.sgx.classes.item.ClassBookHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Книга Мистика: весь полученный опыт и максимальный уровень. require = 0 — не ломаем запуск при смене сигнатур. */
@Mixin(Player.class)
public abstract class ClassBookXpMixin {

    @Inject(method = "giveExperiencePoints", at = @At("TAIL"), require = 0)
    private void arzClasses$countXp(int points, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            ClassBookHooks.onExperience(player, points);
            ClassBookHooks.onLevel(player);
        }
    }

    @Inject(method = "giveExperienceLevels", at = @At("TAIL"), require = 0)
    private void arzClasses$countLevels(int levels, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) {
            ClassBookHooks.onLevel(player);
        }
    }
}
