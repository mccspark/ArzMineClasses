package org.sgx.classes.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Манекен для тренировок: двухблочный (низ + верх), лицом к игроку при установке.
 * Не ломается ударами — удар по нему считает урон оружия (см. TrainingDummyEvents),
 * забрать манекен можно Shift+ПКМ пустой рукой.
 */
public class TrainingDummyBlock extends Block {

    public static final MapCodec<TrainingDummyBlock> CODEC = simpleCodec(TrainingDummyBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private static final VoxelShape LOWER_NS = Block.box(1, 0, 1, 15, 16, 15);
    private static final VoxelShape UPPER_NS = Block.box(0, 0, 3, 16, 16, 13);
    private static final VoxelShape UPPER_EW = Block.box(3, 0, 0, 13, 16, 16);

    public TrainingDummyBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return LOWER_NS;
        }
        return state.getValue(FACING).getAxis() == Direction.Axis.X ? UPPER_EW : UPPER_NS;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        Level level = context.getLevel();
        if (level.isOutsideBuildHeight(pos.above()) || !level.getBlockState(pos.above()).canBeReplaced(context)) {
            return null;
        }
        BlockState state = defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(HALF, DoubleBlockHalf.LOWER);
        return canSurvive(state, level, pos) ? state : null;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), 3);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (state.getValue(HALF) == DoubleBlockHalf.LOWER) {
            return belowState.isFaceSturdy(level, below, Direction.UP);
        }
        return belowState.is(this) && belowState.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    /** Пропала одна половина — вторая исчезает без лишнего дропа; нет опоры — манекен падает. */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        DoubleBlockHalf half = state.getValue(HALF);
        if (directionToNeighbour.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (directionToNeighbour == Direction.UP)) {
            if (!neighbourState.is(this) || neighbourState.getValue(HALF) == half) {
                return Blocks.AIR.defaultBlockState();
            }
            return state;
        }
        if (half == DoubleBlockHalf.LOWER && directionToNeighbour == Direction.DOWN
                && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }
}
