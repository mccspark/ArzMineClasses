package org.sgx.classes.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.PushReaction;
import org.sgx.classes.Arz_classes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Стеклянные двери из разных пород дерева. Ведут себя как обычные деревянные двери,
 * отличаются только текстурой. Породы — только те, для которых есть текстуры (ключи те же, что в ModChests.WOODS).
 */
public final class ModGlassDoors {

    public static final List<String> WOODS = List.of(
            "acacia", "birch", "crimson", "dark_oak", "jungle", "oak", "spruce", "warped");

    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    static {
        for (String wood : WOODS) {
            String path = wood + "_glass_door";
            ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Arz_classes.id(path));
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Arz_classes.id(path));
            boolean nether = wood.equals("crimson") || wood.equals("warped");
            Block block = new DoorBlock(setType(wood), BlockBehaviour.Properties.of()
                    .setId(blockKey)
                    .strength(3.0f)
                    .sound(nether ? SoundType.NETHER_WOOD : SoundType.WOOD)
                    .noOcclusion()
                    .pushReaction(PushReaction.DESTROY));
            BLOCKS.put(wood, block);
            ITEMS.put(wood, new DoubleHighBlockItem(block, new Item.Properties().setId(itemKey)));
        }
    }

    private ModGlassDoors() {
    }

    private static BlockSetType setType(String wood) {
        return switch (wood) {
            case "acacia" -> BlockSetType.ACACIA;
            case "birch" -> BlockSetType.BIRCH;
            case "crimson" -> BlockSetType.CRIMSON;
            case "dark_oak" -> BlockSetType.DARK_OAK;
            case "jungle" -> BlockSetType.JUNGLE;
            case "spruce" -> BlockSetType.SPRUCE;
            case "warped" -> BlockSetType.WARPED;
            default -> BlockSetType.OAK;
        };
    }

    public static Item item(String wood) {
        return ITEMS.get(wood);
    }

    public static List<Item> allItems() {
        return new ArrayList<>(ITEMS.values());
    }

    public static void init() {
        for (String wood : WOODS) {
            String path = wood + "_glass_door";
            Registry.register(BuiltInRegistries.BLOCK, ResourceKey.create(Registries.BLOCK, Arz_classes.id(path)), BLOCKS.get(wood));
            Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, Arz_classes.id(path)), ITEMS.get(wood));
        }
    }
}
