package org.sgx.classes.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.sgx.classes.Arz_classes;

public record StartRarityUpgradePayload() implements CustomPacketPayload {

    public static final Type<StartRarityUpgradePayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "start_rarity_upgrade"));

    public static final StreamCodec<FriendlyByteBuf, StartRarityUpgradePayload> CODEC =
            StreamCodec.unit(new StartRarityUpgradePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}