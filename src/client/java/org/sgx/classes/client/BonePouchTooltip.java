package org.sgx.classes.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.network.chat.Component;
import org.sgx.classes.item.BonePouchItem;

public final class BonePouchTooltip {

    private BonePouchTooltip() {
    }

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!(stack.getItem() instanceof BonePouchItem)) {
                return;
            }
            int stored = BonePouchItem.storedAmount(stack);
            lines.add(Component.translatable("tooltip.arz_classes.bone_pouch_content",
                            stored, BonePouchItem.MAX_CONTENT)
                    .withStyle(style -> style.withColor(0xFF55FF55)));
        });
    }
}