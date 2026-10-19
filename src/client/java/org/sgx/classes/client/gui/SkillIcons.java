package org.sgx.classes.client.gui;

import net.minecraft.resources.Identifier;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.profession.SubclassType;
import org.sgx.classes.profession.skill.SkillDefinition;

public final class SkillIcons {

    // --- Кузнец (assets/arz_classes/textures/gui/sprites/skill/blacksmith/) ---
    private static final Identifier BLACKSMITH_SHARPEN = sprite("skill/blacksmith/sharpen");
    private static final Identifier BLACKSMITH_REPAIR = sprite("skill/blacksmith/repair");
    private static final Identifier BLACKSMITH_ARMOR_BASE = sprite("skill/blacksmith/armor_base");
    private static final Identifier BLACKSMITH_ARMOR_IMPROVE = sprite("skill/blacksmith/armor_improve");
    private static final Identifier BLACKSMITH_WEAPON_ITEM = sprite("skill/blacksmith/weapon_item");
    private static final Identifier BLACKSMITH_WEAPON_IMPROVE = sprite("skill/blacksmith/weapon_improve");
    private static final Identifier BLACKSMITH_NETHERITE = sprite("skill/blacksmith/netherite");

    private static final Identifier RARITY_RARE = sprite("skill/rarity/rare");
    private static final Identifier RARITY_EPIC = sprite("skill/rarity/epic");
    private static final Identifier RARITY_LEGENDARY = sprite("skill/rarity/legendary");
    private static final Identifier RARITY_MYTHIC = sprite("skill/rarity/mythic");

    private static final Identifier FARMER_AUTO_SAPLING = sprite("skill/farmer/auto_sapling");
    private static final Identifier FARMER_DROP_MULTIPLIER = sprite("skill/farmer/drop_multiplier");
    private static final Identifier FARMER_IMPROVED_FARM = sprite("skill/farmer/improved_farm");
    private static final Identifier FARMER_MEAL = sprite("skill/farmer/meal");
    private static final Identifier FARMER_MEAL_BAG = sprite("skill/farmer/meal_bag");
    private static final Identifier FARMER_MEAT_MULTIPLIER = sprite("skill/farmer/meat_multiplier");
    private static final Identifier FARMER_ICON_COOK = sprite("skill/farmer/icon_cook");
    private static final Identifier FARMER_MEAL_BAG_UPGRADE = sprite("skill/farmer/meal_bag_upgrade");
    private static final Identifier FARMER_RARE_DROP = sprite("skill/farmer/rare_drop");
    private static final Identifier FARMER_FOOD_IMPROVE = sprite("skill/farmer/food_improve");
    private static final Identifier FARMER_BONES = sprite("skill/farmer/bones");
    private static final Identifier FARMER_UNPRETENTIOUS = sprite("skill/farmer/unpretentious");
    private static final Identifier FARMER_CAKE = sprite("skill/farmer/cake");
    private static final Identifier RARE_DROP = sprite("skill/farmer/rare_drop");
    private static final Identifier ICON_BOOZE = sprite("skill/farmer/icon_booze");
    private static final Identifier ALCHO_IMPROVE = sprite("skill/farmer/alcho_improve");
    private static final Identifier BOOZE = sprite("skill/farmer/booze");

    private static final Identifier PLACEHOLDER = sprite("skill/placeholder");

    private static final Identifier SPEED = sprite("skill/lumberjack/speed");
    private static final Identifier COAL = sprite("skill/lumberjack/coal");
    private static final Identifier CHANCE_FOR_DROP = sprite("skill/lumberjack/chance_for_drop");
    private static final Identifier DECO = sprite("skill/lumberjack/deco");
    private static final Identifier TREE_CAPITATOR = sprite("skill/lumberjack/treecapitator");
    private static final Identifier CARPENTER = sprite("skill/lumberjack/speed");
    private static final Identifier JAEGER = sprite("skill/lumberjack/jaeger");

