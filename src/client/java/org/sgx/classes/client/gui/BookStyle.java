package org.sgx.classes.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;

/**
 * «Книжный фэнтези»-стиль окон мода: кожа/пергамент, кожаный кант, золотая линия и уголки, кожаные кнопки.
 * <p>
 * Тема выбирается в окне {@link StyleScreen} (шестерёнка в окне классов): классический (плоский) → книга светлая →
 * книга тёмная → зелёная → фиолетовая → тыквенная (Хэллоуин, заперта до разблокировки модом arzmine-halloween).
 * Выбор хранится в config/arz_classes/ui_style.txt (имя темы) и действует на ВСЕ окна мода.
 * По умолчанию «книга светлая».
 * <p>
 * Все методы рисования есть в двух вариантах: без темы (берут текущую) и с явной темой — последние нужны
 * предпросмотру в {@link StyleScreen}, который рисует окно в ещё не применённой теме.
 */
public final class BookStyle {

    /** Тема: цвета панели и кожаных кнопок. */
    public enum Theme {
        CLASSIC("classic", 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0),
        /** Тёплая светлая кожа (как была по умолчанию). */
        BROWN("brown", 0xF0392616, 0xFF6E4527, 0xFF4A2E18, 0xFFA77B2E, 0xFFE0B04A,
                0xFF4A2C17, 0xFF63391C, 0xFF3A2210, 0xFF6B4426, 0xFF2A180C, 0xFFC79A4A, 0xFFE8D5A8, 0xFFFFF0C2, 0xFF7A6A52),
        /** Тёмная кожа (первый вариант). */
        DARK("dark", 0xF01E130B, 0xFF5A3820, 0xFF3A2312, 0xFFA77B2E, 0xFFE0B04A,
                0xFF3A2210, 0xFF52301A, 0xFF2A180C, 0xFF5A3820, 0xFF1A0E06, 0xFFB38840, 0xFFE8D5A8, 0xFFFFF0C2, 0xFF6A5A44),
        /** Зелёный переплёт. */
        GREEN("green", 0xF01E3322, 0xFF3F6B3E, 0xFF284A2A, 0xFFB7A24A, 0xFFE8D27A,
                0xFF2C4F2E, 0xFF3B6A3D, 0xFF1F3A21, 0xFF4E8250, 0xFF12200F, 0xFFC9B85A, 0xFFE6F0D0, 0xFFFFFFE0, 0xFF6A8A6A),
        /** Фиолетовый переплёт. */
        PURPLE("purple", 0xF02A1A3A, 0xFF5B3A80, 0xFF3D2459, 0xFFB79A4A, 0xFFE8CE7A,
                0xFF43296A, 0xFF58368A, 0xFF2E1B4A, 0xFF6E4A9C, 0xFF190E2A, 0xFFC9AE5E, 0xFFEBDDF5, 0xFFFFF0FF, 0xFF7C6A94),
        /** Тыквенная (Хэллоуин): открывается модом arzmine-halloween после 50 вырезанных тыкв. Рамка янтарная. */
        PUMPKIN("pumpkin", 0xF0241008, 0xFFD2690E, 0xFF8A3F07, 0xFFFFB347, 0xFFFFE08A,
                0xFFB85A0C, 0xFFD9730F, 0xFF4A2A10, 0xFFF09A3A, 0xFF2B1204, 0xFF3A1A06, 0xFFFFE2B0, 0xFFFFF4D6, 0xFF8A6A4A);

        final String id;
        final int fill, leather, leatherDark, gold, goldBright;
        final int btn, btnHover, btnDisabled, btnHighlight, btnOutline, stitch, text, textHover, textDisabled;

        Theme(String id, int fill, int leather, int leatherDark, int gold, int goldBright,
              int btn, int btnHover, int btnDisabled, int btnHighlight, int btnOutline, int stitch,
              int text, int textHover, int textDisabled) {
            this.id = id;
            this.fill = fill;
            this.leather = leather;
            this.leatherDark = leatherDark;
            this.gold = gold;
            this.goldBright = goldBright;
            this.btn = btn;
            this.btnHover = btnHover;
            this.btnDisabled = btnDisabled;
            this.btnHighlight = btnHighlight;
            this.btnOutline = btnOutline;
            this.stitch = stitch;
            this.text = text;
            this.textHover = textHover;
            this.textDisabled = textDisabled;
        }

