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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.client.ModKeyBindings;
import org.sgx.classes.item.HarvestBookItem;
import org.sgx.classes.item.ModDataComponents;
import org.sgx.classes.network.BindHarvestBookPayload;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Книга урожая — разворот на два листа, листается стрелками (кнопки внизу, ← →):
 * <ol>
 *   <li>слева все культуры одним столбцом (плавный скролл), справа золотые версии;</li>
 *   <li>слева убитые мирные животные, справа разведённые.</li>
 * </ol>
 * Все строки показываются всегда, с нулями. Данные читаются с самой книги, поэтому экран
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
    private static final int VIEW_H = VISIBLE_ROWS * ROW_H;

    private static final int PAGES = 2;

    private static final int INK = 0xFF3B2A1A;
    private static final int INK_TITLE = 0xFF5B3A12;
    private static final int INK_FADED = 0xFFA08C6C;
    private static final int INK_GREEN = 0xFF2F5D1F;
    private static final int INK_GOLD = 0xFF9A6A00;
    private static final int INK_RED = 0xFF8B3A2F;
    private static final int INK_BROWN = 0xFF6B3F1A;
    private static final int INK_HINT = 0xFF8A7556;
    private static final int LEADER = 0x66806030;
    private static final int STRIPE = 0x16603A10;
    private static final int DIM_OVERLAY = 0x99E4D0A0;

    private static final String PEPPER_COLORS = "green,yellow,red,orange,white,pink,blue,purple,black";

    /** Строка шаблона: ключ счётчика в книге, иконка, название. Количество подставляется при отрисовке. */
    private record Slot(String key, ItemStack icon, Component name) {
    }

    private record Row(ItemStack icon, Component name, int count) {
    }

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

    private final Supplier<ItemStack> source;
    private int left, top;
    private int page = 0;
    // [страница * 2 + сторона]
    private final Scroll[] scrolls = {new Scroll(), new Scroll(), new Scroll(), new Scroll()};
    private final int[] rowCounts = new int[4];
    private int draggingList = -1;
    private long lastFrameNanos = 0;

    private List<Slot> cropSlots, goldSlots, killSlots, bredSlots;
    private BookButton prevButton, nextButton;

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
        // сервер привяжет непривязанную книгу или подтянет в свою историю из ванильной статистики
        ClientPlayNetworking.send(BindHarvestBookPayload.INSTANCE);
        mc.gui.setScreen(new HarvestBookScreen(() -> {
            var p = Minecraft.getInstance().player;
            return p == null ? ItemStack.EMPTY : HarvestBookItem.findForDisplay(p);
        }));
    }

    @Override
    protected void init() {
        this.left = (this.width - BOOK_W) / 2;
        boolean buttonsFit = this.height >= BOOK_H + 46;
        int contentH = BOOK_H + (buttonsFit ? 38 : 0);
        this.top = Math.max(4, (this.height - contentH) / 2);

        buildSlots();

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

    private void setPage(int p) {
        this.page = Math.max(0, Math.min(PAGES - 1, p));
        this.draggingList = -1;
        updateNavButtons();
    }

    private void updateNavButtons() {
        if (prevButton != null) {
            prevButton.active = page > 0;
        }
        if (nextButton != null) {
            nextButton.active = page < PAGES - 1;
        }
    }

    @Override
    public void tick() {
        // книгу убрали из инвентаря/руки/слота, пока экран был открыт — закрываем
        if (source.get().isEmpty()) {
            this.onClose();
        }
    }

    // ───────────────────────── шаблоны строк ─────────────────────────

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

    private static EntityType<?> entityOrNull(String id) {
        var opt = BuiltInRegistries.ENTITY_TYPE.get(idOf(id));
        return opt.isPresent() ? opt.get().value() : null;
    }

    /** Слот по предмету; нет предмета (мод не установлен) — null. nameKey — свой короткий перевод, если нужен. */
    private static Slot itemSlot(String statKey, String itemId, String nameKey) {
        Item item = itemOrNull(itemId);
        if (item == null) {
            return null;
        }
        ItemStack icon = new ItemStack(item);
        return new Slot(statKey, icon, nameKey != null ? Component.translatable(nameKey) : icon.getHoverName());
    }

    private static void add(List<Slot> list, Slot slot) {
        if (slot != null) {
            list.add(slot);
        }
    }

    private void buildSlots() {
        // ── культуры: ваниль → Farmer's Delight → Rustic Delight (недостающие моды просто пропускаются) ──
        List<Slot> crops = new ArrayList<>();
        add(crops, itemSlot("crop:minecraft:wheat", "minecraft:wheat", null));
        add(crops, itemSlot("crop:minecraft:carrots", "minecraft:carrot", null));
        add(crops, itemSlot("crop:minecraft:potatoes", "minecraft:potato", null));
        add(crops, itemSlot("crop:minecraft:beetroots", "minecraft:beetroot", null));
        add(crops, itemSlot("crop:farmersdelight:cabbages", "farmersdelight:cabbage", null));
        add(crops, itemSlot("crop:farmersdelight:onions", "farmersdelight:onion", null));
        add(crops, itemSlot("crop:farmersdelight:tomatoes", "farmersdelight:tomato", null));
        add(crops, itemSlot("crop:farmersdelight:rice_panicles", "farmersdelight:rice", null));
        add(crops, itemSlot("crop:minecraft:brown_mushroom", "minecraft:brown_mushroom", null));
        add(crops, itemSlot("crop:minecraft:red_mushroom", "minecraft:red_mushroom", null));
        add(crops, itemSlot("crop:rusticdelight:cotton", "rusticdelight:cotton_boll",
                "screen.arz_classes.harvest_book.crop.cotton"));
        add(crops, itemSlot("crop:rusticdelight:coffee", "rusticdelight:coffee_beans", null));
        for (String color : PEPPER_COLORS.split(",")) {
            String id = "rusticdelight:bell_pepper_" + color;
            add(crops, itemSlot("item:" + id, id, "screen.arz_classes.harvest_book.pepper." + color));
        }
        this.cropSlots = crops;

        // ── золотые версии ──
        List<Slot> gold = new ArrayList<>();
        for (String id : new String[]{
                "arz_classes:golden_wheat", "minecraft:golden_carrot", "arz_classes:golden_potato",
                "arz_classes:golden_beetroot", "arz_classes:golden_cabbage", "arz_classes:golden_onion",
                "arz_classes:golden_tomato", "arz_classes:golden_rice"}) {
            add(gold, itemSlot("gold:" + id, id, null));
        }
        this.goldSlots = gold;

        // ── животные: одни и те же виды для «убито» и «разведено» ──
        List<Slot> kills = new ArrayList<>();
        List<Slot> bred = new ArrayList<>();
        for (String path : new String[]{
                "cow", "mooshroom", "pig", "sheep", "chicken", "rabbit", "horse", "donkey", "mule", "llama",
                "goat", "camel", "cat", "wolf", "fox", "ocelot", "panda", "turtle", "bee", "axolotl", "frog",
                "sniffer", "armadillo", "strider"}) {
            String id = "minecraft:" + path;
            add(kills, animalSlot("kill:" + id, id));
            add(bred, animalSlot("bred:" + id, id));
        }
        this.killSlots = kills;
        this.bredSlots = bred;
    }

    /** Животное: иконка — яйцо призыва этого моба (id вида «<ns>:<моб>_spawn_egg»), название — имя типа сущности. */
    private static Slot animalSlot(String statKey, String entityId) {
        EntityType<?> type = entityOrNull(entityId);
        if (type == null) {
            return null;
        }
        Identifier id = idOf(entityId);
        Item egg = itemOrNull(id.getNamespace() + ":" + id.getPath() + "_spawn_egg");
        return new Slot(statKey, egg == null ? ItemStack.EMPTY : new ItemStack(egg), type.getDescription());
    }

    // ───────────────────────── данные ─────────────────────────

    private static Slot cropExtra(String key) {
        String id = key.substring(key.indexOf(':') + 1);
        if (key.startsWith("item:")) {
            Item item = itemOrNull(id);
            ItemStack icon = item == null ? ItemStack.EMPTY : new ItemStack(item);
            return new Slot(key, icon, icon.isEmpty() ? Component.literal(id) : icon.getHoverName());
        }
        Block block = blockOrNull(id);
        ItemStack icon = ItemStack.EMPTY;
        Component name = Component.literal(id);
        if (block != null) {
            name = block.getName();
            if (block != Blocks.AIR && block.asItem() != Items.AIR) {
                icon = new ItemStack(block.asItem());
            }
        }
        return new Slot(key, icon, name);
    }

    private static Slot goldExtra(String key) {
        String id = key.substring(5);
        Item item = itemOrNull(id);
        ItemStack icon = item == null ? ItemStack.EMPTY : new ItemStack(item);
        return new Slot(key, icon, icon.isEmpty() ? Component.literal(id) : icon.getHoverName());
    }

    private static Slot animalExtra(String key) {
        String id = key.substring(key.indexOf(':') + 1);
        Slot slot = animalSlot(key, id);
        return slot != null ? slot : new Slot(key, ItemStack.EMPTY, Component.literal(id));
    }

    /** Шаблоны + всё, что попало в книгу, но не вошло в шаблоны (чужие моды) — в конец, по убыванию. */
    private static List<Row> rows(List<Slot> base, Map<String, Integer> stats, Set<String> prefixes,
                                  Function<String, Slot> extraFactory) {
        List<Row> rows = new ArrayList<>(base.size() + 4);
        Set<String> known = new HashSet<>();
        for (Slot s : base) {
            known.add(s.key());
            rows.add(new Row(s.icon(), s.name(), stats.getOrDefault(s.key(), 0)));
        }
        stats.entrySet().stream()
                .filter(e -> !known.contains(e.getKey()) && prefixes.stream().anyMatch(e.getKey()::startsWith))
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> {
                    Slot s = extraFactory.apply(e.getKey());
                    rows.add(new Row(s.icon(), s.name(), e.getValue()));
                });
        return rows;
    }

    private static int sum(Map<String, Integer> stats, String... prefixes) {
        int total = 0;
        for (var e : stats.entrySet()) {
            for (String p : prefixes) {
                if (e.getKey().startsWith(p)) {
                    total += e.getValue();
                    break;
                }
            }
        }
        return total;
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
        if (ModKeyBindings.OPEN_HARVEST_BOOK.matches(event)) {
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
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, left, top, 0, 0, BOOK_W, BOOK_H, BOOK_W, BOOK_H);
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
        Map<String, Integer> stats = HarvestBookItem.stats(book);
        String owner = book.get(ModDataComponents.HARVEST_OWNER_NAME);

        int lc = left + (LEFT_X0 + LEFT_X1) / 2;
        int rc = left + (RIGHT_X0 + RIGHT_X1) / 2;

        if (page == 0) {
            List<Row> crops = rows(cropSlots, stats, Set.of("crop:", "item:"), HarvestBookScreen::cropExtra);
            List<Row> golden = rows(goldSlots, stats, Set.of("gold:"), HarvestBookScreen::goldExtra);

            drawCentered(graphics, this.title.copy().withStyle(ChatFormatting.BOLD), lc, top + TITLE_Y, INK_TITLE);
            Component ownerLine = owner != null
                    ? Component.translatable("screen.arz_classes.harvest_book.owner", owner)
                    : Component.translatable("screen.arz_classes.harvest_book.unbound");
            drawCentered(graphics, ownerLine, lc, top + SUB_Y, owner != null ? INK_GREEN : INK_RED);
            drawCentered(graphics, Component.translatable("screen.arz_classes.harvest_book.total",
                    sum(stats, "crop:", "item:")), lc, top + TOTAL_Y, INK);
            drawList(graphics, 0, crops, left + LEFT_X0, left + LEFT_X1, INK_GREEN);

            drawCentered(graphics, Component.translatable("screen.arz_classes.harvest_book.golden")
                    .withStyle(ChatFormatting.BOLD), rc, top + TITLE_Y, INK_TITLE);
            drawCentered(graphics, Component.translatable("screen.arz_classes.harvest_book.golden_hint")
                    .withStyle(ChatFormatting.ITALIC), rc, top + SUB_Y, INK_HINT);
            drawCentered(graphics, Component.translatable("screen.arz_classes.harvest_book.golden_total",
                    sum(stats, "gold:")), rc, top + TOTAL_Y, INK_GOLD);
            drawList(graphics, 1, golden, left + RIGHT_X0, left + RIGHT_X1, INK_GOLD);
        } else {
            List<Row> kills = rows(killSlots, stats, Set.of("kill:"), HarvestBookScreen::animalExtra);
            List<Row> bred = rows(bredSlots, stats, Set.of("bred:"), HarvestBookScreen::animalExtra);

            drawCentered(graphics, Component.translatable("screen.arz_classes.harvest_book.kills")
                    .withStyle(ChatFormatting.BOLD), lc, top + TITLE_Y, INK_TITLE);
            drawCentered(graphics, Component.translatable("screen.arz_classes.harvest_book.kills_hint")
                    .withStyle(ChatFormatting.ITALIC), lc, top + SUB_Y, INK_HINT);
            drawCentered(graphics, Component.translatable("screen.arz_classes.harvest_book.kills_total",
                    sum(stats, "kill:")), lc, top + TOTAL_Y, INK_BROWN);
            drawList(graphics, 2, kills, left + LEFT_X0, left + LEFT_X1, INK_BROWN);

            drawCentered(graphics, Component.translatable("screen.arz_classes.harvest_book.bred")
                    .withStyle(ChatFormatting.BOLD), rc, top + TITLE_Y, INK_TITLE);
            drawCentered(graphics, Component.translatable("screen.arz_classes.harvest_book.bred_hint")
                    .withStyle(ChatFormatting.ITALIC), rc, top + SUB_Y, INK_HINT);
            drawCentered(graphics, Component.translatable("screen.arz_classes.harvest_book.bred_total",
                    sum(stats, "bred:")), rc, top + TOTAL_Y, INK_GREEN);
            drawList(graphics, 3, bred, left + RIGHT_X0, left + RIGHT_X1, INK_GREEN);
        }

        // номер страницы под книгой (там же, где кнопки листания)
        Component pageLabel = Component.translatable("screen.arz_classes.harvest_book.page", page + 1, PAGES);
        graphics.text(this.font, pageLabel, left + (BOOK_W - this.font.width(pageLabel)) / 2,
                top + BOOK_H + 4, 0xFFE8D5A8, true);
    }

    /** Текст без тени (на пергаменте тень выглядит грязно), по центру x. */
    private void drawCentered(GuiGraphicsExtractor graphics, Component text, int cx, int y, int color) {
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
    private void drawList(GuiGraphicsExtractor graphics, int list, List<Row> rows, int x0, int x1, int countColor) {
        rowCounts[list] = rows.size();
        Scroll scroll = scrolls[list];
        scroll.clamp(rows.size());

        int viewTop = top + LIST_Y;
        int offset = Math.round(scroll.pos);

        graphics.enableScissor(x0 - 1, viewTop, x1 + 1, viewTop + VIEW_H);
        int first = Math.max(0, offset / ROW_H);
        int last = Math.min(rows.size() - 1, (offset + VIEW_H) / ROW_H);
        for (int i = first; i <= last; i++) {
            Row row = rows.get(i);
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
            graphics.text(this.font, name, nameX, textY, zero ? INK_FADED : INK, false);
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
