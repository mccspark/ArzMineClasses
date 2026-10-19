package org.sgx.classes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.sgx.classes.profession.impl.TreeChopSpeed;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourDestroyProgressMixin {

    @Inject(method = "getDestroyProgress", at = @At("RETURN"), cancellable = true)
    private void arz_classes$slowBigTrees(BlockState state, Player player, BlockGetter level, BlockPos pos,
                                          CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(TreeChopSpeed.adjust(cir.getReturnValue(), state, player, level, pos));
    }
}