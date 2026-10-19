package org.sgx.classes.item;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.block.CarpenterWorkbench;
import org.sgx.classes.block.RarityUpgradeTableBlock;

public final class ModBlocks {


    private static final ResourceKey<Block> RARITY_UPGRADE_TABLE_KEY =
            ResourceKey.create(Registries.BLOCK, Arz_classes.id("rarity_upgrade_table"));
    private static final ResourceKey<Item> RARITY_UPGRADE_TABLE_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("rarity_upgrade_table"));
    private static final ResourceKey<Block> CARPENTER_WORKBENCH_KEY =
            ResourceKey.create(Registries.BLOCK, Arz_classes.id("carpenter_workbench"));
    private static final ResourceKey<Item> CARPENTER_WORKBENCH_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("carpenter_workbench"));

    public static final Block RARITY_UPGRADE_TABLE = new RarityUpgradeTableBlock(
            BlockBehaviour.Properties.of()
                    .setId(RARITY_UPGRADE_TABLE_KEY)
                    .strength(3.5f)
                    .sound(SoundType.STONE)
                    .requiresCorrectToolForDrops());

    public static final Item RARITY_UPGRADE_TABLE_ITEM = new BlockItem(RARITY_UPGRADE_TABLE,
            new Item.Properties().setId(RARITY_UPGRADE_TABLE_ITEM_KEY));

    public static final Block CARPENTER_WORKBENCH = new CarpenterWorkbench(
            BlockBehaviour.Properties.of()
                    .setId(CARPENTER_WORKBENCH_KEY)
                    .strength(3.5f)
                    .sound(SoundType.STONE));

    public static final Item CARPENTER_WORKBENCH_ITEM = new BlockItem(CARPENTER_WORKBENCH,
            new Item.Properties().setId(CARPENTER_WORKBENCH_ITEM_KEY));

    private ModBlocks() {
    }

    public static void init() {
        Registry.register(BuiltInRegistries.BLOCK, RARITY_UPGRADE_TABLE_KEY, RARITY_UPGRADE_TABLE);
        Registry.register(BuiltInRegistries.ITEM, RARITY_UPGRADE_TABLE_ITEM_KEY, RARITY_UPGRADE_TABLE_ITEM);
        Registry.register(BuiltInRegistries.BLOCK, CARPENTER_WORKBENCH_KEY, CARPENTER_WORKBENCH);
        Registry.register(BuiltInRegistries.ITEM, CARPENTER_WORKBENCH_ITEM_KEY, CARPENTER_WORKBENCH_ITEM);
    }
}