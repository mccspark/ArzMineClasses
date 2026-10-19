package org.sgx.classes.event;

import de.siphalor.capsaicin.api.food.ConsumableProperties;
import de.siphalor.capsaicin.api.food.FoodContext;
import de.siphalor.capsaicin.api.modifier.Modifier;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.sgx.classes.item.ModDataComponents;

public final class CookEatingSpeedModifier implements Modifier<ConsumableProperties, FoodContext> {

    @Override
    public @NotNull ConsumableProperties apply(@NotNull ConsumableProperties value, @NotNull FoodContext context) {
        ItemStack stack = context.stack();
        if (stack == null || !stack.has(ModDataComponents.MEAL_STARS)) {
            return value;
        }
        value.setConsumeSeconds(context.originalFoodConsumeDurationSeconds());
        return value;
    }
}