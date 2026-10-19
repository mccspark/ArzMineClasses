package org.sgx.classes.client.gui;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.profession.ProfessionType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Руководства, прочитанные до конца (все вкладки).
 * Ключ: <uuid игрока>@<сервер или мир>/<класс>. Хранится на клиенте: config/arz_classes/guide_seen.json
 */
public final class GuideProgress {

    private static final Path FILE = FabricLoader.getInstance().getConfigDir()
            .resolve(Arz_classes.MOD_ID).resolve("guide_seen.json");
    private static Set<String> seen;

    private GuideProgress() {
    }

    /** null — если игрок/мир ещё не загружены (тогда ничего не показываем и не пишем). */
    public static String key(ProfessionType type) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return null;
        }
        String world;
        if (mc.getCurrentServer() != null) {
            world = mc.getCurrentServer().ip;
        } else if (mc.getSingleplayerServer() != null) {
            world = "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName();
        } else {
            return null;
        }
        return mc.player.getUUID() + "@" + world + "/" + type.getSerializedName();
    }

    private static Set<String> seen() {
        if (seen == null) {
            seen = new HashSet<>();
            try {
                if (Files.exists(FILE)) {
                    for (JsonElement e : JsonParser.parseString(Files.readString(FILE)).getAsJsonArray()) {
                        seen.add(e.getAsString());
                    }
                }
            } catch (Exception ignored) {
                // повреждённый файл = руководство покажется заново
            }
        }
        return seen;
    }

    private static void save() {
        JsonArray arr = new JsonArray();
        seen().forEach(arr::add);
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, new Gson().toJson(arr));
        } catch (IOException ignored) {
        }
    }

    public static boolean isSeen(ProfessionType type) {
        String key = key(type);
        return key == null || seen().contains(key);
    }

    public static void markSeen(ProfessionType type) {
        String key = key(type);
        if (key != null && seen().add(key)) {
            save();
        }
    }

    /** Сбросить отметку (при смене класса прогресс обнуляется — руководство покажется снова). */
    public static void forget(ProfessionType type) {
        String key = key(type);
        if (key != null && seen().remove(key)) {
            save();
        }
    }
}