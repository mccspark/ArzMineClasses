package org.sgx.classes.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Маленькая шестерёнка (11×11) без фона; цвет берётся из текущей темы. Открывает окно выбора вида. */
public class GearButton extends AbstractWidget {

    public static final int SIZE = 11;

    /** Пиксельная шестерёнка 9×9: '#' — закрашенный пиксель. */
    private static final String[] GEAR = {
            "...###...",
            ".#.###.#.",
            "..#####..",
            "####.####",
            "###...###",
            "####.####",
            "..#####..",
            ".#.###.#.",
            "...###...",
    };

    private final Runnable onClickAction;
    private final Component tooltip;

    public GearButton(int x, int y, Component tooltip, Runnable onClick) {
        super(x, y, SIZE, SIZE, tooltip);
        this.onClickAction = onClick;
        this.tooltip = tooltip;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        onClickAction.run();
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        boolean hovered = this.isHovered();
        BookStyle.Theme t = BookStyle.theme();
        int color = t == BookStyle.Theme.CLASSIC
                ? (hovered ? 0xFFFFD46B : 0xFFAAAAAA)
                : (hovered ? t.goldBright : t.gold);
        int shadow = 0xA0000000;

        int ox = getX() + 1;
        int oy = getY() + 1;
        // тень на пиксель вправо-вниз, затем сама шестерёнка
        for (int pass = 0; pass < 2; pass++) {
            int dx = pass == 0 ? 1 : 0;
            int c = pass == 0 ? shadow : color;
            for (int row = 0; row < GEAR.length; row++) {
                String line = GEAR[row];
                for (int col = 0; col < line.length(); col++) {
                    if (line.charAt(col) == '#') {
                        graphics.fill(ox + col + dx, oy + row + dx, ox + col + dx + 1, oy + row + dx + 1, c);
                    }
                }
            }
        }

        if (hovered) {
            graphics.setTooltipForNextFrame(tooltip, mouseX, mouseY);
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
