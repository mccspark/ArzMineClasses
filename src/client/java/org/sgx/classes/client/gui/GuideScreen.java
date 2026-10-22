package org.sgx.classes.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.profession.ProfessionColors;
import org.sgx.classes.profession.ProfessionType;

import java.util.ArrayList;
import java.util.List;

/**
 * Руководство по классу с вкладками.
 *
 * Картинки: абзац lang-файла вида  "[img:имя|ширина|высота]"  рисует картинку вместо текста.
 *   Файл: assets/arz_classes/textures/gui/sprites/guide/имя.png
 *   ширина/высота — размер на экране в пикселях GUI (необязательно, по умолчанию 192x108);
 *   картинка сама уменьшается под ширину окна и не выше MAX_IMG_ROWS строк.
 *   Подпись — следующий абзац обычным текстом (удобно начинать с §7, серый цвет).
 * Заголовок раздела: абзац, начинающийся с "§6", — под ним рисуется тонкая линия.
 *
 * mandatory = true (автооткрытие): выйти можно только после прокрутки до конца ВСЕХ вкладок.
 *
 * Как добавить вкладку:
 *   1) добавить в TABS строку  new Tab("id", false)
 *   2) в lang-файле: "guide.arz_classes.tab.id" — название вкладки,
 *      "guide.arz_classes.id.1", ".2", ".3" ... — абзацы по порядку.
 *   perClass = true — вкладка со своим текстом для каждого класса:
 *      "guide.arz_classes.id.<класс>.1..N" + общий блок "guide.arz_classes.id.common.1..N".
 */
public class GuideScreen extends Screen {

    public record Tab(String id, boolean perClass) {
    }

    public static final List<Tab> TABS = List.of(
            new Tab("class_info", true),
            new Tab("crafting", false)
            // сюда добавляются новые вкладки
    );

    private static final int MAX_W = 300;
    private static final int MAX_H = 232;
    private static final int MARGIN = 4;
    private static final int PAD = 12;
    private static final int TITLE_H = 30;
    private static final int TAB_ROW_H = 20;
    private static final int TAB_H = 16;
    private static final int FOOTER_H = 44;
    private static final int SCROLLBAR_W = 4;
    private static final int LINE_H = 12;
    private static final int MAX_IMG_ROWS = 9;
    private static final int DEFAULT_IMG_W = 192;
    private static final int DEFAULT_IMG_H = 108;
    private static final int WHEEL_LINES = 3;
    private static final int BUTTON_W = 90;
    private static final int BUTTON_H = 18;
    private static final int BUTTON_BOTTOM_MARGIN = 8;

    private final Screen parent;
    private final ProfessionType profession;
    private final boolean mandatory;

    private int selected = 0;
    private final int[] scrolls = new int[TABS.size()];
    private final boolean[] finished = new boolean[TABS.size()];

    private int left, top, panelW, panelH;
    private int viewX, viewY, viewW, viewH, visibleLines;
    private final List<Row> rows = new ArrayList<>();

    /** Одна строка прокрутки: текст, пустая строка, разделитель или первая строка картинки (span строк высотой). */
    private record Row(FormattedCharSequence text, boolean divider, Identifier[] images, int w, int h, int span) {
        static Row text(FormattedCharSequence t) {
            return new Row(t, false, null, 0, 0, 1);
        }

        static Row blank() {
            return new Row(FormattedCharSequence.EMPTY, false, null, 0, 0, 1);
        }

        static Row line() {
            return new Row(null, true, null, 0, 0, 1);
        }

        static Row image(Identifier[] ids, int w, int h, int span) {
            return new Row(null, false, ids, w, h, span);
        }
    }
    private boolean draggingBar;

    public GuideScreen(Screen parent, ProfessionType profession, boolean mandatory) {
        super(Component.translatable("screen.arz_classes.guide"));
        this.parent = parent;
        this.profession = profession;
        this.mandatory = mandatory;
    }

    // ---------------- построение ----------------

