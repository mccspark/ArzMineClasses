package org.sgx.classes.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.sgx.classes.item.BookKind;
import org.sgx.classes.item.BookHistory;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.item.ModItems;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.sgx.classes.client.gui.ClassBookScreen.*;

/**
 * Содержимое листов каждой книги: какие панели (списки) на каких страницах. Все строки-шаблоны показываются
 * всегда, с нулями; то, чего нет в шаблонах (предметы чужих модов), дописывается в конец по убыванию.
 */
final class ClassBookPages {

    /** Строка шаблона: ключ счётчика в книге, иконка, название. */
    record Slot(String key, ItemStack icon, Component name, int nameColor) {
        Slot(String key, ItemStack icon, Component name) {
            this(key, icon, name, 0);
        }
    }

    /** Строка списка. nameColor == 0 — цвет чернил по умолчанию. */
    record Row(ItemStack icon, Component name, int count, int nameColor) {
    }

    /** Один лист: заголовок, подпись, итог и список. sub/total/emptyText могут быть null. */
    record Panel(Component title, Component sub, int subColor, Component total, int totalColor,
                 List<Row> rows, int countColor, Component emptyText) {
    }

    record Page(Panel left, Panel right) {
    }

    private static final String PEPPER_COLORS = "green,yellow,red,orange,white,pink,blue,purple,black";

    private final BookKind kind;

    // шаблоны (строятся один раз при открытии экрана — моды к этому моменту уже загружены)
    private List<Slot> cropSlots, goldSlots, killSlots, bredSlots;
    private List<Slot> oreSlots, stoneSlots, logSlots, saplingSlots, appleSlots, raritySlots;
    private List<Slot> tableSlots, anvilSlots;

    ClassBookPages(BookKind kind) {
        this.kind = kind;
        switch (kind) {
            case HARVEST -> buildHarvest();
            case BLACKSMITH -> rarityTemplates();
            case MYSTIC -> mysticTemplates();
            case MINER -> {
                oreSlots = blockSlots("ore:", BookHistory.ORES);
                stoneSlots = blockSlots("stone:", BookHistory.STONES);
            }
            case LUMBERJACK -> {
                logSlots = blockSlots("log:", BookHistory.LOGS);
                saplingSlots = blockSlots("sapling:", BookHistory.SAPLINGS);
                appleSlots = new ArrayList<>();
                add(appleSlots, itemSlot("apple:minecraft:apple", "minecraft:apple", null));
                add(appleSlots, itemSlot("apple:minecraft:golden_apple", "minecraft:golden_apple", null));
            }
        }
    }

    List<Page> build(Map<String, Integer> stats, String owner) {
        return switch (kind) {
            case HARVEST -> harvestPages(stats, owner);
            case BLACKSMITH -> blacksmithPages(stats, owner);
            case MYSTIC -> mysticPages(stats, owner);
            case MINER -> minerPages(stats, owner);
            case LUMBERJACK -> lumberjackPages(stats, owner);
        };
    }

    // ───────────────────────── Кузнец ─────────────────────────

    private void rarityTemplates() {
        List<Slot> rarity = new ArrayList<>();
        for (ItemRarity r : ItemRarity.values()) {
            Item icon = ModItems.RARITY_LOOT_ITEMS.get(r);
            rarity.add(new Slot("rarity:" + r.name(), icon == null ? ItemStack.EMPTY : new ItemStack(icon),
                    Component.translatable(r.translationKey()), r.argb() | 0xFF000000));
        }
        this.raritySlots = rarity;
    }

    private static Component t(String key, Object... args) {
        return Component.translatable("screen.arz_classes.book." + key, args);
    }

    private static Component hint(String key) {
        return t(key).copy().withStyle(net.minecraft.ChatFormatting.ITALIC);
    }

    /** Подпись первой панели — как в книге урожая: «Владелец: ник» (зелёным) или «не привязана» (красным);
     * у остальных панелей — обычная подсказка. */
    private static Panel panel(Component title, Component sub, Component total, int totalColor,
                               List<Row> rows, int countColor, Component empty, String owner, boolean first) {
        if (first) {
            Component line = owner != null
                    ? Component.translatable("screen.arz_classes.harvest_book.owner", owner)
                    : Component.translatable("screen.arz_classes.harvest_book.unbound");
            return new Panel(title, line, owner != null ? INK_GREEN : INK_RED, total, totalColor, rows, countColor, empty);
        }
        return new Panel(title, sub, INK_HINT, total, totalColor, rows, countColor, empty);
    }

