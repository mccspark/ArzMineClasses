package org.sgx.classes.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.item.ModDataComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(ItemStack.class)
public abstract class ItemRarityTooltipMixin {

    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true)
    private void arzClasses$colorByRarity(CallbackInfoReturnable<Component> cir) {
        ItemStack self = (ItemStack) (Object) this;
        ItemRarity rarity = self.get(ModDataComponents.ITEM_RARITY);
        if (rarity == null) {
            return;
        }

        Identifier itemId = BuiltInRegistries.ITEM.getKey(self.getItem());
        String gender = arzClasses$detectGender(itemId.getPath());

        Component prefix = Component.translatable(rarity.translationKey() + "." + gender);
        MutableComponent combined = prefix.copy()
                .append(Component.literal(" "))
                .append(cir.getReturnValue())
                .withStyle(style -> style.withColor(rarity.argb()));
        cir.setReturnValue(combined);
    }

    private static String arzClasses$detectGender(String path) {
        if (path.endsWith("_leggings") || path.endsWith("_boots") || path.endsWith("_tongs")) {
            return "plural"; // поножи, ботинки, клещи
        }
        if (path.endsWith("_pickaxe") || path.endsWith("_shovel") || path.endsWith("_hoe")) {
            return "fem"; // кирка, лопата, мотыга
        }
        if (path.endsWith("_spear")) {
            return "neut"; // копьё
        }
        // helmet, chestplate, sword, axe, shield, hammer и всё остальное — м.р.
        return "masc";
    }
}