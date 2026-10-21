package org.sgx.classes.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Настенная оружейная стойка на 3 предмета. FACING — куда смотрит лицевая сторона (от стены).
 * ПКМ предметом — повесить (в слот, на который навели, иначе в первый свободный).
 * ПКМ пустой рукой — снять предмет из слота под курсором.
 */
public class WeaponRackBlock extends BaseEntityBlock {

    public static final MapCodec<WeaponRackBlock> CODEC = simpleCodec(WeaponRackBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    // Толщина 3 пикселя у стены; один и тот же размер, повёрнутый под сторону
    private static final VoxelShape SHAPE_NORTH = Block.box(0, 1, 13, 16, 15, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.box(0, 1, 0, 16, 15, 3);
    private static final VoxelShape SHAPE_EAST = Block.box(0, 1, 0, 3, 15, 16);
    private static final VoxelShape SHAPE_WEST = Block.box(13, 1, 0, 16, 15, 16);

    public WeaponRackBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WeaponRackBlockEntity(pos, state);
    }

    // ---------- форма и установка ----------

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    /** Ставится только на боковую грань блока (на стену). */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (face.getAxis().isVertical()) {
            return null;
        }
        BlockState state = defaultBlockState().setValue(FACING, face);
        return canSurvive(state, context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos wallPos = pos.relative(facing.getOpposite());
        return level.getBlockState(wallPos).isFaceSturdy(level, wallPos, facing);
    }

    /** Убрали стену — стойка отваливается (дропается через loot table, содержимое — через Container). */
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction directionToNeighbour, BlockPos neighbourPos, BlockState neighbourState,
                                     RandomSource random) {
        if (directionToNeighbour == state.getValue(FACING).getOpposite() && !canSurvive(state, level, pos)) {
            return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, directionToNeighbour, neighbourPos, neighbourState, random);
    }

    // ---------- взаимодействие ----------

    /** Номер слота 0..2 слева направо с точки зрения игрока, смотрящего на лицевую сторону. */
    private static int slotAt(BlockState state, BlockPos pos, Vec3 hitLocation) {
        double fx = hitLocation.x - pos.getX();
        double fz = hitLocation.z - pos.getZ();
        double t = switch (state.getValue(FACING)) {
            case NORTH -> 1.0 - fx;
            case SOUTH -> fx;
            case EAST -> 1.0 - fz;
            default -> fz; // WEST
        };
        return Math.max(0, Math.min(WeaponRackBlockEntity.SLOTS - 1, (int) (t * WeaponRackBlockEntity.SLOTS)));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (!(level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack) || rack.isFull()) {
            return InteractionResult.PASS; // стойка полна — предмет работает как обычно
        }
        if (!level.isClientSide()) {
            if (rack.tryInsert(slotAt(state, pos, hit.getLocation()), stack)) {
                if (!player.hasInfiniteMaterials()) {
                    stack.shrink(1);
                }
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WeaponRackBlockEntity rack) || rack.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack taken = rack.takeAt(slotAt(state, pos, hit.getLocation()));
            if (!taken.isEmpty()) {
                player.getInventory().placeItemBackInInventory(taken);
                level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
