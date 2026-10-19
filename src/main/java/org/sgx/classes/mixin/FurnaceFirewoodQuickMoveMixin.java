package org.sgx.classes.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.FirewoodSlot;
import org.sgx.classes.item.ModItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Шифт-клик: firewood из инвентаря -> слот firewood, и обратно из слота -> в инвентарь. */
@Mixin(AbstractFurnaceMenu.class)
public abstract class FurnaceFirewoodQuickMoveMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true)
    private void arzClasses$quickMoveFirewood(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        if (!((Object) this instanceof FurnaceMenu)) {
            return;
        }
        AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
        int firewoodIndex = menu.slots.size() - 1;
        if (firewoodIndex < 3 || index < 0 || index > firewoodIndex
                || !(menu.slots.get(firewoodIndex) instanceof FirewoodSlot)) {
            return;
        }
        Slot slot = menu.slots.get(index);
        if (!slot.hasItem()) {
            return;
        }
        ItemStack stack = slot.getItem();
        boolean fromFirewoodSlot = index == firewoodIndex;
        boolean toFirewoodSlot = !fromFirewoodSlot && index >= 3 && stack.is(ModItems.FIREWOOD);
        if (!fromFirewoodSlot && !toFirewoodSlot) {
            return;
        }

        ItemStack original = stack.copy();
        AbstractContainerMenuInvoker invoker = (AbstractContainerMenuInvoker) (Object) menu;
        boolean moved = fromFirewoodSlot
                ? invoker.arzClasses$moveItemStackTo(stack, 3, firewoodIndex, true)
                : invoker.arzClasses$moveItemStackTo(stack, firewoodIndex, firewoodIndex + 1, false);
        if (!moved) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount()) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        slot.onTake(player, stack);
        cir.setReturnValue(original);
    }
}