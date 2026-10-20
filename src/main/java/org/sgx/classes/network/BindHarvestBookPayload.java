package org.sgx.classes.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.sgx.classes.Arz_classes;

/**
 * Клиент → сервер: «привяжи мою непривязанную книгу урожая». Нужен, когда книгу открывают
 * хоткеем (например, из слота Trinkets), а не ПКМ в руке — ПКМ привязывает книгу сам.
 */
public record BindHarvestBookPayload() implements CustomPacketPayload {

    public static final BindHarvestBookPayload INSTANCE = new BindHarvestBookPayload();

    public static final Type<BindHarvestBookPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "bind_harvest_book"));

    public static final StreamCodec<ByteBuf, BindHarvestBookPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
