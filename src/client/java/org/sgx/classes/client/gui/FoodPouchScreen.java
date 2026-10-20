package org.sgx.classes.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.item.FoodPouchMenu;

public final class FoodPouchScreen extends AbstractContainerScreen<FoodPouchMenu> {

    private static final Identifier TEXTURE = Arz_classes.id("textures/gui/container/food_pouch.png");

    public FoodPouchScreen(FoodPouchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 159);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blit(TEXTURE, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight,
                0.0f, 1.0f, 0.0f, 1.0f);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        this.titleLabelY = 18;
    }
}
