package org.sgx.classes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.ClassBookHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Книга Мистика: чары, наложенные через наковальню (учитывается в момент, когда игрок забирает результат). */
@Mixin(AnvilMenu.class)
public abstract class ClassBookAnvilMixin {

    // require = 0: сигнатура onTake может отличаться между версиями; без неё просто не считаем наковальню
    @Inject(method = "onTake", at = @At("HEAD"), require = 0)
    private void arzClasses$countAnvilEnchants(Player player, ItemStack result, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            AnvilMenu self = (AnvilMenu) (Object) this;
            ClassBookHooks.onAnvilTaken(serverPlayer, self.getSlot(0).getItem().copy(), result);
        }
    }
}