        public String id() {
            return id;
        }

        /** Цвет заголовков. */
        int title() {
            return this == CLASSIC ? 0xFFFFD46B : goldBright;
        }

        /** Цвет основного текста. */
        int body() {
            return this == CLASSIC ? 0xFFCCCCCC : text;
        }

        /** Приглушённый текст (подписи, значения второго плана). */
        int dim() {
            return this == CLASSIC ? 0xFFAAAAAA : mix(text, fill | 0xFF000000, 0.35f);
        }

        /** Цвет тонких линий (разделители, рамки списков). */
        int line() {
            return this == CLASSIC ? 0xFF555555 : (gold & 0x00FFFFFF) | 0xA0000000;
        }

        /** Тёмная «впадина» под списками, полосками и слотами. */
        int well() {
            return this == CLASSIC ? 0xE0201818 : 0xF0000000 | (mix(btnOutline, leatherDark, 0.55f) & 0x00FFFFFF);
        }
    }

    private static final int OUTER = 0xFF140A04;

    private static Theme theme;

    private static final java.util.Map<Theme, java.util.function.BooleanSupplier> UNLOCKS =
            new java.util.EnumMap<>(Theme.class);

    /** Запирает тему условием (регистрируют другие моды). Пока условие ложно, тему нельзя выбрать и применить. */
    public static void setUnlock(Theme t, java.util.function.BooleanSupplier condition) {
        UNLOCKS.put(t, condition);
    }

    private static final java.util.Map<Theme, Component> LOCK_HINTS = new java.util.EnumMap<>(Theme.class);

    /** То же, плюс подсказка, как открыть тему (показывается в окне выбора оформления). */
    public static void setUnlock(Theme t, java.util.function.BooleanSupplier condition, Component hint) {
        UNLOCKS.put(t, condition);
        LOCK_HINTS.put(t, hint);
    }

    /** Подсказка, как открыть тему; null, если её не задали. */
    public static Component lockHint(Theme t) {
        return LOCK_HINTS.get(t);
    }

    /** Темы, закрытые по умолчанию: открываются только когда другой мод подтвердил условие (и дальше помнятся). */
    private static final java.util.Set<Theme> LOCKED_BY_DEFAULT = java.util.EnumSet.of(Theme.PUMPKIN);

    private static java.util.Set<Theme> earned;