    @Override
    protected void init() {
        panelW = Math.min(MAX_W, this.width - 2 * MARGIN);
        panelH = Math.min(MAX_H, this.height - 2 * MARGIN);
        left = (this.width - panelW) / 2;
        top = (this.height - panelH) / 2;

        int tabRowH = TABS.size() > 1 ? TAB_ROW_H : 0;
        viewX = left + PAD;
        viewY = top + TITLE_H + tabRowH;
        viewW = panelW - 2 * PAD - SCROLLBAR_W - 4;
        viewH = (top + panelH - FOOTER_H) - viewY;
        visibleLines = Math.max(1, viewH / LINE_H);

        buildRows();
        scrolls[selected] = Math.max(0, Math.min(scrolls[selected], maxScroll()));
        checkFinished(); // вкладка без прокрутки считается прочитанной сразу, как только её открыли

        if (TABS.size() > 1) {
            int n = TABS.size();
            int tabW = Math.min(120, (panelW - 2 * PAD - (n - 1) * 2) / n);
            int totalW = n * tabW + (n - 1) * 2;
            int x = left + (panelW - totalW) / 2;
            for (int i = 0; i < n; i++) {
                final int idx = i;
                MutableComponent label = Component.translatable("guide.arz_classes.tab." + TABS.get(i).id());
                if (mandatory && !finished[i]) {
                    label.append(Component.literal(" *").withStyle(ChatFormatting.GOLD));
                }
                StyledButton tabButton = new StyledButton(x + i * (tabW + 2), top + TITLE_H - 4, tabW, TAB_H,
                        label, () -> selectTab(idx));
                tabButton.active = i != selected;
                addRenderableWidget(tabButton);
            }
        }

        StyledButton back = new StyledButton(left + panelW / 2 - BUTTON_W / 2,
                top + panelH - BUTTON_BOTTOM_MARGIN - BUTTON_H, BUTTON_W, BUTTON_H,
                Component.translatable("screen.arz_classes.back"), this::onClose);
        back.active = canLeave();
        addRenderableWidget(back);
    }

    private void selectTab(int index) {
        if (index == selected) {
            return;
        }
        selected = index;
        draggingBar = false;
        rebuildWidgets();
    }

    private void buildRows() {
        rows.clear();
        Tab tab = TABS.get(selected);
        String base = "guide.arz_classes." + tab.id() + ".";
        if (tab.perClass()) {
            addParagraphs(base + profession.getSerializedName() + ".");
            if (rows.isEmpty()) {
                addText(Component.translatable("guide.arz_classes.empty"));
            }
            addParagraphs(base + "common.");
        } else {
            addParagraphs(base);
            if (rows.isEmpty()) {
                addText(Component.translatable("guide.arz_classes.empty"));
            }
        }
        if (selected == TABS.size() - 1) {
            rows.add(Row.blank());
            addText(Component.translatable("guide.arz_classes.footer"));
        }
    }

    private void addText(Component text) {
        for (FormattedCharSequence line : this.font.split(text, viewW)) {
            rows.add(Row.text(line));
        }
    }

    /** Есть ли перевод: для отсутствующего ключа I18n.get возвращает сам ключ. */
    private static boolean hasKey(String key) {
        return !I18n.get(key).equals(key);
    }

    private void addParagraphs(String base) {
        boolean afterHeader = false;
        for (int i = 1; hasKey(base + i); i++) {
            String raw = I18n.get(base + i);
            if (!rows.isEmpty() && !afterHeader) {
                rows.add(Row.blank());
            }
            afterHeader = false;
            if (raw.startsWith("[img:") && raw.endsWith("]")) {
                addImage(raw.substring(5, raw.length() - 1));
                afterHeader = true; // подпись идёт вплотную к картинке
                continue;
            }
            addText(Component.translatable(base + i));
            if (raw.startsWith("\u00a76")) { // заголовок раздела — линия под ним
                rows.add(Row.line());
                afterHeader = true;
            }
        }
    }

    /** spec: "имя", "имя|ширина|высота" или несколько имён через запятую (ряд иконок): "a,b,c|28|28". */
    private void addImage(String spec) {
        String[] parts = spec.split("\\|");
        String[] names = parts[0].split(",");
        boolean multi = names.length > 1;
        int w = multi ? 32 : DEFAULT_IMG_W;
        int h = multi ? 32 : DEFAULT_IMG_H;
        try {
            if (parts.length >= 3) {
                w = Integer.parseInt(parts[1].trim());
                h = Integer.parseInt(parts[2].trim());
            }
        } catch (NumberFormatException ignored) {
            // оставляем размер по умолчанию
        }
        int gap = 4;
        int totalW = names.length * w + (names.length - 1) * gap;
        double k = Math.min(1.0, Math.min(viewW / (double) totalW, MAX_IMG_ROWS * LINE_H / (double) h));
        w = Math.max(1, (int) Math.round(w * k));
        h = Math.max(1, (int) Math.round(h * k));
        Identifier[] ids = new Identifier[names.length];
        for (int i = 0; i < names.length; i++) {
            ids[i] = Arz_classes.id("guide/" + names[i].trim());
        }
        int span = (h + LINE_H - 1) / LINE_H;
        rows.add(Row.image(ids, w, h, span));
        for (int i = 1; i < span; i++) {
            rows.add(Row.blank());
        }
    }

    // ---------------- прогресс чтения ----------------

    private int maxScroll() {
        return Math.max(0, rows.size() - visibleLines);
    }

    private boolean allFinished() {
        for (boolean f : finished) {
            if (!f) {
                return false;
            }
        }
        return true;
    }

    private boolean canLeave() {
        return !mandatory || allFinished();
    }

