package org.sgx.classes.block.chest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class WoodChestBlockEntity extends ChestBlockEntity {
    public WoodChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModChests.CHEST_ENTITY, pos, state);
    }
}
