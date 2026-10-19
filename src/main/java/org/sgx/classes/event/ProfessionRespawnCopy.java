package org.sgx.classes.event;

import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionData;

/**
 * Fabric Attachment API персистентность (.persistent(CODEC)) означает "сохраняется на
 * диск, переживает релогин" — но НЕ означает "переживает смерть в рамках сессии". При
 * возрождении движок создаёт НОВЫЙ инстанс ServerPlayer, старый уничтожается; attachment
 * привязан к конкретному инстансу сущности, поэтому initializer просто создаёт данные
 * с нуля для нового игрока. ServerPlayerEvents.COPY_FROM — штатная точка, где нужно
 * вручную перенести такие данные со старого игрока на нового (то же самое, что ванильный
 * код уже делает сам для инвентаря/опыта, но не делает для кастомных attachment'ов).
 */
public final class ProfessionRespawnCopy {

    private ProfessionRespawnCopy() {
    }

    public static void init() {
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            ProfessionData data = ModAttachments.get(oldPlayer);
            newPlayer.setAttached(ModAttachments.PROFESSION_DATA, data);
        });
    }
}