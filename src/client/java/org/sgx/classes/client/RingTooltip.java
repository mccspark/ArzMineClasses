package org.sgx.classes.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import org.sgx.classes.item.ExperienceRingItem;
import org.sgx.classes.item.ModItems;

public final class RingTooltip {

    private RingTooltip() {
    }

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            double bonus;
            if (stack.is(ModItems.RING_IRON)) {
                bonus = ExperienceRingItem.IRON_XP_BONUS_MULTIPLIER;
            } else if (stack.is(ModItems.RING_GOLD)) {
                bonus = ExperienceRingItem.GOLD_XP_BONUS_MULTIPLIER;
            } else if (stack.is(ModItems.RING_NETHERITE)) {
                bonus = ExperienceRingItem.NETHERITE_XP_BONUS_MULTIPLIER;
            } else {
                return;
            }
            lines.add(Component.translatable("tooltip.arz_classes.ring_xp_bonus",
                            String.format("%.0f", bonus * 100))
                    .withStyle(style -> style.withColor(0xFF55FF55)));
        });
    }
}