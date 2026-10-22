package org.sgx.classes.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Кнопка в общем стиле мода: плоская, тёмная, с золотой рамкой при наведении. */
public class StyledButton extends AbstractWidget {

    private static final int BG = 0xFF1F1F24;
    private static final int BG_HOVER = 0xFF2E2E36;
    private static final int BG_DISABLED = 0xFF17171A;
    private static final int BORDER = 0xFF4A4A52;
    private static final int BORDER_HOVER = 0xFFFFD46B;
    private static final int BORDER_DISABLED = 0xFF2A2A2E;
    private static final int TEXT = 0xFFE6E6E6;
    private static final int TEXT_HOVER = 0xFFFFD46B;
    private static final int TEXT_DISABLED = 0xFF6A6A70;

    private final Runnable onClickAction;
    private Component tooltip;

    public StyledButton(int x, int y, int width, int height, Component message, Runnable onClick) {
        super(x, y, width, height, message);
        this.onClickAction = onClick;
    }

    /** Подсказка при наведении (показывается и на неактивной кнопке). */
    public StyledButton withTooltip(Component tooltip) {
        this.tooltip = tooltip;
        return this;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onClickAction.run();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered() && this.active;
        int bg = !this.active ? BG_DISABLED : (hovered ? BG_HOVER : BG);
        int border = !this.active ? BORDER_DISABLED : (hovered ? BORDER_HOVER : BORDER);
        int text = !this.active ? TEXT_DISABLED : (hovered ? TEXT_HOVER : TEXT);

        int x = getX();
        int y = getY();
        if (BookStyle.enabled()) {
            BookStyle.button(graphics, x, y, width, height, this.isHovered(), this.active, this.getMessage());
            if (this.isHovered() && tooltip != null) {
                graphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
            }
            return;
        }
        graphics.fill(x, y, x + width, y + height, bg);
        graphics.outline(x, y, width, height, border);
        if (this.active) {
            // тонкий блик по верхней кромке внутри рамки
            graphics.fill(x + 1, y + 1, x + width - 1, y + 2, hovered ? 0x30FFD46B : 0x14FFFFFF);
        }

        graphics.centeredText(Minecraft.getInstance().font, this.getMessage(),
                x + width / 2, y + (height - 8) / 2, text);

        if (this.isHovered() && tooltip != null) {
            graphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}