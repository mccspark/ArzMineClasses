package org.sgx.classes.block.chest;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.sgx.classes.Arz_classes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Сундуки из разных пород дерева: обычные и сундуки-ловушки. Порода = ключ в WOODS (так же названы папки с текстурами). */
public final class ModChests {

    public static final List<String> WOODS = List.of(
            "acacia", "bamboo", "birch", "cherry", "warped", "dark_oak",
            "jungle", "mangrove", "oak", "spruce", "crimson", "pale_oak");

    private static final Map<String, Item> PLANKS = Map.ofEntries(
            Map.entry("acacia", Items.ACACIA_PLANKS),
            Map.entry("bamboo", Items.BAMBOO_PLANKS),
            Map.entry("birch", Items.BIRCH_PLANKS),
            Map.entry("cherry", Items.CHERRY_PLANKS),
            Map.entry("warped", Items.WARPED_PLANKS),
            Map.entry("dark_oak", Items.DARK_OAK_PLANKS),
            Map.entry("jungle", Items.JUNGLE_PLANKS),
            Map.entry("mangrove", Items.MANGROVE_PLANKS),
            Map.entry("oak", Items.OAK_PLANKS),
            Map.entry("spruce", Items.SPRUCE_PLANKS),
            Map.entry("crimson", Items.CRIMSON_PLANKS),
            Map.entry("pale_oak", Items.PALE_OAK_PLANKS));

    private static final Map<String, WoodChestBlock> CHESTS = new LinkedHashMap<>();
    private static final Map<String, WoodChestBlock> TRAPPED_CHESTS = new LinkedHashMap<>();
    private static final Map<String, Item> CHEST_ITEMS = new LinkedHashMap<>();
    private static final Map<String, Item> TRAPPED_CHEST_ITEMS = new LinkedHashMap<>();

    public static final BlockEntityType<WoodChestBlockEntity> CHEST_ENTITY;
    public static final BlockEntityType<TrappedWoodChestBlockEntity> TRAPPED_CHEST_ENTITY;

    static {
        for (String wood : WOODS) {
            CHESTS.put(wood, createBlock(wood + "_chest", wood, false));
            TRAPPED_CHESTS.put(wood, createBlock("trapped_" + wood + "_chest", wood, true));
        }
        for (String wood : WOODS) {
            CHEST_ITEMS.put(wood, createItem(wood + "_chest", CHESTS.get(wood)));
            TRAPPED_CHEST_ITEMS.put(wood, createItem("trapped_" + wood + "_chest", TRAPPED_CHESTS.get(wood)));
        }
        CHEST_ENTITY = new BlockEntityType<>(WoodChestBlockEntity::new, Set.<Block>copyOf(CHESTS.values()));
        TRAPPED_CHEST_ENTITY = new BlockEntityType<>(TrappedWoodChestBlockEntity::new, Set.<Block>copyOf(TRAPPED_CHESTS.values()));
    }

    private ModChests() {
    }

    private static WoodChestBlock createBlock(String path, String wood, boolean trapped) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Arz_classes.id(path));
        return new WoodChestBlock(wood, trapped, BlockBehaviour.Properties.of()
                .setId(key)
                .strength(2.5f)
                .sound(SoundType.WOOD));
    }

    private static Item createItem(String path, Block block) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Arz_classes.id(path));
        return new BlockItem(block, new Item.Properties().setId(key));
    }

    public static Item chestItem(String wood) {
        return CHEST_ITEMS.get(wood);
    }

    public static Item trappedChestItem(String wood) {
        return TRAPPED_CHEST_ITEMS.get(wood);
    }

    public static Item planksFor(String wood) {
        return PLANKS.get(wood);
    }

    /** Все предметы-сундуки (для креативной вкладки). */
    public static List<Item> allItems() {
        List<Item> list = new ArrayList<>();
        for (String wood : WOODS) {
            list.add(CHEST_ITEMS.get(wood));
            list.add(TRAPPED_CHEST_ITEMS.get(wood));
        }
        return list;
    }

    public static void init() {
        for (String wood : WOODS) {
            register(wood + "_chest", CHESTS.get(wood), CHEST_ITEMS.get(wood));
            register("trapped_" + wood + "_chest", TRAPPED_CHESTS.get(wood), TRAPPED_CHEST_ITEMS.get(wood));
        }
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, Arz_classes.id("wood_chest")), CHEST_ENTITY);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, Arz_classes.id("trapped_wood_chest")), TRAPPED_CHEST_ENTITY);
    }

    private static void register(String path, Block block, Item item) {
        Registry.register(BuiltInRegistries.BLOCK, ResourceKey.create(Registries.BLOCK, Arz_classes.id(path)), block);
        Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, Arz_classes.id(path)), item);
    }
}
