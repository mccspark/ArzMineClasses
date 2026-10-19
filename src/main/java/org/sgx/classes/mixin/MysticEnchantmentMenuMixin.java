package org.sgx.classes.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.event.ProfessionEvents;
import org.sgx.classes.profession.MysticEnchantBoost;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(EnchantmentMenu.class)
public abstract class MysticEnchantmentMenuMixin {

    @Unique private Player arzClasses$owner;
    @Unique private final int[] arzClasses$power = new int[3];
    @Unique private boolean arzClasses$thresholdActive;
    @Unique private int arzClasses$powerBefore;

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("TAIL"))
    private void arzClasses$captureOwner(int id, Inventory inventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.arzClasses$owner = inventory.player;
    }

    @Inject(method = "slotsChanged", at = @At("HEAD"))
    private void arzClasses$resetThreshold(Container container, CallbackInfo ci) {
        EnchantmentMenu menu = (EnchantmentMenu) (Object) this;
        if (container == menu.getSlot(0).container) {
            arzClasses$thresholdActive = false;
        }
    }

    /** Порог: узлы 1 и 8 снижают слоты на -1/-2/-3 за узел. Исходную силу сохраняем отдельно. */
    @Inject(method = "slotsChanged", at = @At("TAIL"))
    private void arzClasses$applyThreshold(Container container, CallbackInfo ci) {
        EnchantmentMenu menu = (EnchantmentMenu) (Object) this;
        if (container != menu.getSlot(0).container || !(arzClasses$owner instanceof ServerPlayer player)) {
            return;
        }
        ProfessionData data = ModAttachments.get(player);
        if (data.currentProfession() != ProfessionType.MYSTIC) {
            return;
        }
        int nodes = (int) data.customScalarTotal("mystic.enchant_threshold_reduction");
        if (nodes <= 0) {
            return;
        }
        for (int i = 0; i < menu.costs.length && i < arzClasses$power.length; i++) {
            arzClasses$power[i] = menu.costs[i];
            if (menu.costs[i] > 0) {
                menu.costs[i] = Math.max(1, menu.costs[i] - nodes * (i + 1));
            }
        }
        arzClasses$thresholdActive = true;
    }

    /** Сила чар — исходная (до снижения порога), затем буст уровней Мистика. */
    @WrapMethod(method = "getEnchantmentList")
    private List<EnchantmentInstance> arzClasses$wrapList(RegistryAccess access, ItemStack stack, int slot, int cost,
                                                          Operation<List<EnchantmentInstance>> original) {
        int power = cost;
        if (arzClasses$thresholdActive && slot >= 0 && slot < arzClasses$power.length
                && arzClasses$power[slot] > 0) {
            power = arzClasses$power[slot];
        }
        List<EnchantmentInstance> list = original.call(access, stack, slot, power);
        if (arzClasses$owner instanceof ServerPlayer player) {
            return MysticEnchantBoost.apply(list, ModAttachments.get(player));
        }
        return list;
    }

    /** Силу запоминаем до клика: после успеха ваниль обнуляет costs[]. */
    @Inject(method = "clickMenuButton", at = @At("HEAD"))
    private void arzClasses$captureCost(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        EnchantmentMenu self = (EnchantmentMenu) (Object) this;
        if (buttonId < 0 || buttonId >= self.costs.length) {
            arzClasses$powerBefore = 0;
            return;
        }
        arzClasses$powerBefore = (arzClasses$thresholdActive && buttonId < arzClasses$power.length)
                ? arzClasses$power[buttonId] : self.costs[buttonId];
    }

    @Inject(method = "clickMenuButton", at = @At("RETURN"))
    private void arzClasses$onEnchantApplied(Player player, int buttonId, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (arzClasses$powerBefore > 0) {
            ProfessionEvents.onEnchant(serverPlayer, arzClasses$powerBefore);
        }
    }
}