package org.sgx.classes.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.sgx.classes.network.UnlockSkillPayload;

public final class SkillUnlockClientNetworking {

    private SkillUnlockClientNetworking() {
    }

    public static void requestUnlockNext() {
        ClientPlayNetworking.send(new UnlockSkillPayload());
    }
}