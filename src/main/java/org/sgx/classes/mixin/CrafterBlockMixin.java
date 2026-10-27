package org.sgx.classes.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.CrafterBlock;
import org.sgx.classes.item.ClassItemRestrictions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/** Автокрафтер не крафтит предметы классов: для него рецепт считается ненайденным (как «нет рецепта»). */
@Mixin(CrafterBlock.class)
public abstract class CrafterBlockMixin {

    @Inject(method = "getPotentialResults", at = @At("RETURN"), cancellable = true)
    private static void arzClasses$blockClassItems(ServerLevel level, CraftingInput input,
                                                   CallbackInfoReturnable<Optional<RecipeHolder<CraftingRecipe>>> cir) {
        Optional<RecipeHolder<CraftingRecipe>> found = cir.getReturnValue();
        if (found == null || found.isEmpty()) {
            return;
        }
        ItemStack result = found.get().value().assemble(input);
        if (ClassItemRestrictions.isClassCraftOnly(result)) {
            cir.setReturnValue(Optional.empty());
        }
    }
}
