package org.sgx.classes.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.sgx.classes.Arz_classes;

/** Ctrl + колёсико с мешочком для еды в руке: direction > 0 — следующая еда, < 0 — предыдущая. */
public record CycleFoodPouchPayload(int direction) implements CustomPacketPayload {

    public static final Type<CycleFoodPouchPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "cycle_food_pouch"));

    public static final StreamCodec<ByteBuf, CycleFoodPouchPayload> CODEC =
            ByteBufCodecs.VAR_INT.map(CycleFoodPouchPayload::new, CycleFoodPouchPayload::direction);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
