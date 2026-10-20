package org.sgx.classes.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.sgx.classes.item.HarvestBookItem;
import org.sgx.classes.item.ModDataComponents;

public final class HarvestBookTooltip {

    private HarvestBookTooltip() {
    }

    public static void init() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            if (!(stack.getItem() instanceof HarvestBookItem)) {
                return;
            }
            String owner = stack.get(ModDataComponents.HARVEST_OWNER_NAME);
            if (owner != null) {
                lines.add(Component.translatable("tooltip.arz_classes.harvest_book_owner", owner)
                        .withStyle(ChatFormatting.GREEN));
            } else {
                lines.add(Component.translatable("tooltip.arz_classes.harvest_book_unbound")
                        .withStyle(ChatFormatting.GRAY));
            }
        });
    }
}
