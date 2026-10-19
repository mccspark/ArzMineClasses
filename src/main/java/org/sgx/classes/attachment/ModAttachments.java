package org.sgx.classes.attachment;

import org.sgx.classes.Arz_classes;
import org.sgx.classes.profession.ProfessionData;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

/**
 * Fabric Attachment API (fabric-data-attachment-api-v1, часть Fabric API 0.155.0+26.2).
 * Заменяет ручную работу с NBT/капабилити-паттернами: данные переживают релогин
 * и сериализуются автоматически через Codec.
 */
public final class ModAttachments {

    public static final AttachmentType<ProfessionData> PROFESSION_DATA = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "profession_data"),
            builder -> builder
                    .persistent(ProfessionData.CODEC)
                    .initializer(ProfessionData::new)
    );
    
    private ModAttachments() {
    }

    public static ProfessionData get(Player player) {
        // initializer гарантирует, что значение никогда не будет null
        return player.getAttachedOrCreate(PROFESSION_DATA);
    }

    public static void init() {
        // класс-загрузчик: обращение к полям форсирует статическую инициализацию
    }
}