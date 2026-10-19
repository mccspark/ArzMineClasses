package org.sgx.classes.profession;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.network.ClassStatsPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Статистика классов: сколько игроков (включая тех, кто сейчас не в сети) взяли каждый класс.
 * Хранится в world/data как карта UUID -> класс; счётчики считаются на лету, поэтому при смене
 * класса, сбросе или правке админом они не могут разъехаться с реальностью.
 *
 * Данные обновляются без единого хука в местах смены класса: раз в REFRESH_INTERVAL_TICKS
 * сервер сверяет текущий класс каждого онлайн-игрока с записанным, плюс сверка при входе.
 * Любое изменение рассылается всем игрокам пакетом ClassStatsPayload.
 */
public final class ClassStats {

    private static final int REFRESH_INTERVAL_TICKS = 100; // 5 секунд
    private static int tickCounter = 0;

    private ClassStats() {
    }

    /** Вызвать из Arz_classes#onInitialize. */
    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter < REFRESH_INTERVAL_TICKS) {
                return;
            }
            tickCounter = 0;
            refreshAll(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            refresh(server, player);
            // вошедший игрок получает актуальные числа в любом случае
            ServerPlayNetworking.send(player, new ClassStatsPayload(counts(server)));
        });
    }

    /** Сверить класс одного игрока с записанным; при изменении — разослать всем. */
    public static void refresh(MinecraftServer server, ServerPlayer player) {
        ProfessionType type = ModAttachments.get(player).currentProfession();
        if (data(server).set(player.getUUID(), type)) {
            broadcast(server);
        }
    }

    private static void refreshAll(MinecraftServer server) {
        Data data = data(server);
        boolean changed = false;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ProfessionType type = ModAttachments.get(player).currentProfession();
            changed |= data.set(player.getUUID(), type);
        }
        if (changed) {
            broadcast(server);
        }
    }

    /** Счётчики по классам: индекс = ProfessionType#ordinal, NONE не считается. */
    public static List<Integer> counts(MinecraftServer server) {
        int[] counts = new int[ProfessionType.values().length];
        for (String name : data(server).assignments.values()) {
            ProfessionType type = parse(name);
            if (type != null && type != ProfessionType.NONE) {
                counts[type.ordinal()]++;
            }
        }
        List<Integer> result = new ArrayList<>(counts.length);
        for (int c : counts) {
            result.add(c);
        }
        return result;
    }

    private static void broadcast(MinecraftServer server) {
        ClassStatsPayload payload = new ClassStatsPayload(counts(server));
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    private static Data data(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Data.TYPE);
    }

    private static ProfessionType parse(String serializedName) {
        for (ProfessionType type : ProfessionType.values()) {
            if (type.getSerializedName().equals(serializedName)) {
                return type;
            }
        }
        return null;
    }

    // ==================== СОХРАНЕНИЕ ====================

    /** world/data/arz_classes_class_stats.dat: UUID игрока -> serializedName класса. */
    public static class Data extends SavedData {
        public static final String DATA_NAME = "class_stats";

        public static final Codec<Data> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.unboundedMap(Codec.STRING, Codec.STRING)
                                .optionalFieldOf("Assignments", Map.of())
                                .forGetter(d -> d.assignments)
                ).apply(instance, Data::new)
        );

        public static final SavedDataType<Data> TYPE = new SavedDataType<>(
                Identifier.fromNamespaceAndPath("arz_classes", DATA_NAME),
                Data::new,
                CODEC,
                null
        );

        private final Map<String, String> assignments = new HashMap<>();

        public Data() {
        }

        public Data(Map<String, String> assignments) {
            this.assignments.putAll(assignments);
        }

        /** @return true, если запись изменилась. */
        boolean set(UUID uuid, ProfessionType type) {
            String key = uuid.toString();
            String old;
            if (type == null || type == ProfessionType.NONE) {
                old = assignments.remove(key);
                if (old == null) {
                    return false;
                }
            } else {
                String value = type.getSerializedName();
                old = assignments.put(key, value);
                if (value.equals(old)) {
                    return false;
                }
            }
            setDirty();
            return true;
        }
    }
}