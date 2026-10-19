package org.sgx.classes.profession;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Repairable;
import org.sgx.classes.item.ModItems;

/**
 * Определяет, каким материалом можно чинить предмет (SharpeningTrigger) и к какой
 * категории он относится — категория решает процент восстановления в ItemRepair.
 *
 * Соответствие тиров:
 *   copper:    PIECE = copper_nugget,   FULL = copper_ingot
 *   iron:      PIECE = iron_nugget,     FULL = iron_ingot
 *   gold:      PIECE = gold_nugget,     FULL = gold_ingot
 *   diamond:   PIECE = diamond_shard,   FULL = diamond (сам самоцвет)
 *   netherite: PIECE = netherite_scrap. Слитком (netherite_ingot) НЕ чинится вообще —
 *              по требованию: незерит только скрапом.
 *   trident:   PIECE = prismarine_shard (только трезубец)
 */
public final class RepairMaterials {

    public enum Tier { PIECE, FULL }

    private RepairMaterials() {
    }

    public static Tier categorize(Item material) {
        if (material == Items.COPPER_NUGGET
                || material == Items.IRON_NUGGET || material == Items.GOLD_NUGGET
                || material == ModItems.DIAMOND_SHARD || material == Items.NETHERITE_SCRAP
                || material == Items.STICK || material == Items.PRISMARINE_SHARD) {
            return Tier.PIECE;
        }
        return Tier.FULL;
    }

    public static boolean isValid(ItemStack toRepair, ItemStack material) {
        if (material.isEmpty()) {
            return false;
        }
        Item item = material.getItem();

        if (item == Items.STICK) {
            return toRepair.is(Items.BOW) || toRepair.is(Items.CROSSBOW);
        }
        if (item == Items.PRISMARINE_SHARD) {
            return toRepair.is(Items.TRIDENT);
        }
        if (item == Items.NETHERITE_SCRAP) {
            return isTier(toRepair, "netherite_");
        }
        if (item == ModItems.DIAMOND_SHARD || item == Items.DIAMOND) {
            return isTier(toRepair, "diamond_");
        }
        if (item == Items.COPPER_NUGGET || item == Items.COPPER_INGOT) {
            return isTier(toRepair, "copper_");
        }
        if (item == Items.IRON_NUGGET || item == Items.IRON_INGOT) {
            return isTier(toRepair, "iron_");
        }
        if (item == Items.GOLD_NUGGET || item == Items.GOLD_INGOT) {
            return isTier(toRepair, "golden_", "gold_");
        }
        if (isTier(toRepair, "netherite_")) {
            return false;
        }

        Repairable repairable = toRepair.get(DataComponents.REPAIRABLE);
        return repairable != null && repairable.isValidRepairItem(material);
    }

    private static boolean isTier(ItemStack stack, String... prefixes) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (String prefix : prefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }
}