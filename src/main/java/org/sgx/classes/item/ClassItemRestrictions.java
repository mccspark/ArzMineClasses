package org.sgx.classes.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Предметы, которые можно скрафтить только игроку с нужным классом/навыком (см. CraftingResultSlotMixin).
 * Автокрафтер (Crafter) игрока не имеет, поэтому проверить класс он не может — для него все эти
 * предметы запрещены целиком (см. CrafterBlockMixin).
 */
public final class ClassItemRestrictions {

    private ClassItemRestrictions() {
    }

    public static boolean isClassCraftOnly(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return stack.is(ModItems.BLACKSMITH_HAMMER)
                || stack.is(ModItems.COPPER_HAMMER)
                || stack.is(ModItems.BONE_POUCH)
                || stack.is(ModItems.FOOD_POUCH)
                || stack.is(ModItems.FIREWOOD)
                || stack.is(ModItems.MINER_HELMET)
                || stack.is(ModItems.HELMET_BULB)
                || stack.is(ModItems.RING_IRON)
                || stack.is(ModItems.RING_GOLD)
                || stack.is(ModItems.RING_NETHERITE)
                || stack.is(Items.ENCHANTING_TABLE)
                || stack.getItem() instanceof ClassBookItem
                || isCarpenterWorkbench(stack);
    }

    private static boolean isCarpenterWorkbench(ItemStack stack) {
        var id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("arz_classes") && id.getPath().equals("carpenter_workbench");
    }
}
