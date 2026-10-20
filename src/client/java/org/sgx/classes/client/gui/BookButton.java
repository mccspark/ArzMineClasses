package org.sgx.classes.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Кнопка в стиле книги урожая: кожаная, с золотой окантовкой и «стежками», текст цвета пергамента. */
public class BookButton extends AbstractWidget {

    private static final int LEATHER = 0xFF4A2C17;
    private static final int LEATHER_HOVER = 0xFF63391C;
    private static final int LEATHER_PRESSED = 0xFF3A2210;
    private static final int OUTLINE = 0xFF2A180C;
    private static final int HIGHLIGHT = 0xFF6B4426;
    private static final int GOLD = 0xFFA77B2E;
    private static final int GOLD_HOVER = 0xFFE0B04A;
    private static final int STITCH = 0xFFC79A4A;
    private static final int TEXT = 0xFFE8D5A8;
    private static final int TEXT_HOVER = 0xFFFFF0C2;

    private final Runnable onClickAction;

    public BookButton(int x, int y, int width, int height, Component message, Runnable onClick) {
        super(x, y, width, height, message);
        this.onClickAction = onClick;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onClickAction.run();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered() && this.active;
        int x = getX();
        int y = getY();

        graphics.fill(x, y, x + width, y + height, !this.active ? LEATHER_PRESSED : (hovered ? LEATHER_HOVER : LEATHER));
        // фаска: светлый верх, тёмный низ
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, HIGHLIGHT);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, LEATHER_PRESSED);
        graphics.outline(x, y, width, height, OUTLINE);
        // золотая рамка внутри
        graphics.outline(x + 2, y + 2, width - 4, height - 4, hovered ? GOLD_HOVER : GOLD);
        // стежки по верхней и нижней кромке
        for (int sx = x + 6; sx < x + width - 6; sx += 4) {
            graphics.fill(sx, y + 1, sx + 1, y + 2, STITCH);
            graphics.fill(sx, y + height - 2, sx + 1, y + height - 1, STITCH);
        }

        // текст без тени — как на страницах книги
        var font = Minecraft.getInstance().font;
        Component label = this.getMessage();
        graphics.text(font, label, x + (width - font.width(label)) / 2, y + (height - 8) / 2 + 1,
                !this.active ? 0xFF7A6A52 : (hovered ? TEXT_HOVER : TEXT), false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
