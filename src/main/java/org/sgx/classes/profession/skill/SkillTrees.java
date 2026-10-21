package org.sgx.classes.profession.skill;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.sgx.classes.profession.ProfessionType;
import org.sgx.classes.profession.SubclassType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SkillTrees {

    private static final Map<ProfessionType, List<SkillDefinition>> MAIN = new EnumMap<>(ProfessionType.class);
    private static final Map<SubclassType, List<SkillDefinition>> BRANCH = new EnumMap<>(SubclassType.class);
    /** База подкласса: эффекты, которые включаются сразу при выборе подкласса (в дереве узлом не показываются). */
    private static final Map<SubclassType, List<SkillEffect>> BASE = new EnumMap<>(SubclassType.class);

    static {
        // ---------------- ФЕРМЕР ----------------
        MAIN.put(ProfessionType.FARMER, new SkillChainBuilder(1)
                .scalar("farmer_crop_yield", 1, "farmer.crop_yield_percent", 2.0, "skill.arz_classes.farmer_crop_yield")
                .scalar("farmer_animal_yield", 1, "farmer.animal_kill_drop_percent", 4.0, "skill.arz_classes.farmer_animal_yield")
                .milestone("farmer_autoplant", "farmer_autoplant", "skill.arz_classes.farmer_autoplant")
                .milestone("farmer_bonemeal_instant", "farmer_bonemeal_instant", "skill.arz_classes.farmer_bonemeal_instant")
                .scalar("farmer_rare_crop_drop", 1, "farmer.rare_crop_drop_chance_percent", 1.0, "skill.arz_classes.farmer_rare_crop_drop")
                .scalarAndMilestone("farmer_bone_pouch_unlock", "farmer.bone_pouch_range", 1.0,
                        "farmer_bone_pouch", "skill.arz_classes.farmer_bone_pouch")
                .milestone("farmer_food_pouch", "farmer_food_pouch", "skill.arz_classes.farmer_food_pouch")
                .scalar("farmer_animal_yield", 1, "farmer.animal_kill_drop_percent", 4.0, "skill.arz_classes.farmer_animal_yield")
                .scalar("farmer_crop_yield", 1, "farmer.crop_yield_percent", 2.0, "skill.arz_classes.farmer_crop_yield")
                .build(10));

        BRANCH.put(SubclassType.COOK, new SkillChainBuilder(11)
                .scalar("cook_bag_range", 1, "farmer.bone_pouch_range", 2.0, "skill.arz_classes.cook_bag_range_3")
                .scalar("cook_quality_star", 1, "cook.quality_star_count", 1.0, "skill.arz_classes.cook_quality_star")
                .scalar("cook_crop_yield", 1, "farmer.crop_yield_percent", 2.0, "skill.arz_classes.cook_crop_yield")
                .scalar("cook_quality_star", 1, "cook.quality_star_count", 1.0, "skill.arz_classes.cook_quality_star")
                .milestone("cook_bone_from_animals", "cook_bone_from_animals", "skill.arz_classes.cook_bone_from_animals")
                .scalar("cook_quality_star", 1, "cook.quality_star_count", 1.0, "skill.arz_classes.cook_quality_star")
                .scalar("cook_rare_crop_drop", 1, "farmer.rare_crop_drop_chance_percent", 2.0, "skill.arz_classes.cook_rare_crop_drop")
                .scalar("cook_quality_star", 1, "cook.quality_star_count", 1.0, "skill.arz_classes.cook_quality_star")
                .scalar("cook_bag_range", 1, "farmer.bone_pouch_range", 2.0, "skill.arz_classes.cook_bag_range_5")
                .milestone("cook_final_maple", "cook_maple_cake", "skill.arz_classes.cook_maple_cake")
                .build(21));

        BRANCH.put(SubclassType.DRUNKARD, new SkillChainBuilder(11)
                .scalar("drunkard_quality", 10, "drunkard.alcohol_quality_percent", 3.0, "skill.arz_classes.drunkard_quality")
                .build(21));

        // ---------------- ШАХТЁР ----------------
        MAIN.put(ProfessionType.MINER, new SkillChainBuilder(1)
                .scalar("miner_speed", 1, "miner.mining_speed_percent", 5.0, "skill.arz_classes.miner_speed")
                .scalar("miner_bonus_coal", 1, "miner.bonus_drop_coal", 1.0, "skill.arz_classes.miner_bonus_coal")
                .milestone("miner_helmet", "miner_helmet", "skill.arz_classes.miner_helmet")
                .scalar("miner_speed", 1, "miner.mining_speed_percent", 5.0, "skill.arz_classes.miner_speed")
                .scalar("miner_bonus_iron", 1, "miner.bonus_drop_iron", 1.0, "skill.arz_classes.miner_bonus_iron")
                .scalar("miner_speed", 1, "miner.mining_speed_percent", 5.0, "skill.arz_classes.miner_speed")
                .scalar("miner_bonus_iron", 1, "miner.bonus_drop_iron", 1.0, "skill.arz_classes.miner_bonus_iron")
                .scalar("miner_speed", 1, "miner.mining_speed_percent", 5.0, "skill.arz_classes.miner_speed")
                .scalar("miner_bonus_gold", 1, "miner.bonus_drop_gold", 1.0, "skill.arz_classes.miner_bonus_gold")
                .build(10));

        BRANCH.put(SubclassType.COMBAT_MINER, new SkillChainBuilder(11)
                .scalar("combat_miner_dmg", 4, "combat_miner.pickaxe_damage_percent", 3.0, "skill.arz_classes.combat_miner_dmg")
                .milestone("combat_miner_night_vision", "combat_miner_night_vision", "skill.arz_classes.combat_miner_night_vision")
                .scalar("combat_miner_dmg", 5, "combat_miner.pickaxe_damage_percent", 3.0, "skill.arz_classes.combat_miner_dmg")
                .build(21));

        BRANCH.put(SubclassType.CLAN_FARMER, new SkillChainBuilder(11)
                .scalar("clan_farmer_smelt", 4, "clan_farmer.auto_smelt_chance_percent", 4.0, "skill.arz_classes.clan_farmer_smelt")
                .milestone("clan_farmer_netherite", "clan_farmer_mine_netherite", "skill.arz_classes.clan_farmer_netherite")
                .scalar("clan_farmer_smelt", 5, "clan_farmer.auto_smelt_chance_percent", 4.0, "skill.arz_classes.clan_farmer_smelt")
                .build(21));

        // ---------------- КУЗНЕЦ ----------------
        MAIN.put(ProfessionType.BLACKSMITH, new SkillChainBuilder(1)
                .scalar("blacksmith_sharpen", 2, "blacksmith.sharpen_efficiency_percent", 2.0, "skill.arz_classes.blacksmith_sharpen")
                .milestone("blacksmith_rarity_rare", "blacksmith_rarity_rare", "skill.arz_classes.rarity_rare")
                .scalar("blacksmith_durability", 1, "blacksmith.craft_durability_percent", 7.0, "skill.arz_classes.blacksmith_durability")
                .scalar("blacksmith_sharpen", 1, "blacksmith.sharpen_efficiency_percent", 2.0, "skill.arz_classes.blacksmith_sharpen")
                .scalar("blacksmith_durability", 1, "blacksmith.craft_durability_percent", 7.0, "skill.arz_classes.blacksmith_durability")
                .milestone("blacksmith_rarity_epic", "blacksmith_rarity_epic", "skill.arz_classes.rarity_epic")
                .scalar("blacksmith_sharpen", 2, "blacksmith.sharpen_efficiency_percent", 2.0, "skill.arz_classes.blacksmith_sharpen")
                .build(10));

        // 11 заточка, 12 прочность, 13 заточка, 14 легендарное, 15 прочность,
        // 16 заточка, 17 прочность, 18 незеритовая броня, 19 заточка, 20 финал (+заточка и мифическое)
        BRANCH.put(SubclassType.ARMORER, new SkillChainBuilder(11)
                .scalar("armorer_sharpen", 1, "armorer.sharpen_efficiency_percent", 3.0, "skill.arz_classes.armorer_sharpen")
                .scalar("armorer_durability", 1, "armorer.armor_durability_percent", 7.0, "skill.arz_classes.armorer_durability")
                .scalar("armorer_sharpen", 1, "armorer.sharpen_efficiency_percent", 3.0, "skill.arz_classes.armorer_sharpen")
                .milestone("armorer_rarity_legendary", "armorer_rarity_legendary", "skill.arz_classes.rarity_legendary")
                .scalar("armorer_durability", 1, "armorer.armor_durability_percent", 7.0, "skill.arz_classes.armorer_durability")
                .scalar("armorer_sharpen", 1, "armorer.sharpen_efficiency_percent", 3.0, "skill.arz_classes.armorer_sharpen")
                .scalar("armorer_durability", 1, "armorer.armor_durability_percent", 7.0, "skill.arz_classes.armorer_durability")
                .milestone("armorer_netherite_armor", "armorer_netherite_armor", "skill.arz_classes.armorer_netherite_armor")
                .scalar("armorer_sharpen", 1, "armorer.sharpen_efficiency_percent", 3.0, "skill.arz_classes.armorer_sharpen")
                .scalarAndMilestone("armorer_final_mythic", "armorer.sharpen_efficiency_percent", 3.0,
                        "armorer_rarity_mythic", "skill.arz_classes.rarity_mythic")
                .build(21));

        BRANCH.put(SubclassType.WEAPONSMITH, new SkillChainBuilder(11)
                .scalar("weaponsmith_sharpen", 1, "weaponsmith.sharpen_efficiency_percent", 3.0, "skill.arz_classes.weaponsmith_sharpen")
                .scalar("weaponsmith_durability", 1, "weaponsmith.tool_durability_percent", 7.0, "skill.arz_classes.weaponsmith_durability")
                .scalar("weaponsmith_sharpen", 1, "weaponsmith.sharpen_efficiency_percent", 3.0, "skill.arz_classes.weaponsmith_sharpen")
                .milestone("weaponsmith_rarity_legendary", "weaponsmith_rarity_legendary", "skill.arz_classes.rarity_legendary")
                .scalar("weaponsmith_durability", 1, "weaponsmith.tool_durability_percent", 7.0, "skill.arz_classes.weaponsmith_durability")
                .scalar("weaponsmith_sharpen", 1, "weaponsmith.sharpen_efficiency_percent", 3.0, "skill.arz_classes.weaponsmith_sharpen")
                .scalar("weaponsmith_durability", 1, "weaponsmith.tool_durability_percent", 7.0, "skill.arz_classes.weaponsmith_durability")
                .milestone("weaponsmith_netherite_tools", "weaponsmith_netherite_tools", "skill.arz_classes.weaponsmith_netherite_tools")
                .scalar("weaponsmith_sharpen", 1, "weaponsmith.sharpen_efficiency_percent", 3.0, "skill.arz_classes.weaponsmith_sharpen")
                .scalarAndMilestone("weaponsmith_final_mythic", "weaponsmith.sharpen_efficiency_percent", 3.0,
                        "weaponsmith_rarity_mythic", "skill.arz_classes.rarity_mythic")
                .build(21));

        // ---------------- ЛЕСОРУБ ----------------
        MAIN.put(ProfessionType.LUMBERJACK, new SkillChainBuilder(1)
                .scalar("lumberjack_speed", 1, "lumberjack.wood_break_speed_percent", 4.0, "skill.arz_classes.lumberjack_speed")
                .scalar("lumberjack_leaf_drop", 1, "lumberjack.leaf_drop_chance_percent", 5.0, "skill.arz_classes.lumberjack_leaf_drop")
                .scalarAndMilestone("lumberjack_coal_unlock", "lumberjack.coal_drop_chance_percent", 5.0,
                        "lumberjack_coal_chance", "skill.arz_classes.lumberjack_coal_unlock")
                .milestone("lumberjack_decor", "lumberjack_decor", "skill.arz_classes.lumberjack_decor")
                .scalar("lumberjack_leaf_drop", 1, "lumberjack.leaf_drop_chance_percent", 5.0, "skill.arz_classes.lumberjack_leaf_drop")
                .scalar("lumberjack_speed", 1, "lumberjack.wood_break_speed_percent", 4.0, "skill.arz_classes.lumberjack_speed")
                .scalar("lumberjack_coal_boost", 1, "lumberjack.coal_drop_chance_percent", 5.0, "skill.arz_classes.lumberjack_coal_boost")
                .scalar("lumberjack_speed", 1, "lumberjack.wood_break_speed_percent", 4.0, "skill.arz_classes.lumberjack_speed")
                .milestone("lumberjack_treecapitator", "lumberjack_treecapitator", "skill.arz_classes.lumberjack_treecapitator")
                .build(10));

        BRANCH.put(SubclassType.CARPENTER, new SkillChainBuilder(11)
                .scalar("carpenter_fall", 2, "carpenter.safe_fall_bonus", 1.0, "skill.arz_classes.carpenter_fall")
                .milestone("carpenter_woodcutter", "carpenter_woodcutter", "skill.arz_classes.carpenter_woodcutter")
                .scalar("carpenter_fall", 7, "carpenter.safe_fall_bonus", 1.0, "skill.arz_classes.carpenter_fall")
                .build(21));

        BRANCH.put(SubclassType.JAEGER, new SkillChainBuilder(11)
                .scalar("collier_coal", 10, "collier.coal_drop_chance_percent", 2.5, "skill.arz_classes.collier_coal")
                .build(21));

        // ---------------- МИСТИК ----------------
        MAIN.put(ProfessionType.MYSTIC, new SkillChainBuilder(1)
                .scalar("mystic_enchant_threshold", 1, "mystic.enchant_threshold_reduction", 1.0,
                        "skill.arz_classes.mystic_enchant_threshold")
                .scalar("mystic_xp_bonus", 1, "mystic.xp_bonus_percent", 5.0, "skill.arz_classes.mystic_xp_bonus")
                .milestone("mystic_max_level", "mystic_max_level", "skill.arz_classes.mystic_max_level")
                .milestone("mystic_iron_ring_craft", "mystic_iron_ring_craft", "skill.arz_classes.mystic_iron_ring_craft")
                .scalar("mystic_xp_bonus", 1, "mystic.xp_bonus_percent", 5.0, "skill.arz_classes.mystic_xp_bonus")
                .milestone("mystic_enderman_calm", "mystic_enderman_calm", "skill.arz_classes.mystic_enderman_calm")
                .milestone("mystic_all_enchants_bonus", "mystic_all_enchants_bonus", "skill.arz_classes.mystic_all_enchants_bonus")
                .scalar("mystic_enchant_threshold", 1, "mystic.enchant_threshold_reduction", 1.0,
                        "skill.arz_classes.mystic_enchant_threshold")
                .milestone("mystic_gold_ring_craft", "mystic_gold_ring_craft", "skill.arz_classes.mystic_gold_ring_craft")
                .build(10));

        BRANCH.put(SubclassType.ENCHANTER, new SkillChainBuilder(11)
                .scalar("enchanter_filler", 1, "enchanter.unused", 0.0, "skill.arz_classes.enchanter_filler")
                .milestone("enchanter_preview", "enchanter_see_potential", "skill.arz_classes.enchanter_preview")
                .scalar("enchanter_filler", 8, "enchanter.unused", 0.0, "skill.arz_classes.enchanter_filler")
                .build(21));

        BRANCH.put(SubclassType.ALCHEMIST, new SkillChainBuilder(11)
                .scalar("alchemist_potency", 10, "alchemist.potion_potency_percent", 3.0, "skill.arz_classes.alchemist_potency")
                .build(21));

        // ---------------- БАЗА ПОДКЛАССОВ (включается сразу при выборе подкласса) ----------------
        BASE.put(SubclassType.COOK, List.of(
                new SkillEffect.CustomScalar("cook.quality_star_count", 1.0)));

        BASE.put(SubclassType.ARMORER, List.of(
                new SkillEffect.Ability("armorer_armor_sharpen")));                // Заточка брони 1

        BASE.put(SubclassType.WEAPONSMITH, List.of(
                new SkillEffect.CustomScalar("weaponsmith.tool_durability_percent", 7.0))); // Прочность создаваемых инструментов

        BASE.put(SubclassType.DRUNKARD, List.of(
                new SkillEffect.Ability("drunkard_alcohol_craft")));               // Крафт алкоголя

        BASE.put(SubclassType.CARPENTER, List.of(
                new SkillEffect.Ability("carpenter_cheap_crafts")));               // Дешёвые крафты на столярном верстаке

        BASE.put(SubclassType.JAEGER, List.of(
                new SkillEffect.Ability("jaeger_night_vision"),                    // Ночью зрение
                new SkillEffect.CustomScalar("jaeger.night_speed_percent", 10.0))); // Ночью +скорость передвижения

        BASE.put(SubclassType.ENCHANTER, List.of(
                new SkillEffect.Ability("enchanter_reroll"),                       // Рерол чар
                new SkillEffect.CustomScalar("enchanter.reroll_xp_discount", 1.0))); // -n очков опыта за рерол

        BASE.put(SubclassType.ALCHEMIST, List.of(
                new SkillEffect.Ability("alchemist_mortar_pestle")));              // Ступка и пестик
    }

    private SkillTrees() {
    }

    public static List<List<SkillDefinition>> allChains() {
        List<List<SkillDefinition>> all = new java.util.ArrayList<>(MAIN.values());
        all.addAll(BRANCH.values());
        return all;
    }

    public static List<SkillDefinition> mainChain(ProfessionType type) {
        return MAIN.getOrDefault(type, List.of());
    }

    public static List<SkillDefinition> branchChain(SubclassType subclass) {
        return BRANCH.getOrDefault(subclass, List.of());
    }

    /** Эффекты, которые подкласс даёт сразу при выборе (до покупки узлов ветки). */
    public static List<SkillEffect> subclassBase(SubclassType subclass) {
        return BASE.getOrDefault(subclass, List.of());
    }
}