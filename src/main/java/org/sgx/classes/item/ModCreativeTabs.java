package org.sgx.classes.item;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.block.chest.ModChests;

public final class ModCreativeTabs {

    private static final ResourceKey<CreativeModeTab> TAB_KEY =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, Arz_classes.id("arz_classes"));

    public static final CreativeModeTab ARZ_CLASSES_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB, TAB_KEY,
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.arz_classes"))
                    .icon(() -> new ItemStack(ModItems.BLACKSMITH_HAMMER))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.BLACKSMITH_HAMMER);
                        output.accept(ModItems.COPPER_HAMMER);
                        output.accept(ModItems.DIAMOND_SHARD);
                        output.accept(ModItems.FIREWOOD);
                        output.accept(ModItems.KNOWLEDGE_SCROLL);
                        output.accept(ModItems.COIL);
                        output.accept(ModItems.MINER_HELMET);
                        output.accept(ModItems.ADMIN_DAMAGE_STICK);
                        output.accept(ModItems.ADMIN_KNOCKBACK_STICK);
                        output.accept(ModBlocks.RARITY_UPGRADE_TABLE_ITEM);
                        output.accept(ModBlocks.CARPENTER_WORKBENCH_ITEM);
                        for (var rack : org.sgx.classes.block.ModWeaponRacks.allItems()) {
                            output.accept(rack);
                        }
                        output.accept(ModItems.RING_IRON);
                        output.accept(ModItems.RING_GOLD);
                        output.accept(ModItems.RING_NETHERITE);
                        output.accept(ModItems.BONE_POUCH);
                        output.accept(ModItems.FOOD_POUCH);
                        output.accept(ModItems.HARVEST_BOOK);
                        output.accept(ModItems.GOLDEN_BEETROOT);
                        output.accept(ModItems.GOLDEN_CABBAGE);
                        output.accept(ModItems.GOLDEN_ONION);
                        output.accept(ModItems.GOLDEN_POTATO);
                        output.accept(ModItems.GOLDEN_RICE);
                        output.accept(ModItems.GOLDEN_TOMATO);
                        output.accept(ModItems.GOLDEN_WHEAT);
                        for (var lootItem : ModItems.RARITY_LOOT_ITEMS.values()) {
                            output.accept(lootItem);
                        }
                        for (var shard : ModItems.RARITY_SHARDS.values()) {
                            output.accept(shard);
                        }
                        for (var fragment : ModItems.RARITY_FRAGMENTS.values()) {
                            output.accept(fragment);
                        }
                        // сундуки из разных пород дерева (обычные и ловушки)
                        for (var chest : ModChests.allItems()) {
                            output.accept(chest);
                        }
                    })
                    .build());

    private ModCreativeTabs() {
    }

    public static void init() {
        // загрузчик — форсирует статическую инициализацию TAB_KEY/ARZ_CLASSES_TAB выше
    }
}