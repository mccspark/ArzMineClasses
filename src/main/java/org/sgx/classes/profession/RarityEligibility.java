package org.sgx.classes.profession;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import org.sgx.classes.item.ModItems;

/**
 * Единое место: какие предметы вообще могут получить редкость.
 * Броня (4 слота, без элитр), оружие, инструменты, лук, арбалет, щит, удочка, молоты, каска шахтёра.
 * Всё остальное с прочностью (ножницы, огниво, щётка, элитры и т.п.) редкости не получает.
 */
public final class RarityEligibility {

    private RarityEligibility() {
    }

    public static boolean isEligible(ItemStack stack) {
        return !stack.isEmpty() && stack.getMaxDamage() > 0
                && (isArmor(stack) || isWeapon(stack) || isTool(stack) || isBow(stack) || isCrossbow(stack)
                || isShield(stack) || isFishingRod(stack) || isHammer(stack));
    }

    /** Броня на 4 слота (шлем/нагрудник/штаны/ботинки), включая каску шахтёра. Элитры не считаются. */
    public static boolean isArmor(ItemStack stack) {
        if (stack.is(Items.ELYTRA)) {
            return false;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable == null) {
            return false;
        }
        EquipmentSlot slot = equippable.slot();
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }

    public static boolean isWeapon(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES)
                || stack.is(Items.TRIDENT) || stack.is(Items.MACE) || isSpear(stack) || isKnife(stack);
    }
    public static boolean isKnife(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_knife");
    }

    public static boolean isTool(ItemStack stack) {
        return stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES) || stack.is(ItemTags.AXES);
    }

    public static boolean isSpear(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_spear");
    }

    public static boolean isBow(ItemStack stack) {
        return stack.is(Items.BOW);
    }

    public static boolean isCrossbow(ItemStack stack) {
        return stack.is(Items.CROSSBOW);
    }

    public static boolean isShield(ItemStack stack) {
        return stack.is(Items.SHIELD);
    }

    public static boolean isFishingRod(ItemStack stack) {
        return stack.is(Items.FISHING_ROD);
    }

    public static boolean isHammer(ItemStack stack) {
        return stack.is(ModItems.BLACKSMITH_HAMMER) || stack.is(ModItems.COPPER_HAMMER);
    }

    /** Деревянные и каменные предметы (wooden_*, stone_*): кирки, топоры, лопаты, мотыги, мечи, копья. */
    public static boolean isWoodOrStoneTier(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.startsWith("wooden_") || path.startsWith("stone_");
    }
}