    /** true — если вкладка только что стала прочитанной. */
    private boolean checkFinished() {
        if (!finished[selected] && scrolls[selected] >= maxScroll()) {
            finished[selected] = true; // после ресайза окна не сбрасывается
            if (allFinished()) {
                GuideProgress.markSeen(profession);
            }
            return true;
        }
        return false;
    }

    private void setScroll(int value) {
        scrolls[selected] = Math.max(0, Math.min(value, maxScroll()));
        if (checkFinished()) {
            rebuildWidgets(); // обновить звёздочки на вкладках и кнопку «Назад»
        }
    }

    // ---------------- ввод ----------------

    @Override
    public boolean shouldCloseOnEsc() {
        return canLeave();
    }

    @Override
    public void onClose() {
        if (canLeave()) {
            Minecraft.getInstance().gui.setScreen(parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        setScroll(scrolls[selected] - (int) Math.signum(scrollY) * WHEEL_LINES);
        return true;
    }

    private int barX() {
        return left + panelW - PAD - SCROLLBAR_W;
    }

    private int trackH() {
        return visibleLines * LINE_H;
    }

    private int thumbH() {
        return Math.max(12, trackH() * visibleLines / Math.max(1, rows.size()));
    }

    private void dragTo(double mouseY) {
        int range = trackH() - thumbH();
        if (range <= 0) {
            return;
        }
        double ratio = (mouseY - viewY - thumbH() / 2.0) / range;
        setScroll((int) Math.round(Math.max(0, Math.min(1, ratio)) * maxScroll()));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() == 0 && maxScroll() > 0
                && event.x() >= barX() - 3 && event.x() <= barX() + SCROLLBAR_W + 3
                && event.y() >= viewY && event.y() <= viewY + trackH()) {
            draggingBar = true;
            dragTo(event.y());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingBar) {
            dragTo(event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingBar = false;
        return super.mouseReleased(event);
    }

    // ---------------- отрисовка ----------------

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        BookStyle.panel(graphics, left, top, panelW, panelH);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        graphics.centeredText(this.font,
                Component.translatable("screen.arz_classes.guide.title", profession.displayName()),
                left + panelW / 2, top + 12, ProfessionColors.of(profession));

        int scroll = scrolls[selected];
        int end = Math.min(rows.size(), scroll + visibleLines);
        // обрезка по окну текста: картинки, начавшиеся выше видимой области, показываются частично, а не пропадают
        graphics.enableScissor(viewX - 2, viewY, viewX + viewW + 2, viewY + trackH());
        for (int i = Math.max(0, scroll - MAX_IMG_ROWS); i < end; i++) {
            Row row = rows.get(i);
            int y = viewY + (i - scroll) * LINE_H;
            if (row.images() != null) {
                int gap = 4;
                int n = row.images().length;
                int x0 = viewX + (viewW - (n * row.w() + (n - 1) * gap)) / 2;
                for (int j = 0; j < n; j++) {
                    int x = x0 + j * (row.w() + gap);
                    graphics.blitSprite(RenderPipelines.GUI_TEXTURED, row.images()[j], x, y, row.w(), row.h());
                    if (n == 1 && row.w() >= 64) { // рамка только у больших картинок (скриншотов)
                        graphics.outline(x - 1, y - 1, row.w() + 2, row.h() + 2, BookStyle.line(0xFF555555));
                    }
                }
            } else if (i >= scroll && row.text() != null) {
                graphics.text(this.font, row.text(), viewX, y, BookStyle.body(0xFFCCCCCC));
            } else if (i >= scroll && row.divider()) {
                graphics.fill(viewX, y + 4, viewX + viewW, y + 5, BookStyle.line(0xFF555555));
            }
        }
        graphics.disableScissor();

        if (maxScroll() > 0) {
            int thumbH = thumbH();
            int thumbY = viewY + (trackH() - thumbH) * scroll / maxScroll();
            graphics.fill(barX(), viewY, barX() + SCROLLBAR_W, viewY + trackH(), BookStyle.well(0xE0201818));
            graphics.fill(barX(), thumbY, barX() + SCROLLBAR_W, thumbY + thumbH, ProfessionColors.of(profession));
            graphics.outline(barX() - 1, viewY - 1, SCROLLBAR_W + 2, trackH() + 2, BookStyle.line(0xFF555555));
        }

        if (!canLeave()) {
            String hintKey = TABS.size() > 1
                    ? "screen.arz_classes.guide.scroll_hint_tabs"
                    : "screen.arz_classes.guide.scroll_hint";
            graphics.centeredText(this.font, Component.translatable(hintKey),
                    left + panelW / 2, top + panelH - BUTTON_BOTTOM_MARGIN - BUTTON_H - 14, 0xFFFFAA00);
        }
    }
}