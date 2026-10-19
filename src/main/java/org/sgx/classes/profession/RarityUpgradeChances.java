package org.sgx.classes.profession;

import org.sgx.classes.item.ItemRarity;

public final class RarityUpgradeChances {

    private RarityUpgradeChances() {
    }

    public static double successChance(ItemRarity target) {
        return switch (target) {
            case RARE -> 0.50;
            case EPIC -> 0.20;
            case LEGENDARY -> 0.10;
            case MYTHIC -> 0.01;
            case COMMON -> -1.0;
        };
    }

    /** Следующая редкость после текущей, или null если уже MYTHIC (апгрейдить некуда). */
    public static ItemRarity next(ItemRarity current) {
        ItemRarity[] values = ItemRarity.values();
        int nextOrdinal = current.ordinal() + 1;
        return nextOrdinal < values.length ? values[nextOrdinal] : null;
    }
}