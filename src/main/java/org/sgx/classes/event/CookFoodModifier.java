package org.sgx.classes.event;

import de.siphalor.capsaicin.api.food.FoodContext;
import de.siphalor.capsaicin.api.food.FoodProperties;
import de.siphalor.capsaicin.api.modifier.Modifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.sgx.classes.item.ModDataComponents;

public final class CookFoodModifier implements Modifier<FoodProperties, FoodContext> {

    @Override
    public @NotNull FoodProperties apply(@NotNull FoodProperties value, @NotNull FoodContext context) {
        ItemStack stack = context.stack();
        if (stack == null || !stack.has(ModDataComponents.MEAL_STARS)) {
            return value;
        }
        int stars = stack.get(ModDataComponents.MEAL_STARS);
        value.setHunger(context.originalFoodHunger() + stars);
        value.setSaturationModifier(context.originalFoodSaturationModifier() + 0.5f * stars);
        return value;
    }
}