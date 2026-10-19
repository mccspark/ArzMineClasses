package org.sgx.classes.profession;

import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.ArrayList;
import java.util.List;

public final class MysticEnchantBoost {

    public static final int ENCHANTER_MAX_LEVEL = 13;

    private MysticEnchantBoost() {
    }

    public static List<EnchantmentInstance> apply(List<EnchantmentInstance> original, ProfessionData data) {
        if (data.currentProfession() != ProfessionType.MYSTIC || original.isEmpty()) {
            return original;
        }
        boolean canExceed = data.hasAbility("mystic_max_level");        // узел 3
        boolean allPlusOne = data.hasAbility("mystic_all_enchants_bonus"); // узел 7
        if (!canExceed && !allPlusOne) {
            return original;
        }
        boolean enchanter = data.currentSubclass() == SubclassType.ENCHANTER;

        List<EnchantmentInstance> boosted = new ArrayList<>(original.size());
        for (EnchantmentInstance inst : original) {
            int vanillaMax = inst.enchantment().value().getMaxLevel();
            if (vanillaMax <= 1) {
                boosted.add(inst);
                continue;
            }
            int cap = enchanter ? ENCHANTER_MAX_LEVEL : (canExceed ? vanillaMax + 1 : vanillaMax);
            int level = inst.level();

            if (allPlusOne) {
                level = level + 1;
            }
            if (canExceed && inst.level() >= vanillaMax) {
                level = Math.max(level, vanillaMax + 1);
            }
            boosted.add(new EnchantmentInstance(inst.enchantment(), Math.min(cap, level)));
        }
        return boosted;
    }
}