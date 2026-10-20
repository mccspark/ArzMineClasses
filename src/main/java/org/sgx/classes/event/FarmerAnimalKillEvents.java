package org.sgx.classes.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionBehavior;
import org.sgx.classes.profession.ProfessionFeedback;
import org.sgx.classes.profession.ProfessionType;

import java.util.EnumMap;
import java.util.Map;

public final class FarmerAnimalKillEvents {

    private static final Map<ProfessionType, ProfessionBehavior> BEHAVIORS = new EnumMap<>(ProfessionType.class);

    private FarmerAnimalKillEvents() {
    }

    public static void registerBehavior(ProfessionBehavior behavior) {
        BEHAVIORS.put(behavior.type(), behavior);
    }

    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof Animal animal)) {
                return;
            }
            if (!(damageSource.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            // книга урожая считает убийства мирных мобов независимо от текущего класса
            org.sgx.classes.item.HarvestBookItem.recordKill(player, animal.getType());
            ProfessionType active = ModAttachments.get(player).currentProfession();
            ProfessionBehavior behavior = BEHAVIORS.get(active);
            if (behavior != null) {
                int xp = behavior.onPassiveMobKilled(player, (ServerLevel) animal.level(), animal);
                if (xp > 0) {
                    ProfessionFeedback.grantXp(player, active, xp);
                }
            }
        });
    }
}