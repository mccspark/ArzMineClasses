package org.sgx.classes.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.sgx.classes.client.ClientClassStats;
import org.sgx.classes.client.ClientProfessionData;
import org.sgx.classes.network.ClassStatsPayload;
import org.sgx.classes.network.ProfessionNetworking.SelectProfessionPayload;
import org.sgx.classes.network.ProfessionNetworking.SelectSubclassPayload;
import org.sgx.classes.network.ProfessionNetworking.SyncProfessionDataPayload;
import org.sgx.classes.profession.ProfessionType;
import org.sgx.classes.profession.SubclassType;

/**
 * Клиентская сторона сети класса. Только этот файл (и всё что из него вызывается)
 * трогает ClientPlayNetworking — держите ссылки на него подальше от common-кода,
 * иначе на dedicated server вылетит NoClassDefFoundError на клиентские классы MC.
 *
 * НОВОЕ: приём OpenRarityWheelPayload — открывает RarityWheelScreen с уже готовым
 * результатом (см. RarityCraftHandler на сервере). Открывается поверх текущего экрана
 * (обычно InventoryScreen со смиттинг-столом) без закрытия того, что было открыто —
 * колесо просто ложится сверху и закрывается само по завершении/пропуске анимации.
 */
public final class ProfessionClientNetworking {

    private ProfessionClientNetworking() {
    }

    /** Вызвать один раз из Arz_classesClient#onInitializeClient. */
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(SyncProfessionDataPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientProfessionData.set(payload.data())));
        ClientPlayNetworking.registerGlobalReceiver(ClassStatsPayload.TYPE, (payload, context) ->
                context.client().execute(() -> ClientClassStats.set(payload.counts())));

    }

    public static void requestSelectProfession(ProfessionType type) {
        ClientPlayNetworking.send(new SelectProfessionPayload(type));
    }

    public static void requestSelectSubclass(SubclassType type) {
        ClientPlayNetworking.send(new SelectSubclassPayload(type));
    }
}