package org.sgx.classes.block.chest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;

/** Копия логики ванильного TrappedChestBlockEntity, но со своим типом блок-сущности. */
public class TrappedWoodChestBlockEntity extends ChestBlockEntity {
    public TrappedWoodChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModChests.TRAPPED_CHEST_ENTITY, pos, state);
    }

    @Override
    protected void signalOpenCount(Level level, BlockPos pos, BlockState blockState, int previous, int current) {
        super.signalOpenCount(level, pos, blockState, previous, current);
        if (previous != current) {
            Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(
                    level, blockState.getValue(ChestBlock.FACING).getOpposite(), Direction.UP);
            Block block = blockState.getBlock();
            level.updateNeighborsAt(pos, block, orientation);
            level.updateNeighborsAt(pos.below(), block, orientation);
        }
    }
}
