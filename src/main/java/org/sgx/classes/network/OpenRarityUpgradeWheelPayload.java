package org.sgx.classes.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.sgx.classes.Arz_classes;

public record OpenRarityUpgradeWheelPayload(float successChance, boolean success, int targetRarityOrdinal)
        implements CustomPacketPayload {

    public static final Type<OpenRarityUpgradeWheelPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "open_rarity_upgrade_wheel"));

    public static final StreamCodec<FriendlyByteBuf, OpenRarityUpgradeWheelPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, OpenRarityUpgradeWheelPayload::successChance,
            ByteBufCodecs.BOOL, OpenRarityUpgradeWheelPayload::success,
            ByteBufCodecs.VAR_INT, OpenRarityUpgradeWheelPayload::targetRarityOrdinal,
            OpenRarityUpgradeWheelPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}