    private List<Page> blacksmithPages(Map<String, Integer> stats, String owner) {
        List<Row> crafted = rows(List.of(), stats, Set.of("craft:"), ClassBookPages::itemExtra);
        List<Row> rarity = rows(raritySlots, stats, Set.of("rarity:"), k -> new Slot(k, ItemStack.EMPTY, Component.literal(k)));
        List<Row> sharpen = rows(List.of(
                new Slot("sharpen:weapon", new ItemStack(Items.IRON_SWORD), t("blacksmith.sharpen.weapon")),
                new Slot("sharpen:armor", new ItemStack(Items.IRON_CHESTPLATE), t("blacksmith.sharpen.armor"))),
                stats, Set.of(), k -> null);
        List<Row> repair = rows(List.of(
                new Slot("repair:total", new ItemStack(Items.ANVIL), t("blacksmith.repair.total"))),
                stats, Set.of(), k -> null);

        Page p0 = new Page(
                panel(t("blacksmith.crafted"), hint("blacksmith.crafted_hint"),
                        t("blacksmith.crafted_total", sum(stats, "craft:")), INK_BROWN, crafted, INK_BROWN,
                        t("blacksmith.crafted_empty"), owner, true),
                panel(t("blacksmith.rarity"), hint("blacksmith.rarity_hint"),
                        t("blacksmith.rarity_total", sum(stats, "rarity:")), INK_GOLD, rarity, INK_GOLD,
                        null, owner, false));
        Page p1 = new Page(
                panel(t("blacksmith.sharpen"), hint("blacksmith.sharpen_hint"),
                        t("blacksmith.sharpen_total", sum(stats, "sharpen:")), INK_BROWN, sharpen, INK_BROWN,
                        null, owner, false),
                panel(t("blacksmith.repair"), hint("blacksmith.repair_hint"),
                        t("blacksmith.repair_total", sum(stats, "repair:")), INK_GREEN, repair, INK_GREEN,
                        null, owner, false));
        return List.of(p0, p1);
    }

    // ───────────────────────── Мистик ─────────────────────────

    private void mysticTemplates() {
        List<Slot> table = new ArrayList<>();
        List<Slot> anvil = new ArrayList<>();
        try {
            var level = Minecraft.getInstance().level;
            if (level != null) {
                var registry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                List<Slot> all = new ArrayList<>();
                registry.listElements().forEach(holder -> all.add(new Slot(
                        holder.getRegisteredName(), new ItemStack(Items.ENCHANTED_BOOK),
                        holder.value().description())));
                all.sort(Comparator.comparing(s -> s.name().getString()));
                for (Slot s : all) {
                    table.add(new Slot("table:" + s.key(), s.icon(), s.name()));
                    anvil.add(new Slot("anvil:" + s.key(), s.icon(), s.name()));
                }
            }
        } catch (RuntimeException ignored) {
            // реестр чар недоступен — покажем только то, что есть в книге
        }
        this.tableSlots = table;
        this.anvilSlots = anvil;
    }

    private static Slot enchantExtra(String key) {
        String id = key.substring(key.indexOf(':') + 1);
        return new Slot(key, new ItemStack(Items.ENCHANTED_BOOK), Component.literal(id));
    }

    private List<Page> mysticPages(Map<String, Integer> stats, String owner) {
        List<Row> xp = rows(List.of(
                new Slot("xp:total", new ItemStack(Items.EXPERIENCE_BOTTLE), t("mystic.xp.total")),
                new Slot("xp:maxlevel", new ItemStack(Items.ENCHANTING_TABLE), t("mystic.xp.maxlevel"))),
                stats, Set.of(), k -> null);
        List<Row> table = rows(tableSlots, stats, Set.of("table:"), ClassBookPages::enchantExtra);
        List<Row> anvil = rows(anvilSlots, stats, Set.of("anvil:"), ClassBookPages::enchantExtra);
        int viaTable = sum(stats, "table:");
        int viaAnvil = sum(stats, "anvil:");
        List<Row> summary = List.of(
                new Row(new ItemStack(Items.ENCHANTING_TABLE), t("mystic.summary.table"), viaTable, 0),
                new Row(new ItemStack(Items.ANVIL), t("mystic.summary.anvil"), viaAnvil, 0),
                new Row(new ItemStack(Items.ENCHANTED_BOOK), t("mystic.summary.all"), viaTable + viaAnvil, 0));

        Page p0 = new Page(
                panel(t("mystic.xp"), hint("mystic.xp_hint"), null, INK, xp, INK_GREEN, null, owner, true),
                panel(t("mystic.table"), hint("mystic.table_hint"),
                        t("mystic.table_total", viaTable), INK_GOLD, table, INK_GOLD, null, owner, false));
        Page p1 = new Page(
                panel(t("mystic.anvil"), hint("mystic.anvil_hint"),
                        t("mystic.anvil_total", viaAnvil), INK_BROWN, anvil, INK_BROWN, null, owner, false),
                panel(t("mystic.summary"), hint("mystic.summary_hint"), null, INK, summary, INK_GOLD,
                        null, owner, false));
        return List.of(p0, p1);
    }

