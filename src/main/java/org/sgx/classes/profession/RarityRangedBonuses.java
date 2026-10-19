package org.sgx.classes.profession;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.item.ModDataComponents;


public final class RarityRangedBonuses {

    private static final double ARROW_BASE_DAMAGE = 2.0;

    private RarityRangedBonuses() {
    }


    /** Бонус редкости этого предмета (0, если редкости нет). */
    public static double bonusFor(ItemStack stack) {
        ItemRarity rarity = stack.get(ModDataComponents.ITEM_RARITY);
        return rarity == null ? 0.0 : Math.max(0.0, rarity.statBonusPercent());
    }

    /** Вызвать один раз из Arz_classes.onInitialize(). */
    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            // tickCount > 0 — стрела подгружается с чанка, а не только что выпущена: второй раз бонус не применяем
            if (!(entity instanceof AbstractArrow arrow) || arrow.tickCount > 0) {
                return;
            }
            ItemStack weapon = arrow.getWeaponItem();
            if (weapon == null || weapon.isEmpty()
                    || !(RarityEligibility.isBow(weapon) || RarityEligibility.isCrossbow(weapon))) {
                return;
            }
            double bonus = bonusFor(weapon);
            if (bonus > 0) {
                arrow.setBaseDamage(ARROW_BASE_DAMAGE * (1.0 + bonus));
            }
        });
    }
}