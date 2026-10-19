package org.sgx.classes.profession;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class XPModifiers {

    public static final double DEFAULT_MULTIPLIER = 1.0;

    private static double globalMultiplier = DEFAULT_MULTIPLIER;
    private static final Map<UUID, Double> localMultipliers = new HashMap<>();

    private XPModifiers() {
    }

    public static double globalMultiplier() {
        return globalMultiplier;
    }

    public static void setGlobalMultiplier(MinecraftServer server, double value) {
        globalMultiplier = value;
        markGlobalDirty(server);
    }

    public static void resetGlobalMultiplier(MinecraftServer server) {
        globalMultiplier = DEFAULT_MULTIPLIER;
        markGlobalDirty(server);
    }

    public static double localMultiplier(UUID uuid) {
        return localMultipliers.getOrDefault(uuid, DEFAULT_MULTIPLIER);
    }

    public static void setLocalMultiplier(UUID uuid, double value) {
        if (value == DEFAULT_MULTIPLIER) {
            localMultipliers.remove(uuid);
        } else {
            localMultipliers.put(uuid, value);
        }
    }

    public static void resetLocalMultiplier(UUID uuid) {
        localMultipliers.remove(uuid);
    }

    public static boolean hasLocalMultiplier(UUID uuid) {
        return localMultipliers.containsKey(uuid) && localMultipliers.get(uuid) != DEFAULT_MULTIPLIER;
    }
    /** Копия всех активных личных множителей (для команды /profession boosters). */
    public static Map<UUID, Double> allLocalMultipliers() {
        return new HashMap<>(localMultipliers);
    }

    /** Итоговый множитель опыта класса для конкретного игрока. */
    public static double multiplierFor(UUID uuid) {
        return globalMultiplier * localMultiplier(uuid);
    }

    // ==================== СОХРАНЕНИЕ ГЛОБАЛЬНОГО МНОЖИТЕЛЯ ====================

    private static void markGlobalDirty(MinecraftServer server) {
        if (server != null) {
            GlobalXpData savedData = server.overworld().getDataStorage()
                    .computeIfAbsent(GlobalXpData.TYPE);
            savedData.setMultiplier(globalMultiplier); // раньше значение сюда не попадало и терялось после перезапуска
            savedData.setDirty();
        }
    }

    public static void loadGlobalOnServerStart(MinecraftServer server) {
        GlobalXpData savedData = server.overworld().getDataStorage()
                .computeIfAbsent(GlobalXpData.TYPE);
        globalMultiplier = savedData.getMultiplier();
    }

    /** Внутренний класс для сохранения глобального множителя в world/data/ */
    public static class GlobalXpData extends SavedData {
        public static final String DATA_NAME = "profession_global_xp";

        public static final Codec<GlobalXpData> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.DOUBLE.optionalFieldOf("GlobalMultiplier", DEFAULT_MULTIPLIER)
                                .forGetter(GlobalXpData::getMultiplier)
                ).apply(instance, GlobalXpData::new)
        );

        public static final SavedDataType<GlobalXpData> TYPE = new SavedDataType<>(
                Identifier.fromNamespaceAndPath("arz_classes", DATA_NAME),
                GlobalXpData::new,
                CODEC,
                null
        );

        private double multiplier = DEFAULT_MULTIPLIER;

        public GlobalXpData() {
        }

        public GlobalXpData(double multiplier) {
            this.multiplier = multiplier;
            XPModifiers.globalMultiplier = multiplier;
        }

        public double getMultiplier() {
            return multiplier;
        }

        public void setMultiplier(double multiplier) {
            this.multiplier = multiplier;
        }
    }
}