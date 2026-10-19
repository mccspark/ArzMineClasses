package org.sgx.classes.client;

import org.sgx.classes.network.OpenRarityWheelPayload;

/** Очередь на одно значение: пришедший от сервера payload колеса ждёт, пока игрок
 * сам закроет текущий экран (крафт-стол), и только тогда открывается — чтобы не
 * прерывать штатный возврат предмета с курсора при резкой смене экрана. */
public final class PendingRarityWheel {
    private static OpenRarityWheelPayload pending;

    private PendingRarityWheel() {
    }

    public static void queue(OpenRarityWheelPayload payload) {
        pending = payload;
    }

    public static OpenRarityWheelPayload takeIfReady(boolean screenIsNull) {
        if (pending != null && screenIsNull) {
            OpenRarityWheelPayload p = pending;
            pending = null;
            return p;
        }
        return null;
    }
}