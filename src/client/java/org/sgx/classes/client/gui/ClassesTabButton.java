package org.sgx.classes.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Вкладка в инвентаре: тот же стиль, что и у StyledButton (тёмная, золотая рамка при наведении). */
public class ClassesTabButton extends AbstractWidget {

    private static final int BG = 0xFF1F1F24;
    private static final int BG_HOVER = 0xFF2E2E36;
    private static final int BORDER = 0xFF4A4A52;
    private static final int BORDER_HOVER = 0xFFFFD46B;

    private final Runnable onClickAction;
    private final ItemStack icon = new ItemStack(Items.ENCHANTED_BOOK);

    public ClassesTabButton(int x, int y, Runnable onClick) {
        super(x, y, 22, 22, Component.translatable("screen.arz_classes.classes"));
        this.onClickAction = onClick;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onClickAction.run();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered();
        int x = getX();
        int y = getY();

        if (BookStyle.enabled()) {
            BookStyle.button(graphics, x, y, width, height, hovered, true, null);
            graphics.item(icon, x + 3, y + 3);
            if (hovered) {
                graphics.setTooltipForNextFrame(this.getMessage(), mouseX, mouseY);
            }
            return;
        }
        graphics.fill(x, y, x + width, y + height, hovered ? BG_HOVER : BG);
        graphics.outline(x, y, width, height, hovered ? BORDER_HOVER : BORDER);
        // тонкий блик по верхней кромке внутри рамки
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, hovered ? 0x30FFD46B : 0x14FFFFFF);

        graphics.item(icon, x + 3, y + 3);

        if (hovered) {
            graphics.setTooltipForNextFrame(this.getMessage(), mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}