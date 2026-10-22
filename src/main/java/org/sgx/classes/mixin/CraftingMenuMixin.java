package org.sgx.classes.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.item.ModDataComponents;
import org.sgx.classes.network.OpenRarityWheelPayload;
import org.sgx.classes.profession.RarityCraftHandler;
import org.sgx.classes.profession.RepairMerge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Shadow
    public abstract Slot getResultSlot();

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void arzClasses$fixShiftCraftMerge(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (slotIndex != 0) {
            return;
        }
        Slot resultSlot = this.getResultSlot();
        if (resultSlot == null) {
            return;
        }

        ItemStack resultStack = resultSlot.getItem();
        if (resultStack.isEmpty()) {
            return;
        }

        // КРИТИЧЕСКИ ВАЖНО: Применяем RepairMerge прямо в слоте результата
        // ДО того, как движок игры начнет перемещать предмет или очищать сетку крафта.
        // Это гарантирует сохранение прочности, стат и заточки.
        RepairMerge.tryMerge(player, resultStack);

        // Обработка редкости для сервера
        if (player instanceof ServerPlayer serverPlayer) {
            if (!resultStack.has(ModDataComponents.ITEM_RARITY)) {
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
}