package org.sgx.classes.profession;

import eu.pb4.trinkets.api.TrinketAttachment;
import eu.pb4.trinkets.api.TrinketsApi;
import net.minecraft.server.level.ServerPlayer;
import org.sgx.classes.item.ExperienceRingItem;
import org.sgx.classes.item.ModItems;

public final class ExperienceRingCheck {

    private ExperienceRingCheck() {
    }

    /** Множитель опыта класса с учётом лучшего надетого кольца (не складываются между
     * собой, если вдруг надето несколько — берётся наибольший бонус). 1.0 без колец. */
    public static double xpMultiplier(ServerPlayer player) {
        TrinketAttachment attachment = TrinketsApi.getAttachment(player);

        double best = 1.0;
        if (attachment.isEquipped(ModItems.RING_NETHERITE)) {
            best = Math.max(best, 1.0 + ExperienceRingItem.NETHERITE_XP_BONUS_MULTIPLIER);
        }
        if (attachment.isEquipped(ModItems.RING_GOLD)) {
            best = Math.max(best, 1.0 + ExperienceRingItem.GOLD_XP_BONUS_MULTIPLIER);
        }
        if (attachment.isEquipped(ModItems.RING_IRON)) {
            best = Math.max(best, 1.0 + ExperienceRingItem.IRON_XP_BONUS_MULTIPLIER);
        }
        return best;
    }
}