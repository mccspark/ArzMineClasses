package org.sgx.classes.event;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.ModItems;
import org.sgx.classes.profession.RepairMaterials;

public final class HammerArmorGuard {

    private HammerArmorGuard() {
    }

    public static void init() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            boolean isHammer = offHand.is(ModItems.BLACKSMITH_HAMMER) || offHand.is(ModItems.COPPER_HAMMER);
            boolean isRepairMaterial = !offHand.isEmpty() && RepairMaterials.categorize(offHand.getItem()) != null;
            boolean isEquippable = mainHand.get(DataComponents.EQUIPPABLE) != null;
            if ((isHammer || isRepairMaterial) && isEquippable) {
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }
}