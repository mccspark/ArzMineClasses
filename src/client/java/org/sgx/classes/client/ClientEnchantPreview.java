package org.sgx.classes.client;

import org.sgx.classes.network.EnchantFullPreviewPayload;

import java.util.List;

public final class ClientEnchantPreview {
    private static List<EnchantFullPreviewPayload.EnchantSlotPreview> current = List.of();

    private ClientEnchantPreview() {
    }

    public static void set(List<EnchantFullPreviewPayload.EnchantSlotPreview> slots) {
        current = slots;
    }

    public static List<EnchantFullPreviewPayload.EnchantSlotPreview> get() {
        return current;
    }
}