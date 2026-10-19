package org.sgx.classes.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.sgx.classes.Arz_classes;

/**
 * Отправляется сервером клиенту после крафта у кузнеца: веса всех 5 секторов колеса
 * (в фиксированном порядке COMMON/RARE/EPIC/LEGENDARY/MYTHIC — как в ItemRarity.values())
 * и УЖЕ ГОТОВЫЙ результат броска (resultOrdinal), который сервер выбрал сам. Колесо на
 * клиенте — чисто визуализация: сколько бы оно ни крутилось, останавливается ровно на
 * resultOrdinal, подделать/пересчитать на клиенте нельзя.
 */
public record OpenRarityWheelPayload(float commonWeight, float rareWeight, float epicWeight,
                                     float legendaryWeight, float mythicWeight, int resultOrdinal)
        implements CustomPacketPayload {

    public static final Type<OpenRarityWheelPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "open_rarity_wheel"));

    public static final StreamCodec<FriendlyByteBuf, OpenRarityWheelPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, OpenRarityWheelPayload::commonWeight,
            ByteBufCodecs.FLOAT, OpenRarityWheelPayload::rareWeight,
            ByteBufCodecs.FLOAT, OpenRarityWheelPayload::epicWeight,
            ByteBufCodecs.FLOAT, OpenRarityWheelPayload::legendaryWeight,
            ByteBufCodecs.FLOAT, OpenRarityWheelPayload::mythicWeight,
            ByteBufCodecs.VAR_INT, OpenRarityWheelPayload::resultOrdinal,
            OpenRarityWheelPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}