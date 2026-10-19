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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Шифт-клик (quickMoveStack) для result-слота идёт ДРУГИМ путём, чем обычный клик:
 * moveItemStackTo() дробит и переносит стек в инвентарь ДО вызова slot.onTake() (тот
 * зовётся последним, когда исходный стек уже почти пуст). Фикс: бросаем редкость ЗДЕСЬ,
 * на HEAD, ДО того как метод начнёт двигать стек.
 *
 * ВАЖНО: @Shadow поле resultSlots не работает — оно объявлено в AbstractCraftingMenu,
 * а @Shadow ищет только в самом target-классе (CraftingMenu). Используем вместо этого
 * публичный getResultSlot(), объявленный прямо в CraftingMenu.
 */
@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {

    @Shadow
    public abstract Slot getResultSlot();

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void arzClasses$rollRarityBeforeQuickCraft(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (slotIndex != 0) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Slot resultSlot = this.getResultSlot();
        ItemStack resultStack = resultSlot.getItem();
        if (resultStack.isEmpty() || resultStack.has(ModDataComponents.ITEM_RARITY)) {
            return; // пусто, или уже брошено (защита от повторного вызова в цикле quick-move)
        }

        OpenRarityWheelPayload payload = RarityCraftHandler.handle(serverPlayer, resultStack);
        if (payload == null) {
            return;
        }

        ItemRarity result = ItemRarity.values()[payload.resultOrdinal()];
        serverPlayer.sendSystemMessage(
                Component.translatable("message.arz_classes.quick_craft_result",
                                Component.translatable(result.translationKey()))
                        .withStyle(style -> style.withColor(result.argb())));
    }
}