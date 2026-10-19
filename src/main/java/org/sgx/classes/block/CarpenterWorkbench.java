package org.sgx.classes.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionType;

public class CarpenterWorkbench extends BaseEntityBlock {

    public static final MapCodec<CarpenterWorkbench> CODEC = simpleCodec(CarpenterWorkbench::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CarpenterWorkbench(Properties properties) {
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

    /** Передняя сторона смотрит на игрока, который ставит блок. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CarpenterWorkbenchEntity(pos, state);
    }

    /** Столярным верстаком может пользоваться только Лесоруб. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            if (ModAttachments.get(serverPlayer).currentProfession() != ProfessionType.LUMBERJACK) {
                serverPlayer.sendSystemMessage(
                        Component.translatable("message.arz_classes.carpenter_lumberjack_only"));
                return InteractionResult.SUCCESS;
            }
            if (level.getBlockEntity(pos) instanceof CarpenterWorkbenchEntity be) {
                player.openMenu(be);
            }
        }
        return InteractionResult.SUCCESS;
    }
}