    // ───────────────────────── Шахтёр ─────────────────────────

    private List<Page> minerPages(Map<String, Integer> stats, String owner) {
        List<Row> ores = rows(oreSlots, stats, Set.of("ore:"), ClassBookPages::blockExtra);
        List<Row> stone = rows(stoneSlots, stats, Set.of("stone:"), ClassBookPages::blockExtra);
        return List.of(new Page(
                panel(t("miner.ores"), hint("miner.ores_hint"), t("miner.ores_total", sum(stats, "ore:")),
                        INK_BROWN, ores, INK_BROWN, null, owner, true),
                panel(t("miner.stone"), hint("miner.stone_hint"), t("miner.stone_total", sum(stats, "stone:")),
                        INK, stone, INK, null, owner, false)));
    }

    // ───────────────────────── Лесоруб ─────────────────────────

    private List<Page> lumberjackPages(Map<String, Integer> stats, String owner) {
        List<Row> logs = rows(logSlots, stats, Set.of("log:"), ClassBookPages::blockExtra);
        List<Row> saplings = rows(saplingSlots, stats, Set.of("sapling:"), ClassBookPages::blockExtra);
        List<Row> apples = rows(appleSlots, stats, Set.of("apple:"), ClassBookPages::itemExtra);
        Page p0 = new Page(
                panel(t("lumberjack.logs"), hint("lumberjack.logs_hint"),
                        t("lumberjack.logs_total", sum(stats, "log:")), INK_BROWN, logs, INK_BROWN, null, owner, true),
                panel(t("lumberjack.saplings"), hint("lumberjack.saplings_hint"),
                        t("lumberjack.saplings_total", sum(stats, "sapling:")), INK_GREEN, saplings, INK_GREEN,
                        null, owner, false));
        Page p1 = new Page(
                panel(t("lumberjack.apples"), hint("lumberjack.apples_hint"),
                        t("lumberjack.apples_total", sum(stats, "apple:")), INK_RED, apples, INK_RED,
                        null, owner, false),
                null);
        return List.of(p0, p1);
    }

    // ───────────────────────── Урожай ─────────────────────────

