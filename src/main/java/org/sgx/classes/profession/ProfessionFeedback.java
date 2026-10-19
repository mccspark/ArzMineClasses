package org.sgx.classes.profession;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerPlayer;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.network.ProfessionNetworking;
import org.sgx.classes.storage.PlayerDataStore;

public final class ProfessionFeedback {

    private ProfessionFeedback() {
    }

    public static void grantXp(ServerPlayer player, ProfessionType type, int amount) {
        ProfessionData data = ModAttachments.get(player);
        if (type != data.currentProfession()) {
            return;
        }

        // Перемножаем бонус от предметов (колец) и модификаторы команды (/profession globalXP / localXP)
        double totalMultiplier = ExperienceRingCheck.xpMultiplier(player) * XPModifiers.multiplierFor(player.getUUID());
        int bonusedAmount = (int) Math.round(amount * totalMultiplier);

        // Если итоговый XP равен 0, можно либо пропустить, либо начислить 0
        if (bonusedAmount <= 0 && amount > 0 && totalMultiplier > 0) {
            bonusedAmount = 1; // Минимальный гарантированный опыт, если множитель положительный
        }

        boolean leveledUp = data.addXp(bonusedAmount);

        Component message = leveledUp
                ? Component.translatable("message.arz_classes.level_up", type.displayName(), data.level())
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                : Component.translatable("message.arz_classes.xp_gain", bonusedAmount)
                .withStyle(ChatFormatting.AQUA);

        player.connection.send(new ClientboundSetActionBarTextPacket(message));
        ProfessionNetworking.syncToClient(player);
        PlayerDataStore.save(player);
    }
}