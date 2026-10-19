package org.sgx.classes.block.chest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/** Сундук из конкретной породы дерева. trapped = сундук-ловушка (даёт сигнал редстоуна, пока открыт). */
public class WoodChestBlock extends ChestBlock {

    private final String wood;
    private final boolean trapped;

    public WoodChestBlock(String wood, boolean trapped, BlockBehaviour.Properties properties) {
        super(typeFor(trapped), SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties);
        this.wood = wood;
        this.trapped = trapped;
    }

    private static Supplier<BlockEntityType<? extends ChestBlockEntity>> typeFor(boolean trapped) {
        return () -> trapped ? ModChests.TRAPPED_CHEST_ENTITY : ModChests.CHEST_ENTITY;
    }

    public String wood() {
        return wood;
    }

    public boolean isTrapped() {
        return trapped;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return trapped ? new TrappedWoodChestBlockEntity(pos, state) : new WoodChestBlockEntity(pos, state);
    }

    @Override
    protected Stat<Identifier> getOpenChestStat() {
        return trapped ? Stats.CUSTOM.get(Stats.TRIGGER_TRAPPED_CHEST) : super.getOpenChestStat();
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return trapped;
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return trapped ? Mth.clamp(ChestBlockEntity.getOpenCount(level, pos), 0, 15) : 0;
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return trapped && direction == Direction.UP ? state.getSignal(level, pos, direction) : 0;
    }
}
