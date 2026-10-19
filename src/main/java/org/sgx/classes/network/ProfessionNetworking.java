package org.sgx.classes.network;

import io.netty.buffer.ByteBuf;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ClassStats;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;
import org.sgx.classes.profession.SubclassType;
import org.sgx.classes.profession.skill.SkillAttributeSync;
import org.sgx.classes.integration.LuckPermsIntegration;

/**
 * Общие (common) пакеты для GUI выбора класса — регистрируются и грузятся на ОБЕИХ сторонах,
 * поэтому здесь нет ни одного класса из net.fabricmc.fabric.api.client.* (иначе краш на дедике
 * при загрузке класса). C2S-обработчики (реакция на клик в ClassesScreen) живут тут же.
 * Приём S2C-пакета клиентом — в client/network/ProfessionClientNetworking.
 *
 * ВНИМАНИЕ: имена StreamCodec/ByteBufCodecs/CustomPacketPayload соответствуют пакетной сети
 * 1.20.5+ (после networking rework). Если ваша версия MC старее/новее — сверьте сигнатуры
 * ByteBufCodecs.fromCodec(...) и CustomPacketPayload.Type у себя в декомпиле, тут точность
 * не гарантирована без компиляции.
 *
 * НОВОЕ: сюда же добавлена регистрация OpenRarityWheelPayload (S2C, колесо редкости после
 * крафта у кузнеца) — чисто регистрация типа/кодека, сам пакет отправляется из
 * RarityCraftHandler, а не отсюда.
 */
public final class ProfessionNetworking {

    private ProfessionNetworking() {
    }

    public record SelectProfessionPayload(ProfessionType profession) implements CustomPacketPayload {
        public static final Type<SelectProfessionPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "select_profession"));

        public static final StreamCodec<ByteBuf, SelectProfessionPayload> CODEC =
                enumCodec(ProfessionType.values()).map(SelectProfessionPayload::new, SelectProfessionPayload::profession);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SelectSubclassPayload(SubclassType subclass) implements CustomPacketPayload {
        public static final Type<SelectSubclassPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "select_subclass"));

        public static final StreamCodec<ByteBuf, SelectSubclassPayload> CODEC =
                enumCodec(SubclassType.values()).map(SelectSubclassPayload::new, SelectSubclassPayload::subclass);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record SyncProfessionDataPayload(ProfessionData data) implements CustomPacketPayload {
        public static final Type<SyncProfessionDataPayload> TYPE =
                new Type<>(Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "sync_profession_data"));

        // Переиспользуем уже готовый Codec<ProfessionData> вместо ручной сериализации полей.
        // Если fromCodec недоступен в вашей версии — используйте fromCodecWithRegistries.
        public static final StreamCodec<ByteBuf, SyncProfessionDataPayload> CODEC =
                ByteBufCodecs.fromCodec(ProfessionData.CODEC).map(SyncProfessionDataPayload::new, SyncProfessionDataPayload::data);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static <E extends Enum<E>> StreamCodec<ByteBuf, E> enumCodec(E[] values) {
        return ByteBufCodecs.VAR_INT.map(ordinal -> values[ordinal], Enum::ordinal);
    }

    // ---------- регистрация ----------

    /** Вызвать из общего инициализатора мода (Arz_classes#onInitialize) — грузится на обеих сторонах. */
    public static void registerCommon() {
        PayloadTypeRegistry.serverboundPlay().register(SelectProfessionPayload.TYPE, SelectProfessionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SelectSubclassPayload.TYPE, SelectSubclassPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SyncProfessionDataPayload.TYPE, SyncProfessionDataPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OpenRarityWheelPayload.TYPE, OpenRarityWheelPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ClassStatsPayload.TYPE, ClassStatsPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SelectProfessionPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    ProfessionData data = ModAttachments.get(player);
                    data.setCurrentProfession(payload.profession());
                    SkillAttributeSync.reapply(player, data);
                    LuckPermsIntegration.syncGroup(player, data);
                    syncToClient(player);
                    ClassStats.refresh(context.server(), player); // сразу обновить статистику классов
                }));

        ServerPlayNetworking.registerGlobalReceiver(SelectSubclassPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    ProfessionData data = ModAttachments.get(player);
                    if (!data.chooseSubclass(payload.subclass())) {
                        if (!data.subclassUnlocked()) {
                            player.sendSystemMessage(Component.translatable("screen.arz_classes.subclass_locked")
                                    .withStyle(ChatFormatting.RED));
                        }
                        syncToClient(player); // клиент мог показывать устаревшее состояние
                        return;
                    }
                    SkillAttributeSync.reapply(player, data);
                    syncToClient(player);
                }));
    }

    /** Шлёт текущее состояние класса игроку. Вызывать при джойне и после любой мутации ProfessionData. */
    public static void syncToClient(ServerPlayer player) {
        ServerPlayNetworking.send(player, new SyncProfessionDataPayload(ModAttachments.get(player)));
    }
}