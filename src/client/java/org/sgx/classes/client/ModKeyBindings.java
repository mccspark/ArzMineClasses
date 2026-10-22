package org.sgx.classes.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.client.gui.ClassesScreen;
import org.sgx.classes.client.gui.ClassBookScreen;

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

    /** Открыть книгу класса (из слота Trinkets или инвентаря; ключ перевода прежний, чтобы не сбить настройку). Клавиша меняется в настройках управления. */
    public static final KeyMapping OPEN_BOOK = KeyMappingHelper.registerKeyMapping(
            new KeyMapping(
                    "key.arz_classes.open_harvest_book",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_H,
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

            while (OPEN_BOOK.consumeClick()) {
                ClassBookScreen.openFromKey(client);
            }

        });
    }
}