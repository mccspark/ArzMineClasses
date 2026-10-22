package org.sgx.classes.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.item.ModDataComponents;
import org.sgx.classes.network.OpenRarityWheelPayload;
import org.sgx.classes.profession.RarityCraftHandler;
import org.sgx.classes.profession.RepairMerge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** То же, что CraftingMenuMixin, но для крафта 2x2 в инвентаре игрока (слот результата — индекс 0). */
@Mixin(InventoryMenu.class)
public abstract class InventoryMenuMixin {

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void arzClasses$fixShiftCraftMerge(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (slotIndex != 0) {
            return;
        }
        Slot resultSlot = ((AbstractContainerMenu) (Object) this).getSlot(0);
        ItemStack resultStack = resultSlot.getItem();
        if (resultStack.isEmpty()) {
            return;
        }

        // Объединение — прямо в слоте результата, до перемещения стека и очистки сетки.
        RepairMerge.tryMerge(player, resultStack);

        if (player instanceof ServerPlayer serverPlayer && !resultStack.has(ModDataComponents.ITEM_RARITY)) {
            OpenRarityWheelPayload payload = RarityCraftHandler.handle(serverPlayer, resultStack);
            if (payload != null) {
                ItemRarity result = ItemRarity.values()[payload.resultOrdinal()];
                serverPlayer.sendSystemMessage(
                        Component.translatable("message.arz_classes.quick_craft_result",
                                        Component.translatable(result.translationKey()))
                                .withStyle(style -> style.withColor(result.argb())));
            }
        }
    }
}