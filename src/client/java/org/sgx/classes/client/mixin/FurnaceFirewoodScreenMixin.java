package org.sgx.classes.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.sgx.classes.item.FirewoodSlot;
import org.sgx.classes.item.FurnaceFirewood;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * КЛИЕНТСКИЙ миксин (секция "client" в mixins.json).
 * Перед отрисовкой слота firewood рисует его рамку и над ним полоску использований текущего бревна.
 * Координаты slot.x / slot.y уже локальные для окна (сдвиг leftPos/topPos применён до вызова),
 * поэтому всё встаёт на место, в том числе при открытой книге рецептов.
 */
@Mixin(AbstractContainerScreen.class)
public abstract class FurnaceFirewoodScreenMixin {

    private static final int BAR_FILLED = 0xFFE8A23A;
    private static final int BAR_EMPTY = 0xFF555555;
    private static final int BAR_FRAME = 0xFF2B2B2B;

    @Inject(method = "extractSlot", at = @At("HEAD"))
    private void arzClasses$drawFirewoodSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY,
                                             CallbackInfo ci) {
        if (!(slot instanceof FirewoodSlot)) {
            return;
        }
        int x = slot.x - 1;
        int y = slot.y - 1;

        // рамка слота в стиле ванильных
        graphics.fill(x, y, x + 18, y + 18, 0xFF8B8B8B);
        graphics.fill(x, y, x + 17, y + 1, 0xFF373737);
        graphics.fill(x, y, x + 1, y + 17, 0xFF373737);
        graphics.fill(x + 1, y + 17, x + 18, y + 18, 0xFFFFFFFF);
        graphics.fill(x + 17, y + 1, x + 18, y + 18, 0xFFFFFFFF);

        // полоска использований текущего бревна над слотом
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (screen.getMenu() instanceof FurnaceFirewood.MenuAccess access) {
            int charges = access.arzClasses$firewoodData().get(0);
            int segments = FurnaceFirewood.SMELTS_PER_LOG;
            int segW = Math.max(2, 16 / segments);
            int barW = segW * segments + 2;
            int barX = x + (18 - barW) / 2;
            int barY = y - 6;
            graphics.fill(barX, barY, barX + barW, barY + 4, BAR_FRAME);
            for (int i = 0; i < segments; i++) {
                int sx = barX + 1 + i * segW;
                graphics.fill(sx, barY + 1, sx + segW - 1, barY + 3, i < charges ? BAR_FILLED : BAR_EMPTY);
            }
        }
    }
}