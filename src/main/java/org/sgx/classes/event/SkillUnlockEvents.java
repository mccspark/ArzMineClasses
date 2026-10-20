package org.sgx.classes.event;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.network.ProfessionNetworking;
import org.sgx.classes.network.UnlockSkillPayload;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.skill.SkillAttributeSync;

/**
 * Клик по следующему узлу дерева -> тратит очко навыка (см. ProfessionData.unlockNextSkill()).
 * Регистрирует свой payload сама — если у вас есть единая точка регистрации в главном
 * классе, перенесите PayloadTypeRegistry-вызов туда и уберите отсюда, чтобы не дублировать.
 */
public final class SkillUnlockEvents {

    private SkillUnlockEvents() {
    }

    /** Звук и «вспышка» частиц вокруг игрока при открытии нового узла навыка. */
    private static void playUnlockEffects(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6f, 1.3f);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 1.2f);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 30, 0.35, 0.6, 0.35, 0.08);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, y, z, 14, 0.4, 0.5, 0.4, 0.3);
    }

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(UnlockSkillPayload.TYPE, UnlockSkillPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(UnlockSkillPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    ProfessionData data = ModAttachments.get(player);
                    if (data.unlockNextSkill()) {
                        playUnlockEffects(player);
                        SkillAttributeSync.reapply(player, data);
                        ProfessionNetworking.syncToClient(player);
                    }
                }));
    }
}