package org.sgx.classes.profession;

import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.item.ModDataComponents;

/** Единая точка "проставить редкость на стек" — использует и кузнец (RarityCraftHandler),
 * и мировой лут/экипировка мобов (WorldRarityLoot/WorldRarityEquipment). */
public final class RarityApplication {

    private RarityApplication() {
    }

    public static void applyRarity(ItemStack stack, ItemRarity rarity) {
        ItemRarity previous = stack.get(ModDataComponents.ITEM_RARITY);
        stack.set(ModDataComponents.ITEM_RARITY, rarity);

        // Свежая обычная вещь (из мира): нужна только метка «Обычное», пересчитывать нечего.
        if (rarity == ItemRarity.COMMON && previous == null) {
            return;
        }

        RarityStatApplier.apply(stack, rarity);
        RarityDurability.change(stack, previous, rarity);
    }
}