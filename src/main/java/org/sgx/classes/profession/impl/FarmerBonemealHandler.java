package org.sgx.classes.profession.impl;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionData;

public final class FarmerBonemealHandler {

    private FarmerBonemealHandler() {
    }

    public static void register() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            ItemStack stack = player.getItemInHand(hand);
            if (!stack.is(Items.BONE_MEAL)) {
                return InteractionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            if (!(state.getBlock() instanceof CropBlock crop) || crop.isMaxAge(state)) {
                return InteractionResult.PASS;
            }

            ProfessionData data = ModAttachments.get(serverPlayer);
            if (!data.hasAbility("farmer_bonemeal_instant")) {
                return InteractionResult.PASS;
            }

            ServerLevel serverLevel = (ServerLevel) world;
            serverLevel.setBlockAndUpdate(pos, crop.getStateForAge(crop.getMaxAge()));
            serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    8, 0.3, 0.3, 0.3, 0.0);
            serverLevel.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0f, 1.0f);
            serverLevel.gameEvent(player, GameEvent.BLOCK_CHANGE, pos);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }

            return InteractionResult.SUCCESS;
        });
    }
}