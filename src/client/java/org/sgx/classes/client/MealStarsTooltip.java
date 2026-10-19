package org.sgx.classes.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import org.sgx.classes.item.ModDataComponents;

public final class MealStarsTooltip {

    private MealStarsTooltip() {
    }

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!stack.has(ModDataComponents.MEAL_STARS)) {
                return;
            }
            int stars = stack.get(ModDataComponents.MEAL_STARS);
            StringBuilder starLine = new StringBuilder();
            for (int i = 0; i < stars; i++) {
                starLine.append("★");
            }
            lines.add(Component.literal(starLine.toString())
                    .withStyle(style -> style.withColor(0xFFFFD700)));
        });
    }
}