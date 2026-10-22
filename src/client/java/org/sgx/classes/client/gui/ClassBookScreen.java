package org.sgx.classes.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.client.ModKeyBindings;
import org.sgx.classes.client.ClientProfessionData;
import org.sgx.classes.item.BookKind;
import org.sgx.classes.item.ClassBookItem;
import org.sgx.classes.item.ModDataComponents;
import org.sgx.classes.network.BindHarvestBookPayload;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Экран книги со статистикой (урожай, кузнец, мистик, шахтёр, лесоруб): разворот на два листа, листается
 * стрелками (кнопки внизу, ← →). Что на листах — решает {@link ClassBookPages}. Данные читаются с самой книги,
 * поэтому экран обновляется вживую, пока открыт.
 */
public final class ClassBookScreen extends Screen {

    private static final int BOOK_W = 330;
    private static final int BOOK_H = 208;

    // Границы текста на страницах (в координатах текстуры, подогнаны под рамки и орнаменты)
    private static final int LEFT_X0 = 22, LEFT_X1 = 158;
    private static final int RIGHT_X0 = 172, RIGHT_X1 = 308;
    private static final int TITLE_Y = 14;
    private static final int SUB_Y = 33;
    private static final int TOTAL_Y = 44;
    private static final int LIST_Y = 62;
    private static final int ROW_H = 16;
    private static final int VISIBLE_ROWS = 8;
    private static final int VIEW_H = VISIBLE_ROWS * ROW_H;

    static final int INK = 0xFF3B2A1A;
    static final int INK_TITLE = 0xFF5B3A12;
    static final int INK_FADED = 0xFFA08C6C;
    static final int INK_GREEN = 0xFF2F5D1F;
    static final int INK_GOLD = 0xFF9A6A00;
    static final int INK_RED = 0xFF8B3A2F;
    static final int INK_BROWN = 0xFF6B3F1A;
    static final int INK_HINT = 0xFF8A7556;
    private static final int LEADER = 0x66806030;
    private static final int STRIPE = 0x16603A10;
    private static final int DIM_OVERLAY = 0x99E4D0A0;

    private static final String PEPPER_COLORS = "green,yellow,red,orange,white,pink,blue,purple,black";

    /** Плавный скролл одного списка: target — куда едем, pos — где сейчас (в пикселях). */
    private static final class Scroll {
        float pos;
        float target;

        float max(int rows) {
            return Math.max(0, rows * ROW_H - VIEW_H);
        }

        void clamp(int rows) {
            target = Math.max(0, Math.min(max(rows), target));
            pos = Math.max(0, Math.min(max(rows), pos));
        }
    }

    private final BookKind kind;
    private final Identifier texture;
    private final int accent;
    private final Supplier<ItemStack> source;
    private ClassBookPages content;
    private int pageCount = 1;
    private boolean opened = false;
    private int left, top;
    private int page = 0;
    // [страница * 2 + сторона]
    private final Scroll[] scrolls = new Scroll[8];
    private final int[] rowCounts = new int[8];
    private int draggingList = -1;
    private long lastFrameNanos = 0;

    private BookButton prevButton, nextButton;

