package org.sgx.classes.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.sgx.classes.item.BonePouchItem;

public final class BonePouchHint {

    private BonePouchHint() {
    }

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (stack.getItem() instanceof BonePouchItem) {
                lines.add(Component.translatable("tooltip.arz_classes.bone_pouch_open")
                        .withStyle(ChatFormatting.GRAY));
            }
        });
    }
}