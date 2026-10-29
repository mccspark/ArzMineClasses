package org.sgx.classes.item;

/**
 * Учёт зелий, которые РЕАЛЬНО сварены в зельеварке (а не положены в неё игроком).
 * Нужен, чтобы опыт мистика давался только за сваренные зелья и только один раз.
 */
public final class BrewedPotions {

    /** Сколько слотов с зельями у зельеварки (0, 1, 2; слот 3 - ингредиент, 4 - топливо). */
    public static final int POTION_SLOTS = 3;

    private BrewedPotions() {
    }

    /** Реализуется миксином на BrewingStandBlockEntity (состояние хранится в самой зельеварке). */
    public interface State {
        boolean arzClasses$isBrewed(int slot);

        void arzClasses$setBrewed(int slot, boolean brewed);

        /** Вызывается в начале serverTick: запоминает состояние до тика. */
        void arzClasses$beforeTick();

        /** Вызывается в конце serverTick: если варка только что завершилась - помечает зелья как сваренные. */
        void arzClasses$afterTick();
    }
}
