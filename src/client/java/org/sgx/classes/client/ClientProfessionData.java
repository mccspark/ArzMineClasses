package org.sgx.classes.client;

import org.sgx.classes.profession.ProfessionData;

/**
 * Клиентский кэш ProfessionData локального игрока. Обновляется приёмником
 * SyncProfessionDataPayload (см. ProfessionClientNetworking). ClassesScreen читает
 * значения только отсюда — никогда напрямую из ModAttachments на клиенте.
 */
public final class ClientProfessionData {

    private static ProfessionData current = new ProfessionData();

    private ClientProfessionData() {
    }

    public static ProfessionData get() {
        return current;
    }

    public static void set(ProfessionData data) {
        current = data;
    }
}