    private static java.nio.file.Path earnedFile() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
                .resolve("arz_classes").resolve("unlocked_themes.txt");
    }

    /** Заработанные ранее темы (файл читается один раз). Нужно, чтобы тема не пропала, если мод-ивент уберут. */
    private static java.util.Set<Theme> earned() {
        if (earned == null) {
            earned = java.util.EnumSet.noneOf(Theme.class);
            try {
                java.nio.file.Path f = earnedFile();
                if (java.nio.file.Files.exists(f)) {
                    for (String line : java.nio.file.Files.readAllLines(f)) {
                        for (Theme t : Theme.values()) {
                            if (t.id.equals(line.trim().toLowerCase())) {
                                earned.add(t);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // нет файла — ничего не заработано
            }
        }
        return earned;
    }

    private static void remember(Theme t) {
        if (!earned().add(t)) {
            return;
        }
        try {
            java.nio.file.Path f = earnedFile();
            java.nio.file.Files.createDirectories(f.getParent());
            StringBuilder sb = new StringBuilder();
            for (Theme e : earned()) {
                sb.append(e.id).append('\n');
            }
            java.nio.file.Files.writeString(f, sb.toString());
        } catch (Exception ignored) {
            // не сохранилось — откроется заново при следующем подтверждении
        }
    }

    /** Доступна ли тема игроку. Обычные темы — всегда; закрытые по умолчанию — если заработаны сейчас или раньше. */
    public static boolean isUnlocked(Theme t) {
        if (!LOCKED_BY_DEFAULT.contains(t)) {
            java.util.function.BooleanSupplier c = UNLOCKS.get(t);
            return c == null || c.getAsBoolean();
        }
        if (earned().contains(t)) {
            return true;
        }
        java.util.function.BooleanSupplier c = UNLOCKS.get(t);
        if (c != null && c.getAsBoolean()) {
            remember(t);
            return true;
        }
        return false;
    }

    private BookStyle() {
    }

    private static java.nio.file.Path file() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir()
                .resolve("arz_classes").resolve("ui_style.txt");
    }

    /**
     * Текущая тема (читается из файла при первом обращении). Если выбранная тема сейчас заперта
     * (например, тыквенная, пока не вырезано 50 тыкв), временно показывается «книга светлая»,
     * а сохранённый выбор не трогается.
     */
    public static Theme theme() {
        if (theme == null) {
            Theme value = Theme.BROWN;
            try {
                java.nio.file.Path f = file();
                if (java.nio.file.Files.exists(f)) {
                    String s = java.nio.file.Files.readString(f).trim().toLowerCase();
                    for (Theme t : Theme.values()) {
                        if (t.id.equals(s)) {
                            value = t;
                        }
                    }
                }
            } catch (Exception ignored) {
                // нет файла или нет доступа — остаётся тема по умолчанию
            }
            theme = value;
        }
        return isUnlocked(theme) ? theme : Theme.BROWN;
    }

    /** Включён ли книжный стиль (любая тема, кроме классической). */
    public static boolean enabled() {
        return theme() != Theme.CLASSIC;
    }

    /** Применяет тему ко всем окнам мода; выбор запоминается. Запертую тему применить нельзя. */
    public static void set(Theme value) {
        if (!isUnlocked(value)) {
            return;
        }
        theme = value;
        try {
            java.nio.file.Path f = file();
            java.nio.file.Files.createDirectories(f.getParent());
            java.nio.file.Files.writeString(f, value.id);
        } catch (Exception ignored) {
            // выбор просто не сохранится между запусками
        }
    }

    /** Следующая доступная тема по кругу (запертые пропускаются); выбор запоминается. */
    public static void next() {
        Theme[] all = Theme.values();
        int from = theme().ordinal();
        for (int k = 1; k <= all.length; k++) {
            Theme candidate = all[(from + k) % all.length];
            if (isUnlocked(candidate)) {
                set(candidate);
                return;
            }
        }
    }

    // ---------------- цвета текста для экранов (в классической теме возвращают исходный цвет экрана) ----------------

    public static int title(int classicColor) {
        return enabled() ? theme().title() : classicColor;
    }

    public static int body(int classicColor) {
        return enabled() ? theme().body() : classicColor;
    }

    public static int dim(int classicColor) {
        return enabled() ? theme().dim() : classicColor;
    }

    public static int line(int classicColor) {
        return enabled() ? theme().line() : classicColor;
    }

    public static int well(int classicColor) {
        return enabled() ? theme().well() : classicColor;
    }

    /** Смешивает два RGB-цвета (альфа берётся от a). */
    static int mix(int a, int b, float f) {
        int r = Math.round(((a >> 16) & 0xFF) * (1 - f) + ((b >> 16) & 0xFF) * f);
        int g = Math.round(((a >> 8) & 0xFF) * (1 - f) + ((b >> 8) & 0xFF) * f);
        int bl = Math.round((a & 0xFF) * (1 - f) + (b & 0xFF) * f);
        return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
    }

    // ---------------- панель ----------------

    /** Фон панели. */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        panel(g, x, y, w, h, theme(), true);
    }

    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, Theme t) {
        panel(g, x, y, w, h, t, true);
    }

    /** Панель; corners = false — без угловых украшений (для контейнеров, где в углах стоят слоты). */
    public static void panel(GuiGraphicsExtractor g, int x, int y, int w, int h, Theme t, boolean corners) {
        if (t == Theme.CLASSIC) {
            g.fill(x, y, x + w, y + h, 0xC0101010);
            return;
        }
        g.fill(x, y, x + w, y + h, t.fill);
        // лёгкий «свет» сверху, будто от свечи
        g.fill(x + 4, y + 4, x + w - 4, y + 12, 0x14FFD9A0);
        g.outline(x, y, w, h, OUTER);
        g.outline(x + 1, y + 1, w - 2, h - 2, t.leather);
        g.outline(x + 2, y + 2, w - 4, h - 4, t.leatherDark);
        g.outline(x + 3, y + 3, w - 6, h - 6, t.gold);
        if (t == Theme.PUMPKIN) {
            // Хэллоуин: паутина, летучие мыши, тыквы-фонарики вместо ромбиков
            HalloweenArt.panelDecor(g, x, y, w, h, corners);
            return;
        }
        if (corners) {
            diamond(g, x + 7, y + 7, t.goldBright);
            diamond(g, x + w - 8, y + 7, t.goldBright);
            diamond(g, x + 7, y + h - 8, t.goldBright);
            diamond(g, x + w - 8, y + h - 8, t.goldBright);
        }
    }

    private static void diamond(GuiGraphicsExtractor g, int cx, int cy, int color) {
        g.fill(cx - 1, cy, cx + 2, cy + 1, color);
        g.fill(cx, cy - 1, cx + 1, cy + 2, color);
    }

    /** Декоративный разделитель с ромбиком по центру. В классической теме ничего не рисует. */
    public static void divider(GuiGraphicsExtractor g, int cx, int y, int halfWidth) {
        divider(g, cx, y, halfWidth, theme());
    }

    public static void divider(GuiGraphicsExtractor g, int cx, int y, int halfWidth, Theme t) {
        if (t == Theme.CLASSIC) {
            return;
        }
        int dim = (t.gold & 0x00FFFFFF) | 0x99000000;
        if (t == Theme.PUMPKIN) {
            // конфета — тыква — конфета
            HalloweenArt.divider(g, cx, y, halfWidth, dim);
            return;
        }
        g.fill(cx - halfWidth, y, cx - 5, y + 1, dim);
        g.fill(cx + 5, y, cx + halfWidth, y + 1, dim);
        diamond(g, cx, y, t.goldBright);
    }

    // ---------------- элементы ----------------

    /** Кожаная кнопка (используется StyledButton / ClassesTabButton в книжных темах). */
    public static void button(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hovered, boolean active,
                              Component label) {
        button(g, x, y, w, h, hovered, active, label, theme());
    }

    public static void button(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean hovered, boolean active,
                              Component label, Theme t) {
        var font = Minecraft.getInstance().font;
        if (t == Theme.CLASSIC) {
            boolean hot = hovered && active;
            g.fill(x, y, x + w, y + h, !active ? 0xFF17171A : (hot ? 0xFF2E2E36 : 0xFF1F1F24));
            g.outline(x, y, w, h, !active ? 0xFF2A2A2E : (hot ? 0xFFFFD46B : 0xFF4A4A52));
            if (active) {
                g.fill(x + 1, y + 1, x + w - 1, y + 2, hot ? 0x30FFD46B : 0x14FFFFFF);
            }
            if (label != null) {
                g.centeredText(font, label, x + w / 2, y + (h - 8) / 2,
                        !active ? 0xFF6A6A70 : (hot ? 0xFFFFD46B : 0xFFE6E6E6));
            }
            return;
        }
        g.fill(x, y, x + w, y + h, !active ? t.btnDisabled : (hovered ? t.btnHover : t.btn));
        g.fill(x + 1, y + 1, x + w - 1, y + 2, t.btnHighlight);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, t.btnDisabled);
        g.outline(x, y, w, h, t.btnOutline);
        g.outline(x + 2, y + 2, w - 4, h - 4, hovered && active ? t.goldBright : t.gold);
        int stitchIndex = 0;
        for (int sx = x + 6; sx < x + w - 6; sx += 4, stitchIndex++) {
            // в тыквенной теме «стежки» — разноцветные конфетные точки
            int sc = t == Theme.PUMPKIN ? HalloweenArt.stitchColor(stitchIndex) : t.stitch;
            g.fill(sx, y + 1, sx + 1, y + 2, sc);
            g.fill(sx, y + h - 2, sx + 1, y + h - 1, sc);
        }
        if (label != null) {
            g.text(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2 + 1,
                    !active ? t.textDisabled : (hovered ? t.textHover : t.text), false);
        }
    }

    /** Тёмная «впадина»: фон полосок, выпадающих списков, окон прокрутки. */
    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        inset(g, x, y, w, h, theme());
    }

    public static void inset(GuiGraphicsExtractor g, int x, int y, int w, int h, Theme t) {
        g.fill(x, y, x + w, y + h, t.well());
        g.outline(x, y, w, h, t == Theme.CLASSIC ? 0xFF555555 : t.leather);
    }

    /** Слот 18×18 (x, y — левый верхний угол рамки, т.е. координата слота минус 1). */
    public static void slot(GuiGraphicsExtractor g, int x, int y) {
        slot(g, x, y, theme());
    }

    public static void slot(GuiGraphicsExtractor g, int x, int y, Theme t) {
        if (t == Theme.CLASSIC) {
            g.fill(x, y, x + 18, y + 18, 0xFF373737);
            g.outline(x, y, 18, 18, 0xFF000000);
            return;
        }
        g.fill(x, y, x + 18, y + 18, 0xFF000000 | (t.btnOutline & 0x00FFFFFF));
        // «вдавленная» фаска: тёмные верх/лево, светлые низ/право
        g.fill(x, y, x + 18, y + 1, OUTER);
        g.fill(x, y, x + 1, y + 18, OUTER);
        g.fill(x, y + 17, x + 18, y + 18, t.btnHighlight);
        g.fill(x + 17, y, x + 18, y + 18, t.btnHighlight);
    }

    /** Строка списка (рецепты, темы). */
    public static void row(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean selected, boolean hovered) {
        row(g, x, y, w, h, selected, hovered, theme());
    }

    public static void row(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean selected, boolean hovered,
                           Theme t) {
        if (t == Theme.CLASSIC) {
            g.fill(x, y, x + w, y + h, selected ? 0xFF3A3620 : (hovered ? 0xFF2A2222 : 0xFF201818));
            g.outline(x, y, w, h, selected ? 0xFFFFD46B : 0xFF3A3A3A);
            return;
        }
        g.fill(x, y, x + w, y + h, selected ? t.btnHover : (hovered ? t.btn : t.btnDisabled));
        g.fill(x + 1, y + 1, x + w - 1, y + 2, selected ? t.btnHighlight : 0x10FFFFFF);
        g.outline(x, y, w, h, selected ? t.goldBright : t.btnOutline);
    }

    /** Полоска прогресса (0..1) с делениями. */
    public static void bar(GuiGraphicsExtractor g, int x, int y, int w, int h, float progress, int color, Theme t) {
        inset(g, x, y, w, h, t);
        int filled = Math.round((w - 2) * Math.max(0f, Math.min(1f, progress)));
        g.fill(x + 1, y + 1, x + 1 + filled, y + h - 1, color);
        for (int i = 1; i < 10; i++) {
            int tx = x + Math.round(w * (float) i / 10);
            g.fill(tx, y + 1, tx + 1, y + h - 1, 0x70000000);
        }
    }

    /** Маленькая стрелка вправо 9×7 (между сеткой рецепта и результатом). */
    public static void arrow(GuiGraphicsExtractor g, int x, int y, int color) {
        g.fill(x, y + 3, x + 6, y + 4, color);
        g.fill(x + 6, y + 1, x + 7, y + 6, color);
        g.fill(x + 7, y + 2, x + 8, y + 5, color);
        g.fill(x + 8, y + 3, x + 9, y + 4, color);
    }

    /**
     * Фон контейнерного экрана (верстак, стол редкости, мешочки): панель на весь экран + рамка под каждым слотом меню.
     * В классической теме не используется — там остаются родные PNG-текстуры.
     */
    public static void containerBackground(GuiGraphicsExtractor g, int x, int y, int w, int h,
                                           Iterable<? extends Slot> slots) {
        panel(g, x, y, w, h, theme(), false);
        for (Slot s : slots) {
            slot(g, x + s.x - 1, y + s.y - 1);
        }
    }
}