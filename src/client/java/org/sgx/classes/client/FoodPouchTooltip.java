package org.sgx.classes.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.FoodPouchItem;

public final class FoodPouchTooltip {

    private FoodPouchTooltip() {
    }

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!(stack.getItem() instanceof FoodPouchItem)) {
                return;
            }
            lines.add(Component.translatable("tooltip.arz_classes.food_pouch_hint")
                    .withStyle(ChatFormatting.GRAY));
            lines.add(Component.translatable("tooltip.arz_classes.food_pouch_content",
                            FoodPouchItem.storedAmount(stack), FoodPouchItem.MAX_CONTENT)
                    .withStyle(style -> style.withColor(0xFF55FF55)));

            // список содержимого; первая строка — выбранная еда (та, что съедится по ПКМ)
            boolean first = true;
            for (ItemStack food : FoodPouchItem.readContents(stack)) {
                if (food.isEmpty()) {
                    continue;
                }
                Component line = Component.literal(first ? "> " : "  ")
                        .append(food.getHoverName())
                        .append(Component.literal(" \u00D7" + food.getCount()))
                        .withStyle(first ? ChatFormatting.YELLOW : ChatFormatting.GRAY);
                lines.add(line);
                first = false;
            }

            lines.add(Component.translatable("tooltip.arz_classes.food_pouch_open")
                    .withStyle(ChatFormatting.GRAY));
        });
    }
}