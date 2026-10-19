package org.sgx.classes.event;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import org.sgx.classes.profession.RarityEligibility;
import org.sgx.classes.profession.WorldRarityRoller;

/** Редкость для предметов из лута (только броня/оружие/инструменты/луки/щиты и т.п.). Обычная (COMMON) ничего не навешивает. */
public final class WorldRarityLoot {

    private WorldRarityLoot() {
    }

    public static void init() {
        LootTableEvents.MODIFY_DROPS.register((holder, context, drops) -> {
            for (var stack : drops) {
                if (RarityEligibility.isEligible(stack)) {
                    WorldRarityRoller.rollAndApply(stack, context.getRandom());
                }
            }
        });
    }
}