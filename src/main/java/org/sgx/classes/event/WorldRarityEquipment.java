package org.sgx.classes.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.profession.RarityEligibility;
import org.sgx.classes.profession.WorldRarityRoller;

/** Редкость для снаряжения мобов (только подходящие предметы). Предмет с компонентом редкости повторно не бросается. */
public final class WorldRarityEquipment {

    private WorldRarityEquipment() {
    }

    public static void init() {
        ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
            if (!(entity instanceof Mob mob)) {
                return;
            }
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = mob.getItemBySlot(slot);
                if (RarityEligibility.isEligible(stack)) {
                    WorldRarityRoller.rollAndApply(stack, world.getRandom());
                }
            }
        });
    }
}