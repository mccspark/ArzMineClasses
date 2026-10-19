package org.sgx.classes.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingRecipe.class)
public interface ShapedRecipeMixin {

    @Inject(
            method = "getRemainingItems",
            at = @At("RETURN"),
            cancellable = true
    )
    private void arz$damageAxeOnCraft(CraftingInput input, CallbackInfoReturnable<NonNullList<ItemStack>> cir) {
        CraftingRecipe recipe = (CraftingRecipe) this;

        // Получаем результат крафта
        ItemStack result = recipe.assemble(input);

        if (!result.isEmpty() && BuiltInRegistries.ITEM.getKey(result.getItem()).toString().equals("arz_classes:firewood")) {
            NonNullList<ItemStack> remainders = cir.getReturnValue();

            for (int i = 0; i < input.size(); i++) {
                ItemStack stack = input.getItem(i);

                if (stack.is(ItemTags.AXES)) {
                    ItemStack damagedAxe = stack.copy();

                    // Наносим 1 урон
                    damagedAxe.setDamageValue(damagedAxe.getDamageValue() + 1);

                    // Если прочность кончилась — очищаем слот, иначе возвращаем поврежденный топор
                    if (damagedAxe.getDamageValue() >= damagedAxe.getMaxDamage()) {
                        remainders.set(i, ItemStack.EMPTY);
                    } else {
                        remainders.set(i, damagedAxe);
                    }
                }
            }

            cir.setReturnValue(remainders);
        }
    }
}