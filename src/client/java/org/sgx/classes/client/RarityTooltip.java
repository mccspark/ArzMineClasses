package org.sgx.classes.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.item.ModDataComponents;

public final class RarityTooltip {

    private RarityTooltip() {
    }

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            ItemRarity rarity = stack.get(ModDataComponents.ITEM_RARITY);
            if (rarity == null) {
                return;
            }

            if (rarity == ItemRarity.COMMON) {
                lines.add(1, Component.literal("☆").withStyle(ChatFormatting.GRAY)); // пустая звезда
            } else {
                Component stars = Component.literal("★".repeat(rarity.ordinal()))
                        .withStyle(style -> style.withColor(rarity.argb()));
                lines.add(1, stars); // сразу под именем предмета
            }

            if (rarity.statBonusPercent() > 0) {
                Component bonusLine = Component.translatable("rarity.arz_classes.bonus_line",
                        String.format("%.0f", rarity.statBonusPercent() * 100));
                lines.add(2, bonusLine); // сразу под звёздами
            }
        });
    }
}