package org.sgx.classes.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.block.chest.ModChests;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Оружейные стойки из разных пород дерева (те же породы и названия папок, что у сундуков). */
public final class ModWeaponRacks {

    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

    static {
        for (String wood : ModChests.WOODS) {
            String path = wood + "_weapon_rack";
            ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, Arz_classes.id(path));
            ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Arz_classes.id(path));
            Block block = new WeaponRackBlock(BlockBehaviour.Properties.of()
                    .setId(blockKey)
                    .strength(2.0f)
                    .sound(SoundType.WOOD)
                    .noOcclusion());
            BLOCKS.put(wood, block);
            ITEMS.put(wood, new BlockItem(block, new Item.Properties().setId(itemKey)));
        }
    }

    private ModWeaponRacks() {
    }

    public static Item item(String wood) {
        return ITEMS.get(wood);
    }

    public static List<Block> blocks() {
        return new ArrayList<>(BLOCKS.values());
    }

    public static List<Item> allItems() {
        return new ArrayList<>(ITEMS.values());
    }

    public static void init() {
        for (String wood : ModChests.WOODS) {
            String path = wood + "_weapon_rack";
            Registry.register(BuiltInRegistries.BLOCK, ResourceKey.create(Registries.BLOCK, Arz_classes.id(path)), BLOCKS.get(wood));
            Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, Arz_classes.id(path)), ITEMS.get(wood));
        }
    }
}
