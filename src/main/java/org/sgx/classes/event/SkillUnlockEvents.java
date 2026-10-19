package org.sgx.classes.event;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
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

    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(UnlockSkillPayload.TYPE, UnlockSkillPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(UnlockSkillPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    ProfessionData data = ModAttachments.get(player);
                    if (data.unlockNextSkill()) {
                        SkillAttributeSync.reapply(player, data);
                        ProfessionNetworking.syncToClient(player);
                    }
                }));
    }
}