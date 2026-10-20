package org.sgx.classes.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.client.ModKeyBindings;
import org.sgx.classes.item.HarvestBookItem;
import org.sgx.classes.item.ModDataComponents;
import org.sgx.classes.network.BindHarvestBookPayload;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Книга урожая — разворот на два листа: слева собранные культуры, справа золотые.
 * Все культуры показываются всегда (с нулями). Данные читаются с самой книги, поэтому экран
 * обновляется вживую, пока открыт.
 */
public final class HarvestBookScreen extends Screen {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "textures/gui/harvest_book.png");
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

    private static final int INK = 0xFF3B2A1A;
    private static final int INK_TITLE = 0xFF5B3A12;
    private static final int INK_FADED = 0xFFA08C6C;
    private static final int INK_GREEN = 0xFF2F5D1F;
    private static final int INK_GOLD = 0xFF9A6A00;
    private static final int INK_RED = 0xFF8B3A2F;
    private static final int INK_HINT = 0xFF8A7556;
    private static final int LEADER = 0x66806030;
    private static final int STRIPE = 0x16603A10;
    private static final int DIM_OVERLAY = 0x99E4D0A0;

    /** Культуры, которые показываются всегда: блок-культура → предмет для иконки. */
    private static final String[][] CROPS = {
            {"minecraft:wheat", "minecraft:wheat"},
            {"minecraft:carrots", "minecraft:carrot"},
            {"minecraft:potatoes", "minecraft:potato"},
            {"minecraft:beetroots", "minecraft:beetroot"},
            {"farmersdelight:cabbages", "farmersdelight:cabbage"},
            {"farmersdelight:onions", "farmersdelight:onion"},
            {"farmersdelight:tomatoes", "farmersdelight:tomato"},
    };

    /** Золотые версии, которые показываются всегда (id предмета). */
    private static final String[] GOLDEN = {
            "arz_classes:golden_wheat",
            "minecraft:golden_carrot",
            "arz_classes:golden_potato",
            "arz_classes:golden_beetroot",
            "arz_classes:golden_cabbage",
            "arz_classes:golden_onion",
            "arz_classes:golden_tomato",
            "arz_classes:golden_rice",
    };

    private record Row(ItemStack icon, Component name, int count) {
    }

    private final Supplier<ItemStack> source;
    private int left, top;
    private int scrollLeft = 0;
    private int scrollRight = 0;

    /** Книга в руке (ПКМ по предмету). */
    public HarvestBookScreen(InteractionHand hand) {
        this(() -> {
            var player = Minecraft.getInstance().player;
            return player == null ? ItemStack.EMPTY : player.getItemInHand(hand);
        });
    }

    /** Книга из произвольного места (слот Trinkets / инвентарь) — вызывается хоткеем. */
    public HarvestBookScreen(Supplier<ItemStack> source) {
        super(Component.translatable("screen.arz_classes.harvest_book"));
        this.source = source;
    }

    /** Хоткей: найти книгу в слоте Trinkets/инвентаре и открыть. Непривязанную привязывает к игроку. */
    public static void openFromKey(Minecraft mc) {
        var player = mc.player;
        if (player == null) {
            return;
        }
        ItemStack book = HarvestBookItem.findForDisplay(player);
        if (book.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.arz_classes.harvest_book_missing"));
            return;
        }
        if (!book.has(ModDataComponents.HARVEST_OWNER_ID)) {
            ClientPlayNetworking.send(BindHarvestBookPayload.INSTANCE);
        }
        mc.gui.setScreen(new HarvestBookScreen(() -> {
            var p = Minecraft.getInstance().player;
            return p == null ? ItemStack.EMPTY : HarvestBookItem.findForDisplay(p);
        }));
    }

    @Override
    protected void init() {
        this.left = (this.width - BOOK_W) / 2;
        boolean buttonFits = this.height >= BOOK_H + 34;
        int contentH = BOOK_H + (buttonFits ? 28 : 0);
        this.top = Math.max(4, (this.height - contentH) / 2);
        if (buttonFits) {
            addRenderableWidget(new BookButton(left + BOOK_W / 2 - 50, top + BOOK_H + 8, 100, 20,
                    Component.translatable("screen.arz_classes.harvest_book.close"), this::onClose));
        }
    }

    @Override
    public void tick() {
        // книгу убрали из инвентаря/руки/слота, пока экран был открыт — закрываем
        if (source.get().isEmpty()) {
            this.onClose();
        }
    }

    // ───────────────────────── данные ─────────────────────────

    private static Identifier idOf(String s) {
        int c = s.indexOf(':');
        return Identifier.fromNamespaceAndPath(s.substring(0, c), s.substring(c + 1));
    }

    private static Item itemOrNull(String id) {
        var opt = BuiltInRegistries.ITEM.get(idOf(id));
        return opt.isPresent() ? opt.get().value() : null;
    }

    private static Block blockOrNull(String id) {
        var opt = BuiltInRegistries.BLOCK.get(idOf(id));
        return opt.isPresent() ? opt.get().value() : null;
    }

    private static int count(Map<String, Integer> stats, String key) {
        return stats.getOrDefault(key, 0);
    }

    private List<Row> cropRows(Map<String, Integer> stats) {
        List<Row> rows = new ArrayList<>();
        Set<String> shown = new LinkedHashSet<>();
        for (String[] crop : CROPS) {
            Item item = itemOrNull(crop[1]);
            if (item == null || blockOrNull(crop[0]) == null) {
                continue; // мод-источник культуры не установлен
            }
            ItemStack icon = new ItemStack(item);
            String key = "crop:" + crop[0];
            shown.add(key);
            rows.add(new Row(icon, icon.getHoverName(), count(stats, key)));
        }
        // чужие культуры, которые всё же попали в книгу (другие моды) — в конец, по убыванию
        stats.entrySet().stream()
                .filter(e -> e.getKey().startsWith("crop:") && !shown.contains(e.getKey()))
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> {
                    String id = e.getKey().substring(5);
                    Block block = blockOrNull(id);
                    ItemStack icon = ItemStack.EMPTY;
                    Component name = Component.literal(id);
                    if (block != null) {
                        name = block.getName();
                        if (block != Blocks.AIR && block.asItem() != net.minecraft.world.item.Items.AIR) {
                            icon = new ItemStack(block.asItem());
                        }
                    }
                    rows.add(new Row(icon, name, e.getValue()));
                });
        return rows;
    }

    private List<Row> goldenRows(Map<String, Integer> stats) {
        List<Row> rows = new ArrayList<>();
        Set<String> shown = new LinkedHashSet<>();
        for (String id : GOLDEN) {
            Item item = itemOrNull(id);
            if (item == null) {
                continue;
            }
            ItemStack icon = new ItemStack(item);
            shown.add("gold:" + id);
            rows.add(new Row(icon, icon.getHoverName(), count(stats, "gold:" + id)));
        }
        stats.entrySet().stream()
                .filter(e -> e.getKey().startsWith("gold:") && !shown.contains(e.getKey()))
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> {
                    String id = e.getKey().substring(5);
                    Item item = itemOrNull(id);
                    ItemStack icon = item == null ? ItemStack.EMPTY : new ItemStack(item);
                    rows.add(new Row(icon, icon.isEmpty() ? Component.literal(id) : icon.getHoverName(), e.getValue()));
                });
        return rows;
    }

    private static int sum(Map<String, Integer> stats, String prefix) {
        int total = 0;
        for (var e : stats.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                total += e.getValue();
            }
        }
        return total;
    }

    // ───────────────────────── ввод ─────────────────────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int delta = -(int) Math.signum(scrollY);
        if (mouseX < left + BOOK_W / 2.0) {
            scrollLeft += delta;
        } else {
            scrollRight += delta;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // тот же хоткей, что открыл книгу, её и закрывает
        if (ModKeyBindings.OPEN_HARVEST_BOOK.matches(event)) {
            this.onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    // ───────────────────────── отрисовка ─────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0, 0, BOOK_W, BOOK_H, BOOK_W, BOOK_H);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        ItemStack book = source.get();
        Map<String, Integer> stats = HarvestBookItem.stats(book);
        String owner = book.get(ModDataComponents.HARVEST_OWNER_NAME);

        List<Row> crops = cropRows(stats);
        List<Row> golden = goldenRows(stats);

        // ── левая страница: книга и собранный урожай ──
        int lc = left + (LEFT_X0 + LEFT_X1) / 2;
        drawCentered(graphics, this.title.copy().withStyle(ChatFormatting.BOLD), lc, top + TITLE_Y, INK_TITLE);
        Component ownerLine = owner != null
                ? Component.translatable("screen.arz_classes.harvest_book.owner", owner)
                : Component.translatable("screen.arz_classes.harvest_book.unbound");
        drawCentered(graphics, ownerLine, lc, top + SUB_Y, owner != null ? INK_GREEN : INK_RED);
        drawCentered(graphics, Component.translatable("screen.arz_classes.harvest_book.total", sum(stats, "crop:")),
                lc, top + TOTAL_Y, INK);
        scrollLeft = drawList(graphics, crops, left + LEFT_X0, left + LEFT_X1, scrollLeft, INK_GREEN);

        // ── правая страница: золотые культуры ──
        int rc = left + (RIGHT_X0 + RIGHT_X1) / 2;
        drawCentered(graphics, Component.translatable("screen.arz_classes.harvest_book.golden")
                .withStyle(ChatFormatting.BOLD), rc, top + TITLE_Y, INK_TITLE);
        drawCentered(graphics, Component.translatable("screen.arz_classes.harvest_book.golden_hint")
                .withStyle(ChatFormatting.ITALIC), rc, top + SUB_Y, INK_HINT);
        drawCentered(graphics, Component.translatable("screen.arz_classes.harvest_book.golden_total",
                sum(stats, "gold:")), rc, top + TOTAL_Y, INK_GOLD);
        scrollRight = drawList(graphics, golden, left + RIGHT_X0, left + RIGHT_X1, scrollRight, INK_GOLD);
    }

    /** Текст без тени (на пергаменте тень выглядит грязно), по центру x. */
    private void drawCentered(GuiGraphicsExtractor graphics, Component text, int cx, int y, int color) {
        graphics.text(this.font, text, cx - this.font.width(text) / 2, y, color, false);
    }

    /** Рисует список строк «иконка — название ····· число»; возвращает поправленный скролл. */
    private int drawList(GuiGraphicsExtractor graphics, List<Row> rows, int x0, int x1, int scroll, int countColor) {
        int maxScroll = Math.max(0, rows.size() - VISIBLE_ROWS);
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        for (int i = 0; i < VISIBLE_ROWS && i + scroll < rows.size(); i++) {
            Row row = rows.get(i + scroll);
            int y = top + LIST_Y + i * ROW_H;
            boolean zero = row.count() == 0;

            if (((i + scroll) & 1) == 0) {
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
            graphics.text(this.font, row.name(), nameX, textY, zero ? INK_FADED : INK, false);

            String countText = String.valueOf(row.count());
            int countX = x1 - 4 - this.font.width(countText);
            graphics.text(this.font, countText, countX, textY, zero ? INK_FADED : countColor, false);

            // пунктирная линия-«выноска» между названием и числом
            for (int dx = nameX + this.font.width(row.name()) + 5; dx < countX - 4; dx += 3) {
                graphics.fill(dx, y + 11, dx + 1, y + 12, LEADER);
            }
        }

        if (maxScroll > 0) {
            // тонкая полоса прокрутки у правого края страницы (нужна только при чужих культурах из других модов)
            int trackTop = top + LIST_Y;
            int trackH = VISIBLE_ROWS * ROW_H;
            int thumbH = Math.max(8, trackH * VISIBLE_ROWS / rows.size());
            int thumbY = trackTop + (trackH - thumbH) * scroll / maxScroll;
            graphics.fill(x1 + 1, trackTop, x1 + 3, trackTop + trackH, 0x33000000);
            graphics.fill(x1 + 1, thumbY, x1 + 3, thumbY + thumbH, 0xAA5B3A12);
        }
        return scroll;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
