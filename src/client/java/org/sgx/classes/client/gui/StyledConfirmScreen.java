package org.sgx.classes.client.gui;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/** Окно подтверждения в стиле мода. Контракт как у ванильного ConfirmScreen:
 * callback получает true/false и сам решает, какой экран открыть дальше. ESC = «Нет». */
public class StyledConfirmScreen extends Screen {

    private static final int BUTTON_W = 150;
    private static final int BUTTON_H = 20;
    private static final int GAP = 8;
    private static final int MAX_TEXT_WIDTH = 320;

    private final BooleanConsumer callback;
    private final Component message;

    private int textWidth;
    private int titleY;
    private int messageY;
    private int boxX, boxY, boxW, boxH;

    public StyledConfirmScreen(BooleanConsumer callback, Component title, Component message) {
        super(title);
        this.callback = callback;
        this.message = message;
    }

    @Override
    protected void init() {
        this.textWidth = Math.min(MAX_TEXT_WIDTH, this.width - 40);
        int lines = Math.max(1, this.font.split(message, textWidth).size());

        this.titleY = this.height / 2 - 40;
        this.messageY = titleY + 16;
        int buttonsY = messageY + lines * 10 + 20;

        // рамка под текст и кнопки (рисуется только в книжных темах)
        this.boxW = Math.min(this.width - 8, Math.max(textWidth + 40, 2 * BUTTON_W + GAP + 32));
        this.boxX = (this.width - boxW) / 2;
        this.boxY = titleY - 18;
        this.boxH = buttonsY + BUTTON_H + 16 - boxY;

        int centerX = this.width / 2;
        addRenderableWidget(new StyledButton(centerX - BUTTON_W - GAP / 2, buttonsY, BUTTON_W, BUTTON_H,
                CommonComponents.GUI_YES, () -> callback.accept(true)));
        addRenderableWidget(new StyledButton(centerX + GAP / 2, buttonsY, BUTTON_W, BUTTON_H,
                CommonComponents.GUI_NO, () -> callback.accept(false)));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        if (BookStyle.enabled()) {
            BookStyle.panel(graphics, boxX, boxY, boxW, boxH);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);
        graphics.centeredText(this.font, this.title, this.width / 2, titleY, BookStyle.title(0xFFFFD46B));
        graphics.textWithWordWrap(this.font, message, (this.width - textWidth) / 2, messageY, textWidth,
                BookStyle.body(0xFFE6E6E6));
    }

    @Override
    public void onClose() {
        callback.accept(false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}