    /** Книга из произвольного места (рука / слот Trinkets / инвентарь). */
    public ClassBookScreen(BookKind kind, Supplier<ItemStack> source) {
        super(Component.translatable("screen." + Arz_classes.MOD_ID + "." + kind.id()));
        this.kind = kind;
        this.source = source;
        this.texture = Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID,
                "textures/gui/book_" + kind.id().replace("_book", "") + ".png");
        // акцентный цвет чернил — тёмный оттенок цвета класса (ProfessionColors)
        int c = org.sgx.classes.profession.ProfessionColors.of(kind.profession());
        this.accent = 0xFF000000
                | ((int) (((c >> 16) & 0xFF) * 0.5) << 16)
                | ((int) (((c >> 8) & 0xFF) * 0.5) << 8)
                | (int) ((c & 0xFF) * 0.5);
        for (int i = 0; i < scrolls.length; i++) {
            scrolls[i] = new Scroll();
        }
    }

    /** ПКМ по книге в руке. */
    public static void openFromHand(BookKind kind, InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        mc.gui.setScreen(new ClassBookScreen(kind, () -> {
            var player = Minecraft.getInstance().player;
            return player == null ? ItemStack.EMPTY : player.getItemInHand(hand);
        }));
    }

    /**
     * Хоткей: книга текущего класса (иначе любая привязанная к игроку) из слота Trinkets/инвентаря.
     * Непривязанную сервер привяжет, привязанную — догонит историей из ванильной статистики.
     */
    public static void openFromKey(Minecraft mc) {
        var player = mc.player;
        if (player == null) {
            return;
        }
        ItemStack book = ClassBookItem.findForDisplay(player, ClientProfessionData.get().currentProfession());
        BookKind kind = BookKind.of(book);
        if (kind == null) {
            player.sendSystemMessage(Component.translatable("message.arz_classes.harvest_book_missing"));
            return;
        }
        ClientPlayNetworking.send(BindHarvestBookPayload.INSTANCE);
        mc.gui.setScreen(new ClassBookScreen(kind, () -> {
            var p = Minecraft.getInstance().player;
            if (p == null) {
                return ItemStack.EMPTY;
            }
            // тот же вид книги, свою (привязанную) — в приоритете
            String uuid = p.getUUID().toString();
            ItemStack fallback = ItemStack.EMPTY;
            for (ItemStack s : ClassBookItem.books(p, kind)) {
                if (uuid.equals(s.get(ModDataComponents.HARVEST_OWNER_ID))) {
                    return s;
                }
                if (fallback.isEmpty()) {
                    fallback = s;
                }
            }
            return fallback;
        }));
    }

    @Override
    protected void init() {
        this.left = (this.width - BOOK_W) / 2;
        boolean buttonsFit = this.height >= BOOK_H + 46;
        int contentH = BOOK_H + (buttonsFit ? 38 : 0);
        this.top = Math.max(4, (this.height - contentH) / 2);

        if (!opened) {
            opened = true;
            playSound("item.book.page_turn", 0.8f);
        }
        this.content = new ClassBookPages(kind);
        this.pageCount = content.build(Map.of(), null).size();

        if (buttonsFit) {
            int by = top + BOOK_H + 16;
            int cx = left + BOOK_W / 2;
            addRenderableWidget(new BookButton(cx - 50, by, 100, 20,
                    Component.translatable("screen.arz_classes.harvest_book.close"), this::onClose));
            prevButton = addRenderableWidget(new BookButton(cx - 82, by, 26, 20,
                    Component.literal("<"), () -> setPage(page - 1)));
            nextButton = addRenderableWidget(new BookButton(cx + 56, by, 26, 20,
                    Component.literal(">"), () -> setPage(page + 1)));
            updateNavButtons();
        }
    }

    /** Ванильные звуки книги по id (через SoundEvent.createVariableRangeEvent — не зависит от того, как они объявлены в SoundEvents). */
    private static void playSound(String id, float pitch) {
        Minecraft.getInstance().getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                net.minecraft.sounds.SoundEvent.createVariableRangeEvent(Identifier.withDefaultNamespace(id)), pitch));
    }

    @Override
    public void onClose() {
        playSound("item.book.put", 1.0f);
        super.onClose();
    }

    private void setPage(int p) {
        int old = this.page;
        this.page = Math.max(0, Math.min(pageCount - 1, p));
        if (this.page != old) {
            playSound("item.book.page_turn", 1.0f);
        }
        this.draggingList = -1;
        updateNavButtons();
    }

    private void updateNavButtons() {
        if (prevButton != null) {
            prevButton.active = page > 0;
        }
        if (nextButton != null) {
            nextButton.active = page < pageCount - 1;
        }
    }

    @Override
    public void tick() {
        // книгу убрали из инвентаря/руки/слота, пока экран был открыт — закрываем
        if (source.get().isEmpty()) {
            this.onClose();
        }
    }

    // ───────────────────────── ввод ─────────────────────────

    private int listAt(double mouseX) {
        return page * 2 + (mouseX < left + BOOK_W / 2.0 ? 0 : 1);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int list = listAt(mouseX);
        Scroll s = scrolls[list];
        s.target -= (float) scrollY * ROW_H * 1.5f;
        s.clamp(rowCounts[list]);
        return true;
    }

    private int trackX(int list) {
        return left + ((list & 1) == 0 ? LEFT_X1 : RIGHT_X1) + 1;
    }

    private void dragTo(int list, double mouseY) {
        int rows = rowCounts[list];
        Scroll s = scrolls[list];
        float max = s.max(rows);
        if (max <= 0) {
            return;
        }
        int thumbH = thumbHeight(rows);
        double ratio = (mouseY - (top + LIST_Y) - thumbH / 2.0) / (VIEW_H - thumbH);
        s.target = (float) Math.max(0, Math.min(1, ratio)) * max;
        s.pos = s.target; // за ползунком едем без «резинки»
    }

    private static int thumbHeight(int rows) {
        return Math.max(10, VIEW_H * VISIBLE_ROWS / Math.max(rows, 1));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() == 0) {
            int list = listAt(event.x());
            int tx = trackX(list);
            if (rowCounts[list] > VISIBLE_ROWS
                    && event.x() >= tx - 3 && event.x() <= tx + 5
                    && event.y() >= top + LIST_Y && event.y() <= top + LIST_Y + VIEW_H) {
                draggingList = list;
                dragTo(list, event.y());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (draggingList >= 0) {
            dragTo(draggingList, event.y());
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingList = -1;
        return super.mouseReleased(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // тот же хоткей, что открыл книгу, её и закрывает
        if (ModKeyBindings.OPEN_BOOK.matches(event)) {
            this.onClose();
            return true;
        }
        if (event.key() == InputConstants.KEY_LEFT) {
            setPage(page - 1);
            return true;
        }
        if (event.key() == InputConstants.KEY_RIGHT) {
            setPage(page + 1);
            return true;
        }
        return super.keyPressed(event);
    }

    // ───────────────────────── отрисовка ─────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, left, top, 0, 0, BOOK_W, BOOK_H, BOOK_W, BOOK_H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        // шаг плавного скролла: экспоненциальное затухание, не зависит от FPS
        long now = System.nanoTime();
        float dt = lastFrameNanos == 0 ? 0.016f : Math.min(0.1f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;
        float ease = 1f - (float) Math.exp(-dt * 16f);
        for (Scroll s : scrolls) {
            s.pos += (s.target - s.pos) * ease;
            if (Math.abs(s.target - s.pos) < 0.05f) {
                s.pos = s.target;
            }
        }

        ItemStack book = source.get();
        Map<String, Integer> stats = ClassBookItem.stats(book);
        String owner = book.get(ModDataComponents.HARVEST_OWNER_NAME);

        List<ClassBookPages.Page> pages = content.build(stats, owner);
        page = Math.max(0, Math.min(pages.size() - 1, page));
        ClassBookPages.Page current = pages.get(page);
        drawPanel(graphics, page * 2, current.left(), left + LEFT_X0, left + LEFT_X1);
        drawPanel(graphics, page * 2 + 1, current.right(), left + RIGHT_X0, left + RIGHT_X1);

        // номер страницы под книгой — там же, где кнопки листания (владелец теперь на самой странице)
        Component pageLabel = Component.translatable("screen.arz_classes.harvest_book.page", page + 1, pageCount);
        graphics.text(this.font, pageLabel, left + (BOOK_W - this.font.width(pageLabel)) / 2,
                top + BOOK_H + 4, 0xFFE8D5A8, true);
    }

    private void drawPanel(GuiGraphicsExtractor graphics, int list, ClassBookPages.Panel panel, int x0, int x1) {
        if (panel == null) {
            rowCounts[list] = 0;
            return;
        }
        int cx = (x0 + x1) / 2;
        drawCentered(graphics, panel.title().copy().withStyle(ChatFormatting.BOLD), cx, top + TITLE_Y, accent);
        if (panel.sub() != null) {
            drawCentered(graphics, panel.sub(), cx, top + SUB_Y, panel.subColor());
        }
        if (panel.total() != null) {
            drawCentered(graphics, panel.total(), cx, top + TOTAL_Y, panel.totalColor());
        }
        if (panel.rows().isEmpty() && panel.emptyText() != null) {
            rowCounts[list] = 0;
            drawCentered(graphics, panel.emptyText(), cx, top + LIST_Y + VIEW_H / 2 - 4, INK_FADED);
            return;
        }
        drawList(graphics, list, panel.rows(), x0, x1, accent);
    }

    /** Текст без тени (на пергаменте тень выглядит грязно), по центру x. */
    private void drawCentered(GuiGraphicsExtractor graphics, Component text, int cx, int y, int color) {
        int maxW = LEFT_X1 - LEFT_X0; // длинный ник не должен вылезать за лист
        if (this.font.width(text) > maxW) {
            text = Component.literal(fit(text, maxW)).withStyle(text.getStyle());
        }
        graphics.text(this.font, text, cx - this.font.width(text) / 2, y, color, false);
    }

    /** Подрезает слишком длинное название с «…», чтобы оно не наезжало на число. */
    private String fit(Component name, int maxWidth) {
        String s = name.getString();
        if (this.font.width(s) <= maxWidth) {
            return s;
        }
        while (s.length() > 1 && this.font.width(s + "…") > maxWidth) {
            s = s.substring(0, s.length() - 1);
        }
        return s + "…";
    }

    /** Рисует список «иконка — название ····· число» с плавным скроллом и полосой прокрутки. */
    private void drawList(GuiGraphicsExtractor graphics, int list, List<ClassBookPages.Row> rows, int x0, int x1, int countColor) {
        rowCounts[list] = rows.size();
        Scroll scroll = scrolls[list];
        scroll.clamp(rows.size());

        int viewTop = top + LIST_Y;
        int offset = Math.round(scroll.pos);

        graphics.enableScissor(x0 - 1, viewTop, x1 + 1, viewTop + VIEW_H);
        int first = Math.max(0, offset / ROW_H);
        int last = Math.min(rows.size() - 1, (offset + VIEW_H) / ROW_H);
        for (int i = first; i <= last; i++) {
            ClassBookPages.Row row = rows.get(i);
            int y = viewTop + i * ROW_H - offset;
            boolean zero = row.count() == 0;

            if ((i & 1) == 0) {
                graphics.fill(x0, y, x1, y + ROW_H, STRIPE);
            }

            if (!row.icon().isEmpty()) {
                graphics.item(row.icon(), x0 + 1, y);
                if (zero) {
                    // ещё не собрано — иконка «выцветает» под пергамент
                    graphics.fill(x0 + 1, y, x0 + 17, y + 16, DIM_OVERLAY);
                }
            }

            int textY = y + 4;
            int nameX = x0 + 21;
            String countText = String.valueOf(row.count());
            int countX = x1 - 4 - this.font.width(countText);

            String name = fit(row.name(), countX - nameX - 8);
            graphics.text(this.font, name, nameX, textY, zero ? INK_FADED : (row.nameColor() != 0 ? row.nameColor() : INK), false);
            graphics.text(this.font, countText, countX, textY, zero ? INK_FADED : countColor, false);

            // пунктирная линия-«выноска» между названием и числом
            for (int dx = nameX + this.font.width(name) + 5; dx < countX - 4; dx += 3) {
                graphics.fill(dx, y + 11, dx + 1, y + 12, LEADER);
            }
        }
        graphics.disableScissor();

        if (rows.size() > VISIBLE_ROWS) {
            float max = scroll.max(rows.size());
            int thumbH = thumbHeight(rows.size());
            int thumbY = viewTop + Math.round((VIEW_H - thumbH) * (max <= 0 ? 0 : scroll.pos / max));
            int tx = trackX(list);
            graphics.fill(tx, viewTop, tx + 3, viewTop + VIEW_H, 0x33000000);
            graphics.fill(tx, thumbY, tx + 3, thumbY + thumbH, draggingList == list ? 0xFF7A4A18 : 0xAA5B3A12);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