    private static final Identifier MINER_SPEED = sprite("skill/miner/speed");
    private static final Identifier DROP_MULTIPLIER = sprite("skill/miner/drop_multiplier");
    private static final Identifier OVERLAY_COAL = sprite("skill/miner/coal");
    private static final Identifier OVERLAY_IRON = sprite("skill/miner/iron");
    private static final Identifier OVERLAY_GOLD = sprite("skill/miner/gold");
    private static final Identifier HELMET = sprite("skill/miner/helmet");
    private static final Identifier COMBAT_MINER = sprite("skill/miner/combat_miner");
    private static final Identifier CLAN_FARMER = sprite("skill/miner/clan_farmer");


    private static  final  Identifier ALCHEMIST = sprite("skill/mystic/alchemist");
    private static  final  Identifier ENCHANTER = sprite("skill/mystic/enchanter");
    private static  final  Identifier BONUS_XP = sprite("skill/mystic/bonus_exp");
    private static  final  Identifier ENCHANTMENT_UP = sprite("skill/mystic/ench_up");
    private static  final  Identifier ENCHANTMENT_TABLE = sprite("skill/mystic/enchant_table");
    private static  final  Identifier ENDER_MAN = sprite("skill/mystic/enderman");
    private static  final  Identifier LESS_XP = sprite("skill/mystic/less_exp");
    private static  final  Identifier MAX_LVL = sprite("skill/mystic/max_lvl");
    private static  final  Identifier RING_DIAMOND = sprite("skill/mystic/ring_diamond");
    private static  final  Identifier RING_IRON = sprite("skill/mystic/ring_iron");
    private SkillIcons() {
    }

