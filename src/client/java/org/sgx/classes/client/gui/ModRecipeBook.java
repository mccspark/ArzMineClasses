package org.sgx.classes.client.gui;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.item.ModBlocks;
import org.sgx.classes.item.ModItems;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;

import java.util.List;
import java.util.function.Predicate;

/** Список рецептов мода для RecipeBookScreen. Сетки — 9 ячеек (0..8, слева направо
 * сверху вниз), СООТВЕТСТВУЮТ реальным JSON-рецептам в data/arz_classes/recipe/ —
 * при добавлении нового JSON-рецепта добавь сюда соответствующую запись вручную
 * (это не читается из RecipeManager, чтобы не рисковать незнакомым API).
 *
 * profession == null  -> рецепт виден игроку ЛЮБОГО класса.
 * profession != null  -> рецепт виден ТОЛЬКО игроку этого класса — остальные его
 * вообще не видят в списке (это не "заблокирован", а полное сокрытие,
 * см. фильтр в RecipeBookScreen.visibleEntries()). */
public final class ModRecipeBook {

    public record Entry(String id, Component name, ItemStack[] grid, ItemStack result,
                        ProfessionType profession, Predicate<ProfessionData> unlocked, Component lockedHint) {
    }

    public static final List<Entry> ENTRIES = List.of(
            new Entry(
                    "copper_hammer",
                    Component.translatable("recipe.arz_classes.copper_hammer"),
                    new ItemStack[]{
                            new ItemStack(Items.COPPER_BLOCK.weathering().unaffected()), new ItemStack(Items.COPPER_INGOT), new ItemStack(Items.COPPER_BLOCK.weathering().unaffected()),
                            ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY,
                            ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY
                    },
                    new ItemStack(ModItems.COPPER_HAMMER),
                    ProfessionType.BLACKSMITH,
                    data -> true,
                    Component.empty()
            ),
            new Entry(
                    "blacksmith_hammer",
                    Component.translatable("recipe.arz_classes.blacksmith_hammer"),
                    new ItemStack[]{
                            new ItemStack(Items.IRON_INGOT), new ItemStack(Items.IRON_BLOCK), new ItemStack(Items.IRON_INGOT),
                            ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY,
                            ItemStack.EMPTY, new ItemStack(Items.STICK), ItemStack.EMPTY
                    },
                    new ItemStack(ModItems.BLACKSMITH_HAMMER),
                    ProfessionType.BLACKSMITH,
                    data -> true,
                    Component.empty()
            ),
            new Entry(
                    "bone_pouch",
                    Component.translatable("recipe.arz_classes.bone_pouch"),
                    new ItemStack[]{
                            new ItemStack(Items.LEATHER), new ItemStack(Items.LEATHER), new ItemStack(Items.LEATHER),
                            new ItemStack(Items.LEATHER), ItemStack.EMPTY, new ItemStack(Items.LEATHER),
                            new ItemStack(Items.LEATHER), new ItemStack(Items.LEATHER), new ItemStack(Items.LEATHER)
                    },
                    new ItemStack(ModItems.BONE_POUCH),
                    ProfessionType.FARMER,
                    data -> data.hasAbility("farmer_bone_pouch"),
                    Component.translatable("recipe.arz_classes.bone_pouch.locked")
            ),
            new Entry(
                    "rich_soil",
                    Component.translatable("recipe.arz_classes.rich_soil"),
                    new ItemStack[]{
                            new ItemStack(Items.DIRT), new ItemStack(Items.DIRT), new ItemStack(Items.DIRT),
                            new ItemStack(Items.DIRT), foreignItem("farmersdelight", "organic_compost", 1), new ItemStack(Items.DIRT),
                            new ItemStack(Items.DIRT), new ItemStack(Items.DIRT), new ItemStack(Items.DIRT)
                    },
                    foreignItem("farmersdelight", "rich_soil", 4),
                    ProfessionType.FARMER,
                    data -> data.hasAbility("farmer_delight_plots"),
                    Component.translatable("recipe.arz_classes.locked_farmer_delight_plots")
            ),
            // ring_iron.json: IFI / F F / IFI  (I = железо, F = фрагмент RARE)
            new Entry(
                    "ring_iron",
                    Component.translatable("recipe.arz_classes.ring_iron"),
                    new ItemStack[]{
                            new ItemStack(Items.IRON_INGOT), new ItemStack(ModItems.RARITY_FRAGMENTS.get(ItemRarity.RARE)), new ItemStack(Items.IRON_INGOT),
                            new ItemStack(ModItems.RARITY_FRAGMENTS.get(ItemRarity.RARE)), ItemStack.EMPTY, new ItemStack(ModItems.RARITY_FRAGMENTS.get(ItemRarity.RARE)),
                            new ItemStack(Items.IRON_INGOT), new ItemStack(ModItems.RARITY_FRAGMENTS.get(ItemRarity.RARE)), new ItemStack(Items.IRON_INGOT)
                    },
                    new ItemStack(ModItems.RING_IRON),
                    ProfessionType.MYSTIC,
                    data -> data.hasAbility("mystic_iron_ring_craft"),
                    Component.translatable("recipe.arz_classes.ring_iron.locked")
            ),
            // ring_gold.json: GEG / ERE / GEG  (G = золото, E = фрагмент EPIC, R = железное кольцо)
            new Entry(
                    "ring_gold",
                    Component.translatable("recipe.arz_classes.ring_gold"),
                    new ItemStack[]{
                            new ItemStack(Items.GOLD_INGOT), new ItemStack(ModItems.RARITY_FRAGMENTS.get(ItemRarity.EPIC)), new ItemStack(Items.GOLD_INGOT),
                            new ItemStack(ModItems.RARITY_FRAGMENTS.get(ItemRarity.EPIC)), new ItemStack(ModItems.RING_IRON), new ItemStack(ModItems.RARITY_FRAGMENTS.get(ItemRarity.EPIC)),
                            new ItemStack(Items.GOLD_INGOT), new ItemStack(ModItems.RARITY_FRAGMENTS.get(ItemRarity.EPIC)), new ItemStack(Items.GOLD_INGOT)
                    },
                    new ItemStack(ModItems.RING_GOLD),
                    ProfessionType.MYSTIC,
                    data -> data.hasAbility("mystic_gold_ring_craft"),
                    Component.translatable("recipe.arz_classes.ring_gold.locked")
            ),
            // ring_netherite.json: SLS / LRL / SLS  (S = незеритовый лом, L = фрагмент LEGENDARY, R = золотое кольцо)
            new Entry(
                    "ring_netherite",
                    Component.translatable("recipe.arz_classes.ring_netherite"),
                    new ItemStack[]{
                            new ItemStack(Items.NETHERITE_SCRAP), new ItemStack(ModItems.RARITY_FRAGMENTS.get(ItemRarity.LEGENDARY)), new ItemStack(Items.NETHERITE_SCRAP),
                            new ItemStack(ModItems.RARITY_FRAGMENTS.get(ItemRarity.LEGENDARY)), new ItemStack(ModItems.RING_GOLD), new ItemStack(ModItems.RARITY_FRAGMENTS.get(ItemRarity.LEGENDARY)),
                            new ItemStack(Items.NETHERITE_SCRAP), new ItemStack(ModItems.RARITY_FRAGMENTS.get(ItemRarity.LEGENDARY)), new ItemStack(Items.NETHERITE_SCRAP)
                    },
                    new ItemStack(ModItems.RING_NETHERITE),
                    ProfessionType.MYSTIC,
                    data -> true, // как в CraftingResultSlotMixin: незеритовое кольцо — просто только Мистик
                    Component.empty()
            ),
            // enchanting_table.json: " B " / "A#A" / "###"  (B = книга, A = аметистовый осколок, # = гладкий базальт)
            new Entry(
                    "enchanting_table",
                    Component.translatable("recipe.arz_classes.enchanting_table"),
                    new ItemStack[]{
                            ItemStack.EMPTY, new ItemStack(Items.BOOK), ItemStack.EMPTY,
                            new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.SMOOTH_BASALT), new ItemStack(Items.AMETHYST_SHARD),
                            new ItemStack(Items.SMOOTH_BASALT), new ItemStack(Items.SMOOTH_BASALT), new ItemStack(Items.SMOOTH_BASALT)
                    },
                    new ItemStack(Items.ENCHANTING_TABLE),
                    ProfessionType.MYSTIC,
                    data -> true,
                    Component.empty()
            ),
            new Entry(
                    "miner_helmet",
                    Component.translatable("recipe.arz_classes.miner_helmet"),
                    new ItemStack[]{
                            new ItemStack(Items.LEATHER), new ItemStack(Items.LANTERN), new ItemStack(Items.LEATHER),
                            new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.GOLD_INGOT),
                            new ItemStack(Items.GOLD_INGOT), ItemStack.EMPTY, new ItemStack(Items.GOLD_INGOT)
                    },
                    new ItemStack(ModItems.MINER_HELMET),
                    ProfessionType.MINER,
                    data -> data.hasAbility("miner_helmet"),
                    Component.translatable("recipe.arz_classes.miner_helmet.locked")
            ),
            new Entry(
                    "firewood",
                    Component.translatable("recipe.arz_classes.firewood"),
                    new ItemStack[]{
                            new ItemStack(Items.IRON_AXE), ItemStack.EMPTY, ItemStack.EMPTY,
                            new ItemStack(Items.OAK_LOG), ItemStack.EMPTY, ItemStack.EMPTY,
                            ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY
                    },
                    new ItemStack(ModItems.FIREWOOD, 4),
                    ProfessionType.LUMBERJACK,
                    data -> data.hasAbility("lumberjack_coal_chance"),
                    Component.translatable("recipe.arz_classes.firewood.locked")
            ),
            new Entry(
                    "carpenter_workbench",
                    Component.translatable("recipe.arz_classes.carpenter_workbench"),
                    new ItemStack[]{
                            new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OAK_PLANKS),
                            new ItemStack(Items.OAK_LOG), new ItemStack(Items.CRAFTING_TABLE), new ItemStack(Items.OAK_LOG),
                            new ItemStack(Items.OAK_LOG), new ItemStack(Items.OAK_LOG), new ItemStack(Items.OAK_LOG)
                    },
                    new ItemStack(ModBlocks.CARPENTER_WORKBENCH),
                    ProfessionType.LUMBERJACK,
                    data -> data.hasAbility("lumberjack_decor"),
                    Component.translatable("recipe.arz_classes.carpenter_workbench.locked")
            ),
            new Entry(
                    "rarity_upgrade_table",
                    Component.translatable("recipe.arz_classes.rarity_upgrade_table"),
                    new ItemStack[]{
                            new ItemStack(Items.GOLD_INGOT), new ItemStack(Items.DIAMOND), new ItemStack(Items.GOLD_INGOT),
                            new ItemStack(Items.OBSIDIAN), new ItemStack(Items.BOOK), new ItemStack(Items.OBSIDIAN),
                            new ItemStack(Items.OBSIDIAN), new ItemStack(Items.OBSIDIAN), new ItemStack(Items.OBSIDIAN)
                    },
                    new ItemStack(ModBlocks.RARITY_UPGRADE_TABLE),
                    null,               // null = виден всем классам
                    data -> true,
                    Component.empty()
            )
    );

    private static ItemStack foreignItem(String namespace, String path, int count) {
        Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
        return BuiltInRegistries.ITEM.get(id)
                .map(holder -> new ItemStack(holder, count))
                .orElse(ItemStack.EMPTY);
    }


    private ModRecipeBook() {
    }
}