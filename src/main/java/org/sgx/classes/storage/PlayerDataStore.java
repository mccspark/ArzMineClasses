package org.sgx.classes.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionData;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public final class PlayerDataStore {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SUBDIR = "arz_classes_data/players";

    private record Snapshot(String uuid, String name, String profession, int level, int xp,
                            String subclass, int xpToNextLevel, long lastUpdated) {
    }

    public record Info(String name, String profession, int level, int xp, String subclass, int xpToNextLevel) {
    }

    private PlayerDataStore() {
    }

    public static void save(ServerPlayer player) {
        try {
            ProfessionData data = ModAttachments.get(player);
            Path path = fileFor(player.level().getServer(), player.getUUID());
            Files.createDirectories(path.getParent());

            Snapshot snapshot = new Snapshot(
                    player.getUUID().toString(),
                    player.getGameProfile().name(),
                    data.currentProfession().getSerializedName(),
                    data.level(),
                    data.xp(),
                    data.currentSubclass() != null ? data.currentSubclass().getSerializedName() : null,
                    data.isMaxLevel() ? 0 : data.currentProfession().xpToNextLevel(data.level()),
                    System.currentTimeMillis()
            );

            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(snapshot, writer);
            }
        } catch (IOException e) {
            org.sgx.classes.Arz_classes.LOGGER.warn(
                    "Не удалось сохранить снимок данных класса для {}", player.getGameProfile().name(), e);
        }
    }

    public static Info load(MinecraftServer server, UUID uuid) {
        Path path = fileFor(server, uuid);
        if (!Files.exists(path)) {
            return null;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Snapshot snapshot = GSON.fromJson(reader, Snapshot.class);
            if (snapshot == null) {
                return null;
            }
            return new Info(snapshot.name(), snapshot.profession(), snapshot.level(),
                    snapshot.xp(), snapshot.subclass(), snapshot.xpToNextLevel());
        } catch (IOException e) {
            org.sgx.classes.Arz_classes.LOGGER.warn("Не удалось прочитать снимок данных класса для {}", uuid, e);
            return null;
        }
    }

    private static Path fileFor(MinecraftServer server, UUID uuid) {
        return server.getWorldPath(LevelResource.ROOT).resolve(SUBDIR).resolve(uuid + ".json");
    }
}