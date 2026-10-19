package org.sgx.classes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.enchantment.Enchantment;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AnvilMenu.class)
public abstract class MysticAnvilMixin {

    @Unique private Player arzClasses$owner;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("TAIL"))
    private void arzClasses$captureOwner(int id, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.arzClasses$owner = inventory.player;
    }

    @WrapOperation(method = "createResult",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"))
    private int arzClasses$raiseMaxLevel(Enchantment enchantment, Operation<Integer> original) {
        int max = original.call(enchantment);
        if (arzClasses$owner instanceof ServerPlayer player) {
            ProfessionData data = ModAttachments.get(player);
            if (data.currentProfession() == ProfessionType.MYSTIC && data.hasAbility("mystic_max_level")) {
                return max + 1;
            }
        }
        return max;
    }
}