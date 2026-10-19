package org.sgx.classes.profession;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.item.ModDataComponents;

/** Табличка шансов для предметов, которые появляются в мире САМИ (лут, снаряжение мобов),
 * не завязана на игрока/класс. Максимум в мире — Epic. Любой предмет с прочностью
 * получает редкость (COMMON тоже), чтобы все вещи выглядели единообразно. */
public final class WorldRarityRoller {

    private static final double RARE = 0.22;
    private static final double EPIC = 0.09;
    // COMMON = всё остальное (0.69)

    private WorldRarityRoller() {
    }

    public static ItemRarity roll(RandomSource random) {
        double r = random.nextDouble();
        double cursor = EPIC;
        if (r < cursor) return ItemRarity.EPIC;
        cursor += RARE;
        if (r < cursor) return ItemRarity.RARE;
        return ItemRarity.COMMON;
    }

    /** Все предметы с прочностью сначала обычные, дальше — по шансу выше. */
    public static void rollAndApply(ItemStack stack, RandomSource random) {
        if (stack.isEmpty() || stack.getMaxDamage() <= 0) {
            return;
        }
        if (stack.get(ModDataComponents.ITEM_RARITY) != null) {
            return;
        }
        RarityApplication.applyRarity(stack, roll(random));
    }
}