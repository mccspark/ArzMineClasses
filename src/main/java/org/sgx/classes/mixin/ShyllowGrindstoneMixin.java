package org.sgx.classes.mixin;

import net.minecraft.world.inventory.GrindstoneMenu;
import org.sgx.classes.profession.ShyllowEnchant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Точило сняло Shyllow — возвращаем предмету обычный вид (иначе он остался бы радужным без чара). */
@Mixin(GrindstoneMenu.class)
public abstract class ShyllowGrindstoneMixin {

    // require = 0: createResult у точила приватный и может отличаться между версиями; без него просто не чистим вид
    @Inject(method = "createResult", at = @At("TAIL"), require = 0)
    private void arzClasses$shyllowVisuals(CallbackInfo ci) {
        ShyllowEnchant.applyVisuals(((GrindstoneMenu) (Object) this).getSlot(2).getItem());
    }
}