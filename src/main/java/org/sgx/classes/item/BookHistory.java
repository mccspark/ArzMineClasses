package org.sgx.classes.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.sgx.classes.profession.RarityEligibility;

import java.util.HashMap;
import java.util.Map;

/**
 * Подтягивает в книгу то, что игрок уже сделал до неё, из ванильной статистики. Один раз на книгу
 * (метка {@link ClassBookItem#META_IMPORTED}); берётся максимум из «уже в книге» и «в статистике»,
 * чтобы ничего не задвоилось. То, чего в ванили нет (редкости, заточки, починки, опыт, чары, яблоки),
 * не подтягивается — это считается только с момента появления книги.
 */
public final class BookHistory {

    private BookHistory() {
    }

    /** Блоки «Шахтёра»: руды (ключ в книге — "ore:<id блока>"). */
    public static final String[] ORES = {
            "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
            "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
            "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
            "minecraft:gold_ore", "minecraft:deepslate_gold_ore", "minecraft:nether_gold_ore",
            "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
            "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
            "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
            "minecraft:nether_quartz_ore", "minecraft:ancient_debris"};

    /** Каменные блоки (ключ — "stone:<id блока>"): считаются, когда ломаются киркой. */
    public static final String[] STONES = {
            "minecraft:stone", "minecraft:cobblestone", "minecraft:deepslate", "minecraft:cobbled_deepslate",
            "minecraft:andesite", "minecraft:diorite", "minecraft:granite", "minecraft:tuff",
            "minecraft:calcite", "minecraft:dripstone_block", "minecraft:basalt", "minecraft:blackstone",
            "minecraft:netherrack", "minecraft:end_stone"};

    /** Брёвна (ключ — "log:<id блока>"). */
    public static final String[] LOGS = {
            "minecraft:oak_log", "minecraft:spruce_log", "minecraft:birch_log", "minecraft:jungle_log",
            "minecraft:acacia_log", "minecraft:dark_oak_log", "minecraft:mangrove_log", "minecraft:cherry_log",
            "minecraft:pale_oak_log", "minecraft:crimson_stem", "minecraft:warped_stem"};

    /** Саженцы (ключ — "sapling:<id блока>"). */
    public static final String[] SAPLINGS = {
            "minecraft:oak_sapling", "minecraft:spruce_sapling", "minecraft:birch_sapling",
            "minecraft:jungle_sapling", "minecraft:acacia_sapling", "minecraft:dark_oak_sapling",
            "minecraft:cherry_sapling", "minecraft:pale_oak_sapling", "minecraft:mangrove_propagule"};

    /** Культуры Фермера, историю которых берём из «Добыто». */
    private static final String[] MINED_CROPS = {
            "minecraft:wheat", "minecraft:carrots", "minecraft:potatoes", "minecraft:beetroots",
            "minecraft:brown_mushroom", "minecraft:red_mushroom",
            "farmersdelight:cabbages", "farmersdelight:onions", "farmersdelight:rice_panicles",
            "rusticdelight:cotton", "rusticdelight:coffee"};

    /** Золотые культуры мода: историю берём из «Подобрано». Золотую морковь не трогаем: она крафтится. */
    private static final String[] GOLDEN_ITEMS = {
            "arz_classes:golden_wheat", "arz_classes:golden_potato", "arz_classes:golden_beetroot",
            "arz_classes:golden_cabbage", "arz_classes:golden_onion", "arz_classes:golden_tomato",
            "arz_classes:golden_rice"};

    private static Identifier idOf(String s) {
        int c = s.indexOf(':');
        return Identifier.fromNamespaceAndPath(s.substring(0, c), s.substring(c + 1));
    }

    private static void putMax(Map<String, Integer> map, String key, int value) {
        if (value > 0) {
            map.merge(key, value, Math::max);
        }
    }

