package org.sgx.classes.client;

import org.sgx.classes.network.OpenRarityUpgradeWheelPayload;

public final class PendingRarityUpgradeWheel {

    private static OpenRarityUpgradeWheelPayload pending;

    private PendingRarityUpgradeWheel() {
    }

    public static void queue(OpenRarityUpgradeWheelPayload payload) {
        pending = payload;
    }

    public static OpenRarityUpgradeWheelPayload takeIfReady(boolean screenIsNull) {
        if (pending != null && screenIsNull) {
            OpenRarityUpgradeWheelPayload result = pending;
            pending = null;
            return result;
        }
        return null;
    }
}