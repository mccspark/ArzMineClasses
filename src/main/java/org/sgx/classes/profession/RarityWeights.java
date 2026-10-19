package org.sgx.classes.profession;

import org.sgx.classes.item.ItemRarity;

import java.util.ArrayList;
import java.util.List;

/**
 * Веса секторов колеса редкости. У ОБЫЧНОГО игрока — всегда фиксированный 2% на Редкое,
 * без доступа к остальным тирам. У кузнеца каждая открытая редкость растёт ЛИНЕЙНО от
 * базового значения (на уровне, где узел открылся) до максимума на 20 уровне — кроме
 * Мифического, которое доступно только на 20 уровне и потому зафиксировано.
 *
 * На максимальном уровне (20) доли специально подобраны так, чтобы Обычное практически
 * исчезло, а Редкое заняло его место как новый доминирующий исход:
 * Мифическое 1% + Легендарное 10% + Эпическое 30% + Редкое 59% = 100%, Обычное = 0%.
 */
public final class RarityWeights {

    private static final double DEFAULT_RARE_CHANCE = 0.02;

    private static final int RARE_UNLOCK_LEVEL = 3;
    private static final double RARE_BASE = 0.03;
    private static final double RARE_MAX = 0.59;

    private static final int EPIC_UNLOCK_LEVEL = 7;
    private static final double EPIC_BASE = 0.02;
    private static final double EPIC_MAX = 0.30;

    private static final int LEGENDARY_UNLOCK_LEVEL = 14;
    private static final double LEGENDARY_BASE = 0.01;
    private static final double LEGENDARY_MAX = 0.10;

    private static final double MYTHIC_FIXED = 0.01;

    private RarityWeights() {
    }

    public record Weight(ItemRarity rarity, double chance) {
    }

    public static List<Weight> compute(ProfessionData data) {
        boolean isBlacksmith = data.currentProfession() == ProfessionType.BLACKSMITH;
        int level = data.level();

        double rareChance = DEFAULT_RARE_CHANCE;
        double epicChance = 0.0;
        double legendaryChance = 0.0;
        double mythicChance = 0.0;

        if (isBlacksmith) {
            if (data.hasAbility("blacksmith_rarity_rare")) {
                rareChance = growth(level, RARE_UNLOCK_LEVEL, ProfessionType.MAX_LEVEL, RARE_BASE, RARE_MAX);
            }
            if (data.hasAbility("blacksmith_rarity_epic")) {
                epicChance = growth(level, EPIC_UNLOCK_LEVEL, ProfessionType.MAX_LEVEL, EPIC_BASE, EPIC_MAX);
            }
            if (data.hasAbility("armorer_rarity_legendary") || data.hasAbility("weaponsmith_rarity_legendary")) {
                legendaryChance = growth(level, LEGENDARY_UNLOCK_LEVEL, ProfessionType.MAX_LEVEL, LEGENDARY_BASE, LEGENDARY_MAX);
            }
            if (data.hasAbility("armorer_rarity_mythic") || data.hasAbility("weaponsmith_rarity_mythic")) {
                mythicChance = MYTHIC_FIXED;
            }
        }

        double commonChance = Math.max(0.0, 1.0 - rareChance - epicChance - legendaryChance - mythicChance);

        List<Weight> weights = new ArrayList<>();
        weights.add(new Weight(ItemRarity.COMMON, commonChance));
        weights.add(new Weight(ItemRarity.RARE, rareChance));
        weights.add(new Weight(ItemRarity.EPIC, epicChance));
        weights.add(new Weight(ItemRarity.LEGENDARY, legendaryChance));
        weights.add(new Weight(ItemRarity.MYTHIC, mythicChance));
        return weights;
    }

    /** Линейный рост от base (на unlockLevel) до max (на maxLevel), плато на границах. */
    private static double growth(int level, int unlockLevel, int maxLevel, double base, double max) {
        if (level <= unlockLevel) return base;
        if (level >= maxLevel) return max;
        double t = (double) (level - unlockLevel) / (maxLevel - unlockLevel);
        double eased = t * t;
        return base + eased * (max - base);
    }
}