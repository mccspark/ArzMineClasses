package org.sgx.classes.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import org.sgx.classes.client.gui.ClassesScreen;
import org.sgx.classes.client.gui.ClassesTabButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * FIX: leftPos/topPos/imageWidth объявлены на AbstractContainerScreen, а не на самом
 * InventoryScreen — при фиктивном "extends Screen" компилятор их не видит вообще,
 * поэтому @Shadow падал с "Unresolved field ... in target class". Теперь миксин
 * "притворяется" наследником AbstractContainerScreen<InventoryMenu> (реальный родитель
 * InventoryScreen) — поля становятся обычными унаследованными, @Shadow не нужен.
 * Заодно this уже сам по себе Screen — явный каст (Screen)(Object)this больше не нужен.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends AbstractContainerScreen<InventoryMenu> {

    protected InventoryScreenMixin(InventoryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void arzClasses$addClassesTab(CallbackInfo ci) {
        Screen self = this;

        int buttonSize = 22;
        int tabX = this.leftPos + this.imageWidth - buttonSize;
        int tabY = this.topPos - buttonSize - 2; // над панелью, а не сбоку

        this.addRenderableWidget(new ClassesTabButton(tabX, tabY,
                () -> Minecraft.getInstance().gui.setScreen(new ClassesScreen(self))));
    }
}