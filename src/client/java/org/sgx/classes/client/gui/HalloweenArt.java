package org.sgx.classes.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * Хэллоуинские пиксель-спрайты для темы PUMPKIN в BookStyle: тыква-фонарь, конфета, летучая мышь, паутина.
 * Рисуются через fill() сериями одного цвета в строке (не по пикселю), текстуры не нужны.
 */
final class HalloweenArt {
    private HalloweenArt() {
    }

    // ---- спрайты (символ -> цвет в KEYS/COLORS соответствующего набора) ----
    private static final String[] PUMPKIN = {
            "...s...",
            ".OOsOO.",
            "OOOOOOO",
            "OyOOOyO",
            "OOOyOOO",
            "OyOyOyO",
            ".DDDDD.",
    };
    private static final String PUMPKIN_KEYS = "sODy";
    private static final int[] PUMPKIN_COLORS = {0xFF4E7A22, 0xFFEE7B14, 0xFFA84A08, 0xFFFFE066};

    private static final String[] CANDY = {
            "a.bbbbb.a",
            "aabcccbaa",
            "a.bbbbb.a",
    };
    private static final String CANDY_KEYS = "abc";
    private static final int[] CANDY_COLORS = {0xFFF2E6C8, 0xFF8E3FC9, 0xFFC98AF2};

    private static final String[] BAT = {
            "bb.....bb",
            "bbbb.bbbb",
            ".bbbbbbb.",
            "..b...b..",
    };
    private static final String BAT_KEYS = "b";
    private static final int[] BAT_COLORS = {0xFF1A0E26};

    private static final String[] WEB = {
            "########",
            "##.#..#.",
            "#.#...#.",
            "##.#.#..",
            "#...##..",
            "#..###..",
            "###...#.",
            "#......#",
    };
    private static final String WEB_KEYS = "#";
    private static final int[] WEB_COLORS = {0x70FFF1D6};

    private static final String[] SKULL = {
            ".WWW.",
            "WKWKW",
            "WWWWW",
            ".WKW.",
            ".W.W.",
    };
    private static final String SKULL_KEYS = "WK";
    private static final int[] SKULL_COLORS = {0xFFEDE6D0, 0xFF1A0E26};

    private static final String[] GHOST = {
            "..WWW..",
            ".WWWWW.",
            "WKWWWKW",
            "WWWKWWW",
            "WWWWWWW",
            "WWWWWWW",
            "WW.W.WW",
    };
    private static final String GHOST_KEYS = "WK";
    private static final int[] GHOST_COLORS = {0xFFF4F4FF, 0xFF2A1840};

    private static final String[] CORN = {
            "..W..",
            ".WWW.",
            ".OOO.",
            "OOOOO",
            "YYYYY",
    };
    private static final String CORN_KEYS = "WOY";
    private static final int[] CORN_COLORS = {0xFFFFF4DD, 0xFFFF8A1E, 0xFFFFD23A};

    private static final String[] SPIDER = {
            "K.K.K",
            ".KKK.",
            "KKRKK",
            "K.K.K",
    };
    private static final String SPIDER_KEYS = "KR";
    private static final int[] SPIDER_COLORS = {0xFF14081E, 0xFFE02020};

    private static final int[] STITCHES = {0xFFFFD23A, 0xFFFF8A1E, 0xFFFFF4DD};
    private static final int[] LIGHTS = {0xFFFFD23A, 0xFFB05CFF, 0xFFFFF4DD, 0xFFFF6A1A};

    static final int PUMPKIN_SIZE = 7;
    static final int CANDY_W = 9;

    private static void sprite(GuiGraphicsExtractor g, int x, int y, String[] rows, String keys, int[] colors,
                               boolean mirrorX) {
        for (int r = 0; r < rows.length; r++) {
            String row = rows[r];
            int w = row.length();
            int c = 0;
            while (c < w) {
                int sc = mirrorX ? w - 1 - c : c;
                char ch = row.charAt(sc);
                int idx = keys.indexOf(ch);
                if (idx < 0) {
                    c++;
                    continue;
                }
                int start = c;
                while (c < w && row.charAt(mirrorX ? w - 1 - c : c) == ch) {
                    c++;
                }
                g.fill(x + start, y + r, x + c, y + r + 1, colors[idx]);
            }
        }
    }

    static void pumpkin(GuiGraphicsExtractor g, int x, int y) {
        sprite(g, x, y, PUMPKIN, PUMPKIN_KEYS, PUMPKIN_COLORS, false);
    }

    static void candy(GuiGraphicsExtractor g, int x, int y) {
        sprite(g, x, y, CANDY, CANDY_KEYS, CANDY_COLORS, false);
    }

    static void bat(GuiGraphicsExtractor g, int x, int y) {
        sprite(g, x, y, BAT, BAT_KEYS, BAT_COLORS, false);
    }

    static void web(GuiGraphicsExtractor g, int x, int y, boolean right) {
        sprite(g, x, y, WEB, WEB_KEYS, WEB_COLORS, right);
    }

    static int stitchColor(int index) {
        return STITCHES[Math.floorMod(index, STITCHES.length)];
    }

    /**
     * Украшения панели PUMPKIN: паутина с паучком в верхних углах, летучие мыши и череп на верхней кромке,
     * гирлянда огоньков по бокам, привидение и конфеты-«кукурузинки» на нижней кромке, тыквы на нижних углах.
     */
    static void panelDecor(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean corners) {
        int cx = x + w / 2;
        // верх
        web(g, x + 4, y + 4, false);
        web(g, x + w - 12, y + 4, true);
        bat(g, x + w / 4 - 4, y - 1);
        bat(g, x + w - w / 4 - 4, y - 1);
        sprite(g, cx - 2, y - 3, SKULL, SKULL_KEYS, SKULL_COLORS, false);
        // паучок висит на ниточке под правой паутиной
        g.fill(x + w - 9, y + 12, x + w - 8, y + 19, 0x90FFFFFF);
        sprite(g, x + w - 11, y + 19, SPIDER, SPIDER_KEYS, SPIDER_COLORS, false);
        // гирлянда по бокам
        int i = 0;
        for (int ly = y + 18; ly < y + h - 18; ly += 12, i++) {
            int c = LIGHTS[i % LIGHTS.length];
            g.fill(x + 1, ly, x + 3, ly + 2, c);
            g.fill(x + w - 3, ly, x + w - 1, ly + 2, c);
        }
        // низ
        sprite(g, cx - 3, y + h - 5, GHOST, "WK", GHOST_COLORS, false);
        sprite(g, x + w / 4 - 2, y + h - 3, CORN, CORN_KEYS, CORN_COLORS, false);
        sprite(g, x + w - w / 4 - 2, y + h - 3, CORN, CORN_KEYS, CORN_COLORS, false);
        if (corners) {
            pumpkin(g, x - 2, y + h - 5);
            pumpkin(g, x + w - 5, y + h - 5);
        }
    }

    /** Разделитель PUMPKIN: тыква по центру, конфеты по краям (если хватает ширины). */
    static void divider(GuiGraphicsExtractor g, int cx, int y, int halfWidth, int lineColor) {
        pumpkin(g, cx - 3, y - 3);
        int from = cx - halfWidth;
        int to = cx + halfWidth;
        if (halfWidth >= 24) {
            candy(g, from, y - 1);
            candy(g, to - CANDY_W, y - 1);
            from += CANDY_W + 2;
            to -= CANDY_W + 2;
        }
        g.fill(from, y, cx - 5, y + 1, lineColor);
        g.fill(cx + 5, y, to, y + 1, lineColor);
    }
}