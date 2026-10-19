package org.sgx.classes.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.sgx.classes.Arz_classes;

/**
 * Параметры полосы для мини-игры кузнеца: три вложенные зоны вокруг центра center —
 * жёлтая (частичный успех), зелёная (успех), ярко-зелёная (лучший успех), всё в долях
 * ширины бара [0..1]. Каждая ширина — это ПОЛНАЯ ширина зоны (не половина), т.е.
 * границы зоны — [center - width/2, center + width/2]. Зоны вложены друг в друга:
 * brightWidth < greenWidth < yellowWidth.
 *
 * repair — режим мини-игры: false = заточка (жёлто-зелёная палитра), true = ремонт
 * (синяя палитра на клиенте). Влияет только на визуал экрана, не на подсчёт зон.
 */
public record OpenSharpeningPayload(float center, float brightWidth, float greenWidth,
                                    float yellowWidth, float barSpeed, boolean repair) implements CustomPacketPayload {

    public static final Type<OpenSharpeningPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "open_sharpening"));

    public static final StreamCodec<FriendlyByteBuf, OpenSharpeningPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, OpenSharpeningPayload::center,
            ByteBufCodecs.FLOAT, OpenSharpeningPayload::brightWidth,
            ByteBufCodecs.FLOAT, OpenSharpeningPayload::greenWidth,
            ByteBufCodecs.FLOAT, OpenSharpeningPayload::yellowWidth,
            ByteBufCodecs.FLOAT, OpenSharpeningPayload::barSpeed,
            ByteBufCodecs.BOOL, OpenSharpeningPayload::repair,
            OpenSharpeningPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}