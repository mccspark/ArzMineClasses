package org.sgx.classes.profession;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;

public interface ProfessionBehavior {

    ProfessionType type();

    default int onBlockBroken(ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state) {
        return 0;
    }
    default int onItemEnchanted(ServerPlayer player, ServerLevel world, int enchantPower) {
        return 0;
    }
    default int onPotionTaken(ServerPlayer player, ServerLevel world, int potionTier) {
        return 0;
    }

    default int onPassiveMobKilled(ServerPlayer player, ServerLevel world, LivingEntity mob) {
        return 0;
    }
}