    private void buildHarvest() {
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

        List<Slot> gold = new ArrayList<>();
        for (String id : new String[]{
                "arz_classes:golden_wheat", "minecraft:golden_carrot", "arz_classes:golden_potato",
                "arz_classes:golden_beetroot", "arz_classes:golden_cabbage", "arz_classes:golden_onion",
                "arz_classes:golden_tomato", "arz_classes:golden_rice"}) {
            add(gold, itemSlot("gold:" + id, id, null));
        }
        this.goldSlots = gold;

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

    private List<Page> harvestPages(Map<String, Integer> stats, String owner) {
        List<Row> crops = rows(cropSlots, stats, Set.of("crop:", "item:"), ClassBookPages::cropExtra);
        List<Row> golden = rows(goldSlots, stats, Set.of("gold:"), ClassBookPages::goldExtra);
        List<Row> kills = rows(killSlots, stats, Set.of("kill:"), ClassBookPages::animalExtra);
        List<Row> bred = rows(bredSlots, stats, Set.of("bred:"), ClassBookPages::animalExtra);

        Component ownerLine = owner != null
                ? Component.translatable("screen.arz_classes.harvest_book.owner", owner)
                : Component.translatable("screen.arz_classes.harvest_book.unbound");
        Component bookTitle = Component.translatable("screen.arz_classes.harvest_book");

        Page p0 = new Page(
                new Panel(bookTitle, ownerLine, owner != null ? INK_GREEN : INK_RED,
                        Component.translatable("screen.arz_classes.harvest_book.total", sum(stats, "crop:", "item:")),
                        INK, crops, INK_GREEN, null),
                new Panel(Component.translatable("screen.arz_classes.harvest_book.golden"),
                        Component.translatable("screen.arz_classes.harvest_book.golden_hint")
                                .withStyle(net.minecraft.ChatFormatting.ITALIC), INK_HINT,
                        Component.translatable("screen.arz_classes.harvest_book.golden_total", sum(stats, "gold:")),
                        INK_GOLD, golden, INK_GOLD, null));
        Page p1 = new Page(
                new Panel(Component.translatable("screen.arz_classes.harvest_book.kills"),
                        Component.translatable("screen.arz_classes.harvest_book.kills_hint")
                                .withStyle(net.minecraft.ChatFormatting.ITALIC), INK_HINT,
                        Component.translatable("screen.arz_classes.harvest_book.kills_total", sum(stats, "kill:")),
                        INK_BROWN, kills, INK_BROWN, null),
                new Panel(Component.translatable("screen.arz_classes.harvest_book.bred"),
                        Component.translatable("screen.arz_classes.harvest_book.bred_hint")
                                .withStyle(net.minecraft.ChatFormatting.ITALIC), INK_HINT,
                        Component.translatable("screen.arz_classes.harvest_book.bred_total", sum(stats, "bred:")),
                        INK_GREEN, bred, INK_GREEN, null));
        return List.of(p0, p1);
    }

    // ───────────────────────── общие помощники ─────────────────────────

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

    /** Слот по предмету; нет предмета (мод не установлен) — null. nameKey — свой короткий перевод. */
    private static Slot itemSlot(String statKey, String itemId, String nameKey) {
        Item item = itemOrNull(itemId);
        if (item == null) {
            return null;
        }
        ItemStack icon = new ItemStack(item);
        return new Slot(statKey, icon, nameKey != null ? Component.translatable(nameKey) : icon.getHoverName());
    }

    /** Слоты по блокам: ключ = prefix + id блока; иконка и название — от блока. Отсутствующие блоки пропускаются. */
    private static List<Slot> blockSlots(String prefix, String[] blockIds) {
        List<Slot> list = new ArrayList<>();
        for (String id : blockIds) {
            Slot s = blockSlot(prefix + id, id);
            if (s != null) {
                list.add(s);
            }
        }
        return list;
    }

    private static Slot blockSlot(String statKey, String blockId) {
        Block block = blockOrNull(blockId);
        if (block == null || block == Blocks.AIR) {
            return null;
        }
        ItemStack icon = block.asItem() == Items.AIR ? ItemStack.EMPTY : new ItemStack(block.asItem());
        return new Slot(statKey, icon, block.getName());
    }

    private static void add(List<Slot> list, Slot slot) {
        if (slot != null) {
            list.add(slot);
        }
    }

    private static Slot animalSlot(String statKey, String entityId) {
        EntityType<?> type = entityOrNull(entityId);
        if (type == null) {
            return null;
        }
        Identifier id = idOf(entityId);
        Item egg = itemOrNull(id.getNamespace() + ":" + id.getPath() + "_spawn_egg");
        return new Slot(statKey, egg == null ? ItemStack.EMPTY : new ItemStack(egg), type.getDescription());
    }

    private static Slot cropExtra(String key) {
        String id = key.substring(key.indexOf(':') + 1);
        if (key.startsWith("item:")) {
            return itemExtra(key);
        }
        Slot s = blockSlot(key, id);
        return s != null ? s : new Slot(key, ItemStack.EMPTY, Component.literal(id));
    }

    private static Slot blockExtra(String key) {
        String id = key.substring(key.indexOf(':') + 1);
        Slot s = blockSlot(key, id);
        return s != null ? s : new Slot(key, ItemStack.EMPTY, Component.literal(id));
    }

    private static Slot itemExtra(String key) {
        String id = key.substring(key.indexOf(':') + 1);
        Item item = itemOrNull(id);
        ItemStack icon = item == null ? ItemStack.EMPTY : new ItemStack(item);
        return new Slot(key, icon, icon.isEmpty() ? Component.literal(id) : icon.getHoverName());
    }

    private static Slot goldExtra(String key) {
        return itemExtra(key);
    }

    private static Slot animalExtra(String key) {
        String id = key.substring(key.indexOf(':') + 1);
        Slot slot = animalSlot(key, id);
        return slot != null ? slot : new Slot(key, ItemStack.EMPTY, Component.literal(id));
    }

    /** Шаблоны + всё, что попало в книгу по этим префиксам, но не вошло в шаблоны — в конец, по убыванию. */
    private static List<Row> rows(List<Slot> base, Map<String, Integer> stats, Set<String> prefixes,
                                  Function<String, Slot> extraFactory) {
        List<Row> rows = new ArrayList<>(base.size() + 4);
        Set<String> known = new HashSet<>();
        for (Slot s : base) {
            known.add(s.key());
            rows.add(new Row(s.icon(), s.name(), stats.getOrDefault(s.key(), 0), s.nameColor()));
        }
        stats.entrySet().stream()
                .filter(e -> !known.contains(e.getKey()) && prefixes.stream().anyMatch(e.getKey()::startsWith))
                .sorted((a, b) -> b.getValue() - a.getValue())
                .forEach(e -> {
                    Slot s = extraFactory.apply(e.getKey());
                    if (s != null) {
                        rows.add(new Row(s.icon(), s.name(), e.getValue(), s.nameColor()));
                    }
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
}
