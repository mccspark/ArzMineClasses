package org.sgx.classes.profession;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.network.ProfessionNetworking;
import org.sgx.classes.storage.PlayerDataStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Глобальное «открыть/закрыть выбор подкласса ВСЕМ».
 * <p>
 * Работает не правкой файлов оффлайн-игроков, а флагом на уровне мира: пока флаг включён, каждый игрок
 * получает открытие при входе — и те, кто давно не заходил, и те, кто ещё ни разу не заходил.
 * Состояние лежит в файле arz_classes_subclass_unlock.json в папке мира.
 * <p>
 * Вызвать {@link #init()} один раз из onInitialize главного класса.
 */
public final class SubclassUnlockAll {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "arz_classes_subclass_unlock.json";

    /** Включён ли режим «открыто всем». */
    private static boolean allUnlocked;
    /** Игроки, которые уже обработаны текущим «unlock all» (чтобы ручной lock <ник> после этого не перетирался). */
    private static final Set<UUID> granted = new HashSet<>();
    /** Игроки, которым «lock all» надо закрыть выбор при следующем входе (они были оффлайн). */
    private static final Set<UUID> pendingRevoke = new HashSet<>();

    private static MinecraftServer server;

    private SubclassUnlockAll() {
    }

    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(SubclassUnlockAll::load);
        ServerLifecycleEvents.SERVER_STOPPED.register(s -> {
            server = null;
            allUnlocked = false;
            granted.clear();
            pendingRevoke.clear();
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, s) -> onJoin(handler.getPlayer()));
    }

    public static boolean isAllUnlocked() {
        return allUnlocked;
    }

    // ==================== команды ====================

    /** @return сколько игроков в сети реально изменилось */
    public static int unlockAll(MinecraftServer s) {
        allUnlocked = true;
        pendingRevoke.clear();
        int changed = 0;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            granted.add(p.getGameProfile().id());
            if (setUnlocked(p, true)) {
                changed++;
            }
        }
        save();
        return changed;
    }

    /** @return сколько игроков в сети реально изменилось */
    public static int lockAll(MinecraftServer s) {
        allUnlocked = false;
        Set<UUID> online = new HashSet<>();
        int changed = 0;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            online.add(p.getGameProfile().id());
            if (setUnlocked(p, false)) {
                changed++;
            }
        }
        // оффлайн-игроков, которым открывали через unlock all, закроем при следующем входе
        pendingRevoke.addAll(granted);
        pendingRevoke.removeAll(online);
        granted.clear();
        save();
        return changed;
    }

    // ==================== вход игрока ====================

    /**
     * Применить глобальное состояние к игроку. Вызывается при входе;
     * если при смене класса флаг подкласса сбрасывается — вызвать и там.
     */
    public static void onJoin(ServerPlayer player) {
        if (server == null) {
            return;
        }
        UUID id = player.getGameProfile().id();
        if (allUnlocked) {
            if (granted.add(id)) {
                setUnlocked(player, true);
                save();
            }
        } else if (pendingRevoke.remove(id)) {
            setUnlocked(player, false);
            save();
        }
    }

    // ==================== внутреннее ====================

    private static boolean setUnlocked(ServerPlayer player, boolean unlock) {
        ProfessionData data = ModAttachments.get(player);
        if (data.subclassUnlocked() == unlock) {
            return false;
        }
        data.setSubclassUnlocked(unlock);
        PlayerDataStore.save(player);
        ProfessionNetworking.syncToClient(player);

        // игроку без класса сообщение ни к чему — флаг выставляем молча
        if (data.currentProfession() != ProfessionType.NONE) {
            if (unlock) {
                player.sendSystemMessage(Component.literal("Вам открыт выбор подкласса! Загляните в дерево навыков.")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            } else {
                player.sendSystemMessage(Component.literal("Выбор подкласса снова закрыт.")
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        return true;
    }

    private static Path file(MinecraftServer s) {
        return s.getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
    }

    private static void load(MinecraftServer s) {
        server = s;
        allUnlocked = false;
        granted.clear();
        pendingRevoke.clear();

        Path file = file(s);
        if (!Files.exists(file)) {
            return;
        }
        try {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            allUnlocked = root.has("allUnlocked") && root.get("allUnlocked").getAsBoolean();
            readIds(root, "granted", granted);
            readIds(root, "pendingRevoke", pendingRevoke);
        } catch (IOException | RuntimeException e) {
            // битый файл — начинаем с чистого состояния
        }
    }

    private static void readIds(JsonObject root, String key, Set<UUID> into) {
        if (!root.has(key) || !root.get(key).isJsonArray()) {
            return;
        }
        for (JsonElement el : root.getAsJsonArray(key)) {
            try {
                into.add(UUID.fromString(el.getAsString()));
            } catch (RuntimeException ignored) {
                // пропускаем мусор
            }
        }
    }

    private static void save() {
        if (server == null) {
            return;
        }
        JsonObject root = new JsonObject();
        root.addProperty("allUnlocked", allUnlocked);
        root.add("granted", ids(granted));
        root.add("pendingRevoke", ids(pendingRevoke));
        try {
            Files.writeString(file(server), GSON.toJson(root));
        } catch (IOException ignored) {
            // не смогли записать — состояние живёт в памяти до перезапуска
        }
    }

    private static JsonArray ids(Set<UUID> set) {
        JsonArray arr = new JsonArray();
        for (UUID id : set) {
            arr.add(id.toString());
        }
        return arr;
    }
}