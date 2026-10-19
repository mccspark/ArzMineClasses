package org.sgx.classes.mixin;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import org.sgx.classes.event.ProfessionEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(targets = "net.minecraft.world.inventory.BrewingStandMenu$PotionSlot")
public abstract class BrewingStandMenuPotionSlotMixin {

    @Inject(method = "onTake", at = @At("TAIL"))
    private void arzClasses$onPotionTaken(Player player, ItemStack carried, CallbackInfo ci) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        PotionContents contents = carried.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        Optional<Holder<Potion>> potionHolder = contents.potion();
        if (potionHolder.isEmpty()) {
            return;
        }
        String path = potionHolder.get().unwrapKey()
                .map(key -> key.identifier().getPath())
                .orElse("");
        int tier = (path.startsWith("strong_") || path.startsWith("long_")) ? 1 : 0;
        ProfessionEvents.onPotionTaken(serverPlayer, tier);
    }
}