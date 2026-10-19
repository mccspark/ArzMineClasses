package org.sgx.classes.profession;

import org.sgx.classes.item.ItemRarity;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Взвешенный бросок редкости по списку RarityWeights.Weight — сумма весов не обязана быть 1.0. */
public final class RarityRoller {

    private RarityRoller() {
    }

    public static ItemRarity roll(List<RarityWeights.Weight> weights) {
        double total = 0.0;
        for (RarityWeights.Weight w : weights) {
            total += Math.max(0.0, w.chance());
        }
        if (total <= 0.0) {
            return ItemRarity.COMMON;
        }
        double roll = ThreadLocalRandom.current().nextDouble() * total;
        double cursor = 0.0;
        for (RarityWeights.Weight w : weights) {
            cursor += Math.max(0.0, w.chance());
            if (roll < cursor) {
                return w.rarity();
            }
        }
        return ItemRarity.COMMON;
    }
}