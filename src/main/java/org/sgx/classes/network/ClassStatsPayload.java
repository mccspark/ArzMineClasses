package org.sgx.classes.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.sgx.classes.Arz_classes;

import java.util.List;

/** S2C: сколько игроков взяли каждый класс. Индекс списка = ProfessionType#ordinal. */
public record ClassStatsPayload(List<Integer> counts) implements CustomPacketPayload {

    public static final Type<ClassStatsPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "class_stats"));

    public static final StreamCodec<ByteBuf, ClassStatsPayload> CODEC =
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list())
                    .map(ClassStatsPayload::new, ClassStatsPayload::counts);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}