    private static Identifier sprite(String path) {
        return Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, path);
    }

    public static Identifier of(SkillDefinition skill) {
        String id = skill.id();

        // --- Редкость — проверяем ПЕРВЫМИ, общая иконка независимо от ветки ---
        if (id.startsWith("blacksmith_rarity_rare")) {
            return RARITY_RARE;
        }
        if (id.startsWith("blacksmith_rarity_epic")) {
            return RARITY_EPIC;
        }
        if (id.startsWith("armorer_rarity_legendary") || id.startsWith("weaponsmith_rarity_legendary")) {
            return RARITY_LEGENDARY;
        }
        if (id.startsWith("armorer_final_mythic") || id.startsWith("weaponsmith_final_mythic")) {
            return RARITY_MYTHIC;
        }

        // --- Кузнец (главная цепочка, до подкласса) ---
        if (id.startsWith("blacksmith_sharpen")) {
            return BLACKSMITH_SHARPEN;
        }
        if (id.startsWith("blacksmith_unlimited_repair") || id.startsWith("blacksmith_durability")) {
            return BLACKSMITH_REPAIR;
        }

        // --- Бронник ---
        if (id.startsWith("armorer_sharpen")) {
            return BLACKSMITH_ARMOR_IMPROVE;
        }
        if (id.startsWith("armorer_netherite_armor")) {
            return BLACKSMITH_NETHERITE;
        }
        // armorer_durability (прочность БРОНИ) — своей иконки нет, PLACEHOLDER

        // --- Оружейник ---
        if (id.startsWith("weaponsmith_sharpen")) {
            return BLACKSMITH_WEAPON_IMPROVE;
        }
        if (id.startsWith("weaponsmith_durability")) {
            return BLACKSMITH_REPAIR;
        }
        if (id.startsWith("weaponsmith_netherite_tools")) {
            return BLACKSMITH_NETHERITE;
        }

        // --- Фермер (главная цепочка) ---
        if (id.startsWith("farmer_crop_yield")) {
            return FARMER_DROP_MULTIPLIER; // "больше дропа" — общий множитель, культуры
        }
        if (id.startsWith("farmer_animal_yield")) {
            return FARMER_MEAT_MULTIPLIER; // больше дропа мяса с мирных животных
        }
        if (id.startsWith("farmer_autoplant")) {
            return FARMER_AUTO_SAPLING;
        }
        if (id.startsWith("farmer_bonemeal_instant")) {
            return FARMER_MEAL; // всегда одна костная мука на полный рост
        }
        if (id.startsWith("farmer_bone_pouch_unlock")) {
            return FARMER_MEAL_BAG; // открывает крафт костного мешочка
        }
        if (id.startsWith("farmer_farmdelight_growing") || id.startsWith("farmer_delight_plots")) {
            return FARMER_IMPROVED_FARM; // грядки/фарм-делайт связанные милстоуны
        }
        if (id.startsWith("farmer_rare_crop_drop")) {
            return RARE_DROP;
        }

        // --- Повар (ветка) ---
        if (id.startsWith("cook_farmdelight_cooking")) {
            return FARMER_ICON_COOK;
        }
        if (id.startsWith("cook_final_maple")) {
            return FARMER_CAKE;
        }
        if (id.startsWith("cook_bag_range")) {
            return FARMER_MEAL_BAG_UPGRADE;
        }
        if (id.startsWith("cook_quality_star")) {
            return FARMER_FOOD_IMPROVE; // качество еды (звёзды)
        }
        if (id.startsWith("cook_rare_crop_drop")) {
            return FARMER_RARE_DROP; // шанс редкого дропа с растений (золотая морковь и т.п.)
        }
        if (id.startsWith("cook_bone_from_animals")) {
            return FARMER_BONES; // кости с мирных животных
        }
        if (id.startsWith("cook_unlimited_food")) {
            return FARMER_UNPRETENTIOUS; // снимает лимит SpiceOfLife2 на разнообразие еды
        }

        // --- Алкаш (пока без изменений — ветка ещё не пересобрана) ---
        if (id.startsWith("drunkard_quality")) {
            return ALCHO_IMPROVE;
        }
        if (id.startsWith("drunkard_no_debuffs")) {
            return BOOZE;
        }
        if (id.startsWith("lumberjack_speed")) {
            return SPEED;
        }
        if (id.startsWith("lumberjack_leaf_drop")) {
            return CHANCE_FOR_DROP;
        }
        if (id.startsWith("lumberjack_coal_unlock")) {
            return COAL;
        }
        if (id.startsWith("lumberjack_decor")) {
            return DECO;
        }
        if (id.startsWith("lumberjack_treecapitator")) {
            return  TREE_CAPITATOR;
        }
        if (id.startsWith("lumberjack_coal_boost"))
            return COAL;


        // Miner | Шахтёр
        if (id.startsWith("miner_speed")) {
            return MINER_SPEED;
        }
        if (id.startsWith("miner_bonus_coal")) {
            return DROP_MULTIPLIER;
        }
        if (id.startsWith("miner_bonus_iron")) {
            return DROP_MULTIPLIER;
        }
        if (id.startsWith("miner_bonus_gold")) {
            return DROP_MULTIPLIER;
        }
        if (id.startsWith("miner_helmet")) {
            return HELMET;
        }

        //Mystic | Мистик
        if (id.startsWith("mystic_enchant_threshold")) {
            return LESS_XP;
        }
        if (id.startsWith("mystic_xp_bonus")) {
            return BONUS_XP;
        }
        if (id.startsWith("mystic_max_level")) {
            return MAX_LVL;
        }
        if (id.startsWith("mystic_iron_ring_craft")) {
            return RING_IRON;
        }
        if (id.startsWith("mystic_enderman_calm")) {
            return ENDER_MAN;
        }
        if (id.startsWith("mystic_all_enchants_bonus")) {
            return ENCHANTMENT_UP;
        }
        if (id.startsWith("mystic_gold_ring_craft")) {
            return RING_DIAMOND;
        }

        return PLACEHOLDER;
    }
    public static Identifier overlayOf(SkillDefinition skill) {
        String id = skill.id();
        if (id.startsWith("miner_bonus_coal")) {
            return OVERLAY_COAL;
        }
        if (id.startsWith("miner_bonus_iron")) {
            return OVERLAY_IRON;
        }
        if (id.startsWith("miner_bonus_gold")) {
            return OVERLAY_GOLD;
        }
        return null;
    }


    public static Identifier forSubclassChoice(SubclassType subclass) {
        return switch (subclass) {
            case ARMORER -> BLACKSMITH_ARMOR_BASE;
            case WEAPONSMITH -> BLACKSMITH_WEAPON_ITEM;
            case COOK -> FARMER_ICON_COOK;
            case DRUNKARD -> ICON_BOOZE;
            case CARPENTER -> CARPENTER;
            case JAEGER -> JAEGER;
            case COMBAT_MINER -> COMBAT_MINER;
            case CLAN_FARMER -> CLAN_FARMER;
            case ALCHEMIST -> ALCHEMIST;
            case ENCHANTER -> ENCHANTER;
            default -> PLACEHOLDER;
        };
    }
}