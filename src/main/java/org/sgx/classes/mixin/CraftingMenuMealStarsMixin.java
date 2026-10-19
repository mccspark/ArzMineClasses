package org.sgx.classes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.item.ModDataComponents;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.SubclassType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMealStarsMixin {

    @Shadow
    public abstract Slot getResultSlot();

    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void arzClasses$stampMealStarsBeforeQuickCraft(Player player, int slotIndex, CallbackInfoReturnable<ItemStack> cir) {
        if (slotIndex != 0) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        Slot resultSlot = this.getResultSlot();
        ItemStack resultStack = resultSlot.getItem();
        if (resultStack.isEmpty() || !resultStack.has(DataComponents.FOOD)) {
            return;
        }
        if (resultStack.has(ModDataComponents.MEAL_STARS)) {
            return;
        }
        ProfessionData data = ModAttachments.get(serverPlayer);
        if (data.currentSubclass() != SubclassType.COOK) {
            return;
        }
        int stars = Math.min(5, (int) data.customScalarTotal("cook.quality_star_count"));
        if (stars <= 0) {
            return;
        }
        resultStack.set(ModDataComponents.MEAL_STARS, stars);
    }
}