package org.sgx.classes.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.network.OpenRarityWheelPayload;
import org.sgx.classes.profession.RarityCraftHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {

    @Unique
    private OpenRarityWheelPayload arzClasses$pendingWheelPayload;

    @Inject(method = "onTake", at = @At("HEAD"))
    private void arzClasses$rollRarityOnResultTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (player instanceof ServerPlayer serverPlayer) {
            // Обычный клик: стек ещё полный на этот момент (в отличие от шифт-пути,
            // см. CraftingMenuMixin), поэтому тут бросок отрабатывает как и раньше.
            this.arzClasses$pendingWheelPayload = RarityCraftHandler.handle(serverPlayer, stack);
        }
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void arzClasses$openWheelAfterTake(Player player, ItemStack stack, CallbackInfo ci) {
        OpenRarityWheelPayload payload = this.arzClasses$pendingWheelPayload;
        this.arzClasses$pendingWheelPayload = null;
        if (payload != null && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.closeContainer(); // ингредиенты уже списаны выше — вернётся только неиспользованный остаток
            ServerPlayNetworking.send(serverPlayer, payload);
        }
    }
}