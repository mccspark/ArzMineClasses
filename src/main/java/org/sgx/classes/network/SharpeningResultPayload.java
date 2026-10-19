package org.sgx.classes.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.sgx.classes.Arz_classes;

/** Теперь несёт не boolean, а конкретную зону — MISS/YELLOW/GREEN/BRIGHT_GREEN. */
public record SharpeningResultPayload(SharpeningZone zone) implements CustomPacketPayload {

    private static final SharpeningZone[] ZONES = SharpeningZone.values();

    public static final Type<SharpeningResultPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "sharpening_result"));

    public static final StreamCodec<FriendlyByteBuf, SharpeningResultPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.map(ordinal -> ZONES[ordinal], SharpeningZone::ordinal),
            SharpeningResultPayload::zone,
            SharpeningResultPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
