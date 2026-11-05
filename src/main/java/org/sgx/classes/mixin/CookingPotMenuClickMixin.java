package org.sgx.classes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.event.CookingPotXp;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Считает, сколько предметов реально ушло из слота результата котелка Farmer's Delight за один клик
 * (обычный клик, шифт-клик, двойной клик, число, Q — всё одним способом: «было - стало»).
 * Цель — сам AbstractContainerMenu#clicked, а не класс FD, поэтому виртуальная диспетчеризация нам не мешает.
 * Тип клика (в новых версиях ClickType переименован) нам не нужен, поэтому он принимается как @Coerce Object —
 * так код не зависит от названия этого класса.
 */
@Mixin(AbstractContainerMenu.class)
public abstract class CookingPotMenuClickMixin {

    @Unique
    private Item arzClasses$potItem;
    @Unique
    private int arzClasses$potCountBefore;

    @Inject(method = "clicked", at = @At("HEAD"))
    private void arzClasses$potBefore(int slotId, int button, @Coerce Object clickType, Player player, CallbackInfo ci) {
        this.arzClasses$potItem = null;
        this.arzClasses$potCountBefore = 0;
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        if (menu.slots.size() <= CookingPotXp.OUTPUT_SLOT || !CookingPotXp.isPotMenu(menu)) {
            return;
        }
        ItemStack out = menu.getSlot(CookingPotXp.OUTPUT_SLOT).getItem();
        if (out.isEmpty()) {
            return;
        }
        this.arzClasses$potItem = out.getItem();
        this.arzClasses$potCountBefore = out.getCount();
    }

    @Inject(method = "clicked", at = @At("RETURN"))
    private void arzClasses$potAfter(int slotId, int button, @Coerce Object clickType, Player player, CallbackInfo ci) {
        Item item = this.arzClasses$potItem;
        int before = this.arzClasses$potCountBefore;
        this.arzClasses$potItem = null;
        this.arzClasses$potCountBefore = 0;
        if (item == null || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        ItemStack out = menu.getSlot(CookingPotXp.OUTPUT_SLOT).getItem();
        int now = (!out.isEmpty() && out.is(item)) ? out.getCount() : 0;
        int taken = before - now;
        if (taken > 0) {
            CookingPotXp.awardServings(serverPlayer, item, taken);
        }
    }
}