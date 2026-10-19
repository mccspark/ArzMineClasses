package org.sgx.classes.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.client.gui.ClassesScreen;

public final class ModKeyBindings {

    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "main")
    );

    public static final KeyMapping OPEN_CLASSES = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.arz_classes.open_classes",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_K,
                    CATEGORY
            )
    );

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }

            // while + consumeClick, чтобы не терять быстрые нажатия
            while (OPEN_CLASSES.consumeClick()) {
                client.gui.setScreen(new ClassesScreen(null));
            }

        });
    }
}