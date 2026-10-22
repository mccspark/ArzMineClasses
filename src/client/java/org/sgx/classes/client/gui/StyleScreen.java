package org.sgx.classes.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Окно выбора оформления. Слева список тем, справа живой предпросмотр: мини-окно рисуется в выбранной (ещё не
 * применённой) теме. «Применить» сохраняет тему для всех окон мода, «Отмена»/Esc — закрывает без изменений.
 */
public final class StyleScreen extends Screen {

    private static final int MAX_W = 330;
    private static final int MAX_H = 200;
    private static final int PAD = 12;
    private static final int LIST_W = 130;
    private static final int LIST_TOP = 34;
    private static final int BUTTON_H = 18;
    private static final int BUTTON_BOTTOM_MARGIN = 8;
    private static final int SWATCH = 12;

    private final Screen parent;
    private BookStyle.Theme selected = BookStyle.theme();

    private int left, top, panelW, panelH;
    private int listX, listY, rowH;
    private int prevX, prevY, prevW, prevH;
    private StyledButton applyButton;

    public StyleScreen(Screen parent) {
        super(Component.translatable("screen.arz_classes.ui_style.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        panelW = Math.min(MAX_W, this.width - 8);
        panelH = Math.min(MAX_H, this.height - 8);
        left = (this.width - panelW) / 2;
        top = (this.height - panelH) / 2;

        int count = BookStyle.Theme.values().length;
        int buttonsY = top + panelH - BUTTON_BOTTOM_MARGIN - BUTTON_H;
        listX = left + PAD;
        listY = top + LIST_TOP;
        rowH = Math.max(16, Math.min(24, (buttonsY - 8 - listY) / count));

        prevX = listX + LIST_W + 10;
        prevY = listY;
        prevW = left + panelW - PAD - prevX;
        prevH = rowH * count - 2;

        int bw = 100;
        applyButton = new StyledButton(left + panelW / 2 - bw - 4, buttonsY, bw, BUTTON_H,
                Component.translatable("screen.arz_classes.ui_style.apply"), this::apply);
        applyButton.active = selected != BookStyle.theme();
        addRenderableWidget(applyButton);
        addRenderableWidget(new StyledButton(left + panelW / 2 + 4, buttonsY, bw, BUTTON_H,
                Component.translatable("screen.arz_classes.ui_style.cancel"), this::onClose));
    }

    private void apply() {
        BookStyle.set(selected);
        this.onClose(); // родительский экран при возврате пересоздаёт виджеты и подхватывает новую тему
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() == 0) {
            BookStyle.Theme[] all = BookStyle.Theme.values();
            for (int i = 0; i < all.length; i++) {
                int y = listY + i * rowH;
                if (event.x() >= listX && event.x() < listX + LIST_W && event.y() >= y && event.y() < y + rowH - 2) {
                    selected = all[i];
                    applyButton.active = selected != BookStyle.theme();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        BookStyle.panel(graphics, left, top, panelW, panelH);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        BookStyle.Theme current = BookStyle.theme();
        graphics.centeredText(this.font, this.title, left + panelW / 2, top + 11, current.title());
        BookStyle.divider(graphics, left + panelW / 2, top + 26, panelW / 2 - 24);

        BookStyle.Theme[] all = BookStyle.Theme.values();
        for (int i = 0; i < all.length; i++) {
            BookStyle.Theme t = all[i];
            int y = listY + i * rowH;
            int h = rowH - 2;
            boolean hovered = mouseX >= listX && mouseX < listX + LIST_W && mouseY >= y && mouseY < y + h;
            BookStyle.row(graphics, listX, y, LIST_W, h, t == selected, hovered);

            int sx = listX + 6;
            int sy = y + (h - SWATCH) / 2;
            if (t == BookStyle.Theme.CLASSIC) {
                graphics.fill(sx, sy, sx + SWATCH, sy + SWATCH, 0xFF1F1F24);
                graphics.outline(sx, sy, SWATCH, SWATCH, 0xFF4A4A52);
            } else {
                graphics.fill(sx, sy, sx + SWATCH, sy + SWATCH, t.leather);
                graphics.fill(sx + 2, sy + 2, sx + SWATCH - 2, sy + SWATCH - 2, t.fill | 0xFF000000);
                graphics.outline(sx, sy, SWATCH, SWATCH, t.gold);
            }

            Component name = Component.translatable("screen.arz_classes.ui_style.name." + t.id());
            int color = t == selected ? current.title() : current.body();
            graphics.text(this.font, name, sx + SWATCH + 6, y + (h - 8) / 2 + 1, color);
            if (t == current) {
                // отметка «сейчас применено»
                graphics.text(this.font, "*", listX + LIST_W - 10, y + (h - 8) / 2 + 1, current.dim());
            }
        }

        drawPreview(graphics, prevX, prevY, prevW, prevH, selected);
    }

    /** Мини-окно в теме t: заголовок, текст, слоты, полоска, две кнопки (обычная и под курсором). */
    private void drawPreview(GuiGraphicsExtractor g, int x, int y, int w, int h, BookStyle.Theme t) {
        BookStyle.panel(g, x, y, w, h, t);
        int cx = x + w / 2;

        g.centeredText(this.font, Component.translatable("screen.arz_classes.ui_style.preview_title"),
                cx, y + 10, t.title());
        BookStyle.divider(g, cx, y + 22, Math.max(8, w / 2 - 14), t);

        int textY = y + 28;
        var lines = this.font.split(Component.translatable("screen.arz_classes.ui_style.preview_text"), w - 24);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            g.centeredText(this.font, lines.get(i), cx, textY, t.body());
            textY += 9;
        }

        int slotY = y + 48;
        int slotX = cx - 27;
        ItemStack[] stacks = {new ItemStack(Items.WHEAT), new ItemStack(Items.CARROT), new ItemStack(Items.POTATO)};
        for (int i = 0; i < stacks.length; i++) {
            BookStyle.slot(g, slotX + i * 18, slotY, t);
            g.item(stacks[i], slotX + i * 18 + 1, slotY + 1);
        }

        BookStyle.bar(g, x + 12, slotY + 18 + 8, w - 24, 8, 0.45f, 0xFF55AA55, t);

        int btnY = y + h - 8 - 16;
        int btnW = (w - 12 * 2 - 6) / 2;
        BookStyle.button(g, x + 12, btnY, btnW, 16, false, true,
                Component.translatable("screen.arz_classes.select"), t);
        BookStyle.button(g, x + 12 + btnW + 6, btnY, btnW, 16, true, true,
                Component.translatable("screen.arz_classes.back"), t);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
