package org.sgx.classes.profession;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.item.ModDataComponents;
import org.sgx.classes.item.ModItems;

public final class RarityUpgradeHandler {

    private RarityUpgradeHandler() {
    }

    public record Result(boolean canAttempt, ItemRarity target, double chance) {
        public static final Result NONE = new Result(false, null, 0);
    }

    public static Result check(ItemStack itemStack, ItemStack shardStack) {
        if (itemStack.isEmpty() || shardStack.isEmpty()) {
            return Result.NONE;
        }
        // Улучшать можно только то, что вообще получает редкость (или уже имеет её — старые предметы остаются как есть)
        if (!RarityEligibility.isEligible(itemStack) && !itemStack.has(ModDataComponents.ITEM_RARITY)) {
            return Result.NONE;
        }
        ItemRarity current = itemStack.getOrDefault(ModDataComponents.ITEM_RARITY, ItemRarity.COMMON);
        ItemRarity target = RarityUpgradeChances.next(current);
        if (target == null) {
            return Result.NONE; // уже MYTHIC
        }
        Item expectedShard = ModItems.RARITY_SHARDS.get(target);
        if (expectedShard == null || !shardStack.is(expectedShard)) {
            return Result.NONE; // не тот осколок для этого шага
        }
        return new Result(true, target, RarityUpgradeChances.successChance(target));
    }

    /** Бросок и применение результата. Осколок должен быть списан ВЫЗЫВАЮЩИМ кодом отдельно —
     * тут только сам предмет. Возвращает true при успехе. */
    public static boolean attempt(ItemStack itemStack, ItemRarity target, double chance) {
        boolean success = Math.random() < chance;
        if (success) {
            ItemRarity previous = itemStack.getOrDefault(ModDataComponents.ITEM_RARITY, ItemRarity.COMMON);
            itemStack.set(ModDataComponents.ITEM_RARITY, target);
            RarityStatApplier.apply(itemStack, target);
            RarityDurability.change(itemStack, previous, target); // прочность растёт вместе с редкостью
        } else {
            int maxDamage = itemStack.getMaxDamage();
            if (maxDamage > 0) {
                int loss = Math.max(1, maxDamage / 3);
                int newDamage = itemStack.getDamageValue() + loss;
                if (newDamage >= maxDamage) {
                    itemStack.shrink(1); // предмет разрушается
                } else {
                    itemStack.setDamageValue(newDamage);
                }
            }
        }
        return success;
    }
}