    private static int mined(ServerPlayer player, String blockId) {
        var ref = BuiltInRegistries.BLOCK.get(idOf(blockId));
        if (ref.isEmpty()) {
            return 0;
        }
        Block block = ref.get().value();
        return player.getStats().getValue(Stats.BLOCK_MINED.get(block));
    }

    private static int picked(ServerPlayer player, String itemId) {
        var ref = BuiltInRegistries.ITEM.get(idOf(itemId));
        return ref.isEmpty() ? 0 : player.getStats().getValue(Stats.ITEM_PICKED_UP.get(ref.get().value()));
    }

    /** Подтягивает историю в книгу, если ещё не подтянута. Только для владельца книги. */
    public static void importOnce(ServerPlayer player, ItemStack book) {
        BookKind kind = BookKind.of(book);
        if (kind == null || !player.getUUID().toString().equals(book.get(ModDataComponents.HARVEST_OWNER_ID))) {
            return;
        }
        Map<String, Integer> current = ClassBookItem.stats(book);
        if (current.containsKey(ClassBookItem.META_IMPORTED)) {
            return;
        }
        Map<String, Integer> map = new HashMap<>(current);
        switch (kind) {
            case HARVEST -> harvest(player, map);
            case BLACKSMITH -> blacksmith(player, map);
            case MINER -> miner(player, map);
            case LUMBERJACK -> lumberjack(player, map);
            case MYSTIC -> { /* в ванили нет ни опыта «за всё время», ни счётчика чар по видам */ }
        }
        map.put(ClassBookItem.META_IMPORTED, 1);
        book.set(ModDataComponents.HARVEST_STATS, map);
    }

    private static void harvest(ServerPlayer player, Map<String, Integer> map) {
        var counter = player.getStats();
        for (String id : MINED_CROPS) {
            putMax(map, "crop:" + id, mined(player, id));
        }
        // помидоры собираются ПКМ и блок не ломают — считаем по подобранным помидорам
        putMax(map, "crop:farmersdelight:tomatoes", picked(player, "farmersdelight:tomato"));
        for (String id : HarvestBookItem.COUNTED_DROPS) {
            putMax(map, "item:" + id, picked(player, id));
        }
        for (String id : GOLDEN_ITEMS) {
            putMax(map, "gold:" + id, picked(player, id));
        }
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type.getCategory() == MobCategory.CREATURE || type.getCategory() == MobCategory.AXOLOTLS) {
                putMax(map, "kill:" + BuiltInRegistries.ENTITY_TYPE.getKey(type),
                        counter.getValue(Stats.ENTITY_KILLED.get(type)));
            }
        }
    }

    /** Кузнец: скрафченное снаряжение (то, что получает редкость) — из «Создано». */
    private static void blacksmith(ServerPlayer player, Map<String, Integer> map) {
        for (Item item : BuiltInRegistries.ITEM) {
            int crafted = player.getStats().getValue(Stats.ITEM_CRAFTED.get(item));
            if (crafted > 0 && RarityEligibility.isEligible(new ItemStack(item))) {
                putMax(map, "craft:" + BuiltInRegistries.ITEM.getKey(item), crafted);
            }
        }
    }

    private static void miner(ServerPlayer player, Map<String, Integer> map) {
        for (String id : ORES) {
            putMax(map, "ore:" + id, mined(player, id));
        }
        for (String id : STONES) {
            putMax(map, "stone:" + id, mined(player, id));
        }
    }

    private static void lumberjack(ServerPlayer player, Map<String, Integer> map) {
        for (String id : LOGS) {
            putMax(map, "log:" + id, mined(player, id));
        }
        // посаженные саженцы — из «Использовано» (предмет саженца)
        for (String id : SAPLINGS) {
            var ref = BuiltInRegistries.ITEM.get(idOf(id));
            if (ref.isPresent()) {
                putMax(map, "sapling:" + id, player.getStats().getValue(Stats.ITEM_USED.get(ref.get().value())));
            }
        }
    }
}
