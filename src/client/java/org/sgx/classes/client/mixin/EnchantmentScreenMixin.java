package org.sgx.classes.client.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.gui.screens.inventory.EnchantmentScreen;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import org.sgx.classes.client.ClientProfessionData;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.SubclassType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

@Mixin(EnchantmentScreen.class)
public abstract class EnchantmentScreenMixin extends AbstractContainerScreen<EnchantmentMenu> {

    protected EnchantmentScreenMixin(EnchantmentMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    // --- Рунический текст в слоте -> настоящее название. ТОЛЬКО Зачарователь. ---
    @WrapOperation(method = "extractBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/EnchantmentNames;getRandomName(Lnet/minecraft/client/gui/Font;I)Lnet/minecraft/network/chat/FormattedText;"))
    private FormattedText arzClasses$revealRealName(EnchantmentNames names, Font font, int width,
                                                    Operation<FormattedText> original,
                                                    @Local(name = "i") int i) {
        ProfessionData data = ClientProfessionData.get();
        if (data.currentSubclass() == SubclassType.ENCHANTER) {
            EnchantmentMenu menu = this.getMenu();
            if (Minecraft.getInstance().level != null) {
                Optional<Holder.Reference<Enchantment>> enchant = Minecraft.getInstance().level.registryAccess()
                        .lookupOrThrow(Registries.ENCHANTMENT).get(menu.enchantClue[i]);
                if (enchant.isPresent() && menu.levelClue[i] >= 0) {
                    return Enchantment.getFullname(enchant.get(), menu.levelClue[i]);
                }
            }
        }
        return original.call(names, font, width);
    }
}