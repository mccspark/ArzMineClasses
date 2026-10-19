package org.sgx.classes.network;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.sgx.classes.Arz_classes;

import java.util.List;

public record EnchantFullPreviewPayload(List<EnchantSlotPreview> slots) implements CustomPacketPayload {

    public record EnchantEntry(Identifier enchantmentId, int level) {
        public static final StreamCodec<ByteBuf, EnchantEntry> CODEC = StreamCodec.composite(
                Identifier.STREAM_CODEC, EnchantEntry::enchantmentId,
                ByteBufCodecs.VAR_INT, EnchantEntry::level,
                EnchantEntry::new);
    }

    public record EnchantSlotPreview(List<EnchantEntry> entries) {
        public static final StreamCodec<ByteBuf, EnchantSlotPreview> CODEC =
                EnchantEntry.CODEC.apply(ByteBufCodecs.list()).map(EnchantSlotPreview::new, EnchantSlotPreview::entries);
    }

    public static final Type<EnchantFullPreviewPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "enchant_full_preview"));

    public static final StreamCodec<ByteBuf, EnchantFullPreviewPayload> CODEC =
            EnchantSlotPreview.CODEC.apply(ByteBufCodecs.list()).map(EnchantFullPreviewPayload::new, EnchantFullPreviewPayload::slots);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void registerCommon() {
        PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
    }
}