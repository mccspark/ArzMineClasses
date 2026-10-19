package org.sgx.classes.profession;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.network.ProfessionNetworking;
import org.sgx.classes.storage.PlayerDataStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Таймеры глобального и личных бустеров опыта. Множители живут в XPModifiers (в памяти),
 * здесь время снятия и сам глобальный множитель — чтобы пережить перезапуск сервера. */
public final class BoosterTimers {

    public static final long DEFAULT_MS = 24L * 60 * 60 * 1000;
    private static final long MAX_MS = 365L * 24 * 60 * 60 * 1000;
    private static final Pattern DURATION = Pattern.compile("^(\\d+)([smhd])$", Pattern.CASE_INSENSITIVE);
    private static final Logger LOG = LoggerFactory.getLogger("arz_classes");
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("arz_classes_boosters.properties");

    private static long globalExpiry = 0L; // 0 = таймера нет
    private static double globalMultiplier = 0.0; // множитель, с которым запущен глобальный бустер (0 = нет)
    private static final Map<UUID, Long> localExpiry = new HashMap<>();
    private static int tickCounter = 0;

    private BoosterTimers() {
    }

    /** Вызвать один раз из Arz_classes.onInitialize(). */
    public static void init() {
        ServerLifecycleEvents.SERVER_STARTED.register(BoosterTimers::load);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (++tickCounter % 20 == 0) {
                check(server);
            }
        });
    }

    // ---------- API для команд ----------

    public static void setGlobal(MinecraftServer server, double multiplier, long durationMs) {
        XPModifiers.setGlobalMultiplier(server, multiplier);
        globalExpiry = System.currentTimeMillis() + durationMs;
        globalMultiplier = multiplier;
        save();
    }

    public static void clearGlobal() {
        globalExpiry = 0L;
        globalMultiplier = 0.0;
        save();
    }

    public static void setLocal(UUID uuid, long durationMs) {
        localExpiry.put(uuid, System.currentTimeMillis() + durationMs);
        save();
    }

    public static void clearLocal(UUID uuid) {
        localExpiry.remove(uuid);
        save();
    }

    /** Для игрока НЕ в сети: помечаем таймер истёкшим, бустер снимется, как только он зайдёт. */
    public static void forceExpireLocal(UUID uuid) {
        localExpiry.put(uuid, 1L);
        save();
    }

    /** Сколько миллисекунд осталось, или -1 если таймера нет. */
    public static long remainingLocal(UUID uuid) {
        Long t = localExpiry.get(uuid);
        return t == null ? -1L : Math.max(0L, t - System.currentTimeMillis());
    }

    public static long remainingGlobal() {
        return globalExpiry == 0L ? -1L : Math.max(0L, globalExpiry - System.currentTimeMillis());
    }

    /** Копия таймеров личных бустеров: UUID -> сколько мс осталось (0, если истёк, но ещё не снят). */
    public static Map<UUID, Long> localRemainingSnapshot() {
        long now = System.currentTimeMillis();
        Map<UUID, Long> out = new HashMap<>();
        localExpiry.forEach((u, t) -> out.put(u, Math.max(0L, t - now)));
        return out;
    }

    /** Формат: 30s, 15m, 2h, 1d. */
    public static OptionalLong parseDuration(String raw) {
        Matcher m = DURATION.matcher(raw.trim());
        if (!m.matches()) {
            return OptionalLong.empty();
        }
        long n;
        try {
            n = Long.parseLong(m.group(1));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
        long unit = switch (Character.toLowerCase(m.group(2).charAt(0))) {
            case 's' -> 1000L;
            case 'm' -> 60_000L;
            case 'h' -> 3_600_000L;
            default -> 86_400_000L;
        };
        if (n <= 0 || n > MAX_MS / unit) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(n * unit);
    }

    public static String formatDuration(long ms) {
        long s = ms / 1000;
        long d = s / 86400;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("д ");
        if (h > 0) sb.append(h).append("ч ");
        if (m > 0) sb.append(m).append("м ");
        if (d == 0 && h == 0 && m == 0) sb.append(sec).append("с");
        return sb.toString().trim();
    }

    // ---------- проверка истечения ----------

    private static void check(MinecraftServer server) {
        long now = System.currentTimeMillis();
        boolean changed = false;

        if (globalExpiry != 0L && now >= globalExpiry) {
            globalExpiry = 0L;
            globalMultiplier = 0.0;
            XPModifiers.resetGlobalMultiplier(server);
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("Глобальный бустер опыта класса закончился.").withStyle(ChatFormatting.GRAY), false);
            changed = true;
        }

        Iterator<Map.Entry<UUID, Long>> it = localExpiry.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            if (now < e.getValue()) {
                continue;
            }
            UUID uuid = e.getKey();
            ServerPlayer online = server.getPlayerList().getPlayer(uuid);
            if (online == null) {
                continue; // снимем, когда игрок зайдёт в игру
            }
            it.remove();
            XPModifiers.resetLocalMultiplier(uuid);
            ModAttachments.get(online).setLocalXpMultiplier(XPModifiers.DEFAULT_MULTIPLIER);
            PlayerDataStore.save(online);
            ProfessionNetworking.syncToClient(online);
            online.sendSystemMessage(
                    Component.literal("Ваш личный бустер опыта класса закончился.").withStyle(ChatFormatting.GRAY));
            changed = true;
        }

        if (changed) {
            save();
        }
    }

    // ---------- сохранение ----------

    private static void save() {
        Properties p = new Properties();
        if (globalExpiry != 0L) {
            p.setProperty("global", Long.toString(globalExpiry));
            if (globalMultiplier > 0.0) {
                p.setProperty("globalMultiplier", Double.toString(globalMultiplier));
            }
        }
        localExpiry.forEach((u, t) -> p.setProperty("local." + u, Long.toString(t)));
        try (var out = Files.newOutputStream(FILE)) {
            p.store(out, "arz_classes XP boosters (expiry, epoch millis)");
        } catch (IOException e) {
            LOG.warn("Не удалось сохранить таймеры бустеров", e);
        }
    }

    private static void load(MinecraftServer server) {
        globalExpiry = 0L;
        globalMultiplier = 0.0;
        localExpiry.clear();
        if (!Files.exists(FILE)) {
            return;
        }
        Properties p = new Properties();
        try (var in = Files.newInputStream(FILE)) {
            p.load(in);
        } catch (IOException e) {
            LOG.warn("Не удалось прочитать таймеры бустеров", e);
            return;
        }
        try {
            String g = p.getProperty("global");
            if (g != null) {
                globalExpiry = Long.parseLong(g);
            }
            String gm = p.getProperty("globalMultiplier");
            if (gm != null) {
                globalMultiplier = Double.parseDouble(gm);
            }
            for (String key : p.stringPropertyNames()) {
                if (key.startsWith("local.")) {
                    localExpiry.put(UUID.fromString(key.substring(6)), Long.parseLong(p.getProperty(key)));
                }
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Файл таймеров бустеров повреждён, часть данных пропущена", e);
        }

        // Глобальный множитель живёт в памяти — после перезапуска возвращаем его, пока таймер не истёк
        if (globalExpiry != 0L) {
            if (System.currentTimeMillis() >= globalExpiry) {
                // бустер закончился, пока сервер был выключен
                globalExpiry = 0L;
                globalMultiplier = 0.0;
                XPModifiers.resetGlobalMultiplier(server);
                save();
            } else if (globalMultiplier > 0.0) {
                XPModifiers.setGlobalMultiplier(server, globalMultiplier);
            }
        }
    }
}