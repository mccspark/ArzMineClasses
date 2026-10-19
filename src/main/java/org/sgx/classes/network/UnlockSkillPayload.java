package org.sgx.classes.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.sgx.classes.Arz_classes;

/** Пустое тело — сервер сам знает, какой узел следующий (unlockedSkillCount на ProfessionData). */
public record UnlockSkillPayload() implements CustomPacketPayload {

    public static final Type<UnlockSkillPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "unlock_skill"));

    public static final StreamCodec<ByteBuf, UnlockSkillPayload> CODEC =
            StreamCodec.unit(new UnlockSkillPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}