package org.sgx.classes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.sgx.classes.profession.impl.PlacedLogTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Block.class)
public abstract class BlockSetPlacedByMixin {

    @Inject(method = "setPlacedBy", at = @At("TAIL"))
    private void arz_classes$onSetPlacedBy(Level level, BlockPos pos, BlockState state,
                                           LivingEntity placer, ItemStack stack, CallbackInfo ci) {
        if (level.isClientSide() || !(placer instanceof Player)) {
            return;
        }
        if (state.getBlock().builtInRegistryHolder().is(BlockTags.LOGS)) {
            PlacedLogTracker.get((ServerLevel) level).markPlaced(pos);
        }
    }
}