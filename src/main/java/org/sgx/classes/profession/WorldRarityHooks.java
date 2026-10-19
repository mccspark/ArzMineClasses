package org.sgx.classes.profession;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;

/** Редкость снаряжения враждебных мобов (броня и предметы в руках). */
public final class WorldRarityHooks {

    private static final String ROLLED_TAG = "arz_rarity_rolled";

    private WorldRarityHooks() {
    }

    /** Вызвать один раз из Arz_classes.onInitialize(). */
    public static void init() {
        // Событие срабатывает после полной выдачи снаряжения; метка не даёт бросать заново при загрузке чанка.
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (!(entity instanceof Mob mob)) {
                return;
            }
            if (!(mob instanceof Enemy)) {
                return;
            }
            if (!mob.addTag(ROLLED_TAG)) {
                return; // метка уже стояла: снаряжению этого моба редкость уже бросали
            }
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = mob.getItemBySlot(slot);
                WorldRarityRoller.rollAndApply(stack, mob.getRandom());
            }
        });
    }
}