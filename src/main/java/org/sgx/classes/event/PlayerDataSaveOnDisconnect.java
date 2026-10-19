package org.sgx.classes.event;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.sgx.classes.storage.PlayerDataStore;

/** Подстраховка: если какой-то источник изменений забыл вызвать PlayerDataStore.save()
 * явно, снимок всё равно сохранится в момент выхода игрока с сервера. */
public final class PlayerDataSaveOnDisconnect {

    private PlayerDataSaveOnDisconnect() {
    }

    public static void init() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                PlayerDataStore.save(handler.getPlayer()));
    }
}