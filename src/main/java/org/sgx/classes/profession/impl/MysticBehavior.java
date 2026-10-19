package org.sgx.classes.profession.impl;

import org.sgx.classes.profession.ProfessionBehavior;
import org.sgx.classes.profession.ProfessionType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class MysticBehavior implements ProfessionBehavior {

    private static final int XP_PER_ENCHANT_LEVEL = 40;
    private static final int XP_PER_POTION_BASE = 100;
    private static final int XP_PER_POTION_TIER_BONUS = 40;

    @Override
    public ProfessionType type() {
        return ProfessionType.MYSTIC;
    }

    @Override
    public int onItemEnchanted(ServerPlayer player, ServerLevel world, int enchantPower) {
        return enchantPower * XP_PER_ENCHANT_LEVEL;
    }

    @Override
    public int onPotionTaken(ServerPlayer player, ServerLevel world, int potionTier) {
        return XP_PER_POTION_BASE + potionTier * XP_PER_POTION_TIER_BONUS;
    }
}