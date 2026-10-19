package org.sgx.classes.attachment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.profession.ProfessionData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

/** <папка мира>/arz_classes/players/<Ник>.json — последнее состояние игрока В ЭТОМ МИРЕ.
 *
 * Игрока опознаём по uuid ВНУТРИ файла, а имя файла = текущий ник. Если игрок сменил ник,
 * его файл переименовывается. Старые файлы вида <uuid>.json подхватываются и переименовываются.
 *
 * Запись: при выходе, остановке сервера и автоматически при изменении данных
 * (проверка раз в секунду; на диск пишется только при реальном отличии).
 * Чтение: при входе. Админ правит поле "data", пока игрока нет на сервере. Поле "uuid" менять нельзя.
 * Сломанный JSON переименовывается в .broken, игрок продолжает с текущими данными. */
public final class PlayerDataFiles {

    private static final Logger LOGGER = LoggerFactory.getLogger(Arz_classes.MOD_ID);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CHECK_INTERVAL_TICKS = 20;

    /** Последние записанные данные игрока — чтобы не писать файл без изменений. */
    private static final Map<UUID, String> LAST_WRITTEN = new HashMap<>();
    /** Файл каждого онлайн-игрока (определяется при входе). */
    private static final Map<UUID, Path> FILES = new HashMap<>();
    private static int tickCounter = 0;

    /** Папка файлов игроков внутри текущего мира; задаётся при старте сервера. */
    private static Path playersDir;

    private PlayerDataFiles() {
    }

    private static Path dir() {
        return playersDir;
    }

    private static String sanitize(String name) {
        return name.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }

    private static Path desiredFile(ServerPlayer player) {
        return dir().resolve(sanitize(player.getName().getString()) + ".json");
    }

    /** uuid из файла или null, если файл не читается / поля нет. */
    private static String readUuid(Path file) {
        try {
            JsonObject obj = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            return obj.has("uuid") ? obj.get("uuid").getAsString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Ищет файл игрока по uuid внутри — имя файла может быть устаревшим (смена ника, старый формат). */
    private static Path findByUuid(UUID uuid) {
        Path d = dir();
        if (!Files.isDirectory(d)) {
            return null;
        }
        try (Stream<Path> stream = Files.list(d)) {
            List<Path> files = stream.filter(p -> p.getFileName().toString().endsWith(".json")).toList();
            for (Path f : files) {
                if (uuid.toString().equals(readUuid(f))) {
                    return f;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Не удалось просмотреть папку {}", d, e);
        }
        return null;
    }

    private static boolean sameFile(Path a, Path b) {
        try {
            return Files.exists(a) && Files.exists(b) && Files.isSameFile(a, b);
        } catch (IOException e) {
            return false;
        }
    }

    private static void move(Path from, Path to) {
        try {
            Files.move(from, to, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.error("Не удалось переименовать {} -> {}", from, to, e);
        }
    }

    /** Освобождает желаемое имя, если оно занято файлом ДРУГОГО игрока. */
    private static void freeTarget(Path desired, UUID owner) {
        if (!Files.exists(desired)) {
            return;
        }
        String other = readUuid(desired);
        if (owner.toString().equals(other)) {
            return;
        }
        String base = desired.getFileName().toString().replaceAll("\\.json$", "");
        if (other == null) {
            move(desired, desired.resolveSibling(base + ".json.broken"));
        } else {
            move(desired, desired.resolveSibling(base + "_" + other + ".json"));
        }
    }

    /** Определяет, какой файл принадлежит игроку, и при необходимости переименовывает его под текущий ник. */
    private static Path locate(ServerPlayer player) {
        Path desired = desiredFile(player);
        Path existing = findByUuid(player.getUUID());
        try {
            Files.createDirectories(dir());
        } catch (IOException e) {
            LOGGER.error("Не удалось создать папку {}", dir(), e);
        }
        if (existing == null) {
            // Файла игрока ещё нет. Если желаемое имя занято чужим файлом с известным uuid — освободим его.
            // Нечитаемый файл на этом имени оставляем: load() сам пометит его как .broken.
            if (Files.exists(desired) && readUuid(desired) != null) {
                freeTarget(desired, player.getUUID());
            }
            return desired;
        }
        if (!sameFile(existing, desired)) {
            freeTarget(desired, player.getUUID());
            move(existing, desired);
        }
        return desired;
    }

    private static Path fileOf(ServerPlayer player) {
        return FILES.computeIfAbsent(player.getUUID(), id -> locate(player));
    }

    /** Записывает файл игрока, только если данные изменились с прошлой записи. */
    public static void save(ServerPlayer player) {
        if (playersDir == null) {
            return;
        }
        ProfessionData data = ModAttachments.get(player);
        var encoded = ProfessionData.CODEC.encodeStart(JsonOps.INSTANCE, data);
        if (encoded.result().isEmpty()) {
            LOGGER.error("Не удалось сериализовать данные игрока {}", player.getUUID());
            return;
        }
        JsonElement dataJson = encoded.result().get();
        String snapshot = dataJson.toString();
        if (snapshot.equals(LAST_WRITTEN.get(player.getUUID()))) {
            return;
        }

        JsonObject root = new JsonObject();
        root.addProperty("uuid", player.getUUID().toString());
        root.addProperty("name", player.getName().getString());
        root.addProperty("saved_at", Instant.now().toString());
        root.add("data", dataJson);
        try {
            Path file = fileOf(player);
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(root)); // перезаписывает: остаётся только последнее
            LAST_WRITTEN.put(player.getUUID(), snapshot);
        } catch (IOException e) {
            LOGGER.error("Не удалось записать файл игрока {}", player.getUUID(), e);
        }
    }

    private static void load(ServerPlayer player) {
        if (playersDir == null) {
            return;
        }
        LAST_WRITTEN.remove(player.getUUID());
        FILES.remove(player.getUUID());
        Path file = fileOf(player); // тут же переименует файл при смене ника
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonElement data = JsonParser.parseString(Files.readString(file))
                    .getAsJsonObject().get("data");
            var decoded = ProfessionData.CODEC.parse(JsonOps.INSTANCE, data);
            if (decoded.result().isPresent()) {
                player.setAttached(ModAttachments.PROFESSION_DATA, decoded.result().get());
            } else {
                LOGGER.error("Файл {} содержит некорректные данные, правки не применены", file);
                markBroken(file);
            }
        } catch (Exception e) {
            LOGGER.error("Не удалось прочитать файл {}", file, e);
            markBroken(file);
        }
    }

    private static void markBroken(Path file) {
        move(file, file.resolveSibling(file.getFileName() + ".broken"));
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            playersDir = server.getWorldPath(LevelResource.ROOT)
                    .resolve(Arz_classes.MOD_ID).resolve("players");
            // кэши статические: при смене мира в одной сессии они хранили бы данные прошлого мира
            LAST_WRITTEN.clear();
            FILES.clear();
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> load(handler.getPlayer()));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            save(handler.getPlayer());
            LAST_WRITTEN.remove(handler.getPlayer().getUUID());
            FILES.remove(handler.getPlayer().getUUID());
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server ->
                server.getPlayerList().getPlayers().forEach(PlayerDataFiles::save));

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter % CHECK_INTERVAL_TICKS != 0) {
                return;
            }
            server.getPlayerList().getPlayers().forEach(PlayerDataFiles::save);
        });
    }
}