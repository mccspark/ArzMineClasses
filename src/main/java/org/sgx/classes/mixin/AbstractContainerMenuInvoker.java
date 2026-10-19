package org.sgx.classes.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuInvoker {

    @Invoker("addSlot")
    Slot arzClasses$addSlot(Slot slot);

    @Invoker("addDataSlots")
    void arzClasses$addDataSlots(ContainerData data);

    @Invoker("moveItemStackTo")
    boolean arzClasses$moveItemStackTo(ItemStack stack, int startIndex, int endIndex, boolean reverse);
}