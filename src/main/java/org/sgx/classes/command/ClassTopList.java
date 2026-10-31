package org.sgx.classes.command;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;
import org.sgx.classes.storage.PlayerDataStore;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * /profession top &lt;класс&gt; [страница] — все игроки выбранного класса (онлайн и оффлайн)
 * от самого прокачанного к самому слабому: уровень, опыт, а также наигранное время и опыт в час.
 * Опыт в час помогает вычислить накрутку: у накрутивших он на порядок выше, чем у остальных.
 */
final class ClassTopList {

    private static final int PAGE_SIZE = 8;
    private static final ChatFormatting HEADER = ChatFormatting.GOLD;
    private static final ChatFormatting LABEL = ChatFormatting.GRAY;
    private static final ChatFormatting VALUE = ChatFormatting.WHITE;
    private static final ChatFormatting GOOD = ChatFormatting.GREEN;
    private static final ChatFormatting BAD = ChatFormatting.RED;
    private static final String DIVIDER = "▬".repeat(28);

    /** hours &lt; 0 - статистика времени не найдена. */
    private record Row(String name, boolean online, int level, int xp, int needed, long totalXp, double hours) {
        double xpPerHour() {
            return hours > 0.05 ? totalXp / hours : -1;
        }
    }

    private ClassTopList() {
    }

    static int run(CommandContext<CommandSourceStack> ctx, int requestedPage) {
        CommandSourceStack source = ctx.getSource();
        String raw = StringArgumentType.getString(ctx, "class");

        ProfessionType type = null;
        for (ProfessionType t : ProfessionType.values()) {
            if (t != ProfessionType.NONE && t.getSerializedName().equalsIgnoreCase(raw)) {
                type = t;
            }
        }
        if (type == null) {
            source.sendFailure(Component.literal("✘ Неизвестный класс: " + raw).withStyle(BAD));
            return 0;
        }

        List<Row> rows = collect(source.getServer(), type);
        if (rows.isEmpty()) {
            source.sendFailure(Component.literal("✘ Игроков класса " + type.getSerializedName() + " не найдено.")
                    .withStyle(BAD));
            return 0;
        }

        int pages = (rows.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int page = Math.max(1, Math.min(requestedPage, pages));
        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(rows.size(), from + PAGE_SIZE);
        String className = type.getSerializedName();

        source.sendSuccess(ClassTopList::divider, false);
        source.sendSuccess(() -> Component.literal("Рейтинг класса " + className + " ")
                .withStyle(HEADER, ChatFormatting.BOLD)
                .append(Component.literal("(всего " + rows.size() + ", стр. " + page + " / " + pages + ")")
                        .withStyle(LABEL)), false);
        source.sendSuccess(ClassTopList::divider, false);

        for (int i = from; i < to; i++) {
            Row row = rows.get(i);
            int place = i + 1;
            source.sendSuccess(() -> nameLine(place, row), false);
            source.sendSuccess(() -> detailLine(row), false);
        }
        source.sendSuccess(ClassTopList::divider, false);
        if (page < pages) {
            source.sendSuccess(() -> Component.literal("Дальше: /profession top " + className + " " + (page + 1))
                    .withStyle(LABEL), false);
        }
        return 1;
    }

    private static Component nameLine(int place, Row row) {
        return Component.literal(" " + place + ". ").withStyle(LABEL)
                .append(Component.literal(row.name()).withStyle(HEADER, ChatFormatting.BOLD))
                .append(Component.literal(row.online() ? " [online]" : " [offline]")
                        .withStyle(row.online() ? GOOD : LABEL));
    }

    private static Component detailLine(Row row) {
        MutableComponent c = Component.literal("    ");
        c.append(pair("Ур.", row.level() + "/" + ProfessionType.MAX_LEVEL, GOOD));
        c.append(Component.literal(" · ").withStyle(LABEL));
        c.append(pair("Опыт", row.needed() < 0 ? "МАКС" : row.xp() + "/" + row.needed(), VALUE));
        c.append(Component.literal(" · ").withStyle(LABEL));
        c.append(pair("Всего", String.format(Locale.ROOT, "%,d", row.totalXp()), VALUE));
        c.append(Component.literal(" · ").withStyle(LABEL));
        if (row.hours() < 0) {
            c.append(pair("Время", "нет данных", LABEL));
        } else {
            c.append(pair("Время", String.format(Locale.ROOT, "%.1f ч", row.hours()), VALUE));
            c.append(Component.literal(" · ").withStyle(LABEL));
            double perHour = row.xpPerHour();
            c.append(pair("Опыт/час", perHour < 0 ? "—" : String.format(Locale.ROOT, "%,.0f", perHour),
                    ChatFormatting.AQUA));
        }
        return c;
    }

    private static Component pair(String label, String value, ChatFormatting color) {
        return Component.literal(label + ": ").withStyle(LABEL)
                .append(Component.literal(value).withStyle(color));
    }

    private static Component divider() {
        return Component.literal(DIVIDER).withStyle(HEADER, ChatFormatting.BOLD);
    }

    // ==================== сбор данных ====================

    private static List<Row> collect(MinecraftServer server, ProfessionType type) {
        List<Row> rows = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();

        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            UUID id = online.getGameProfile().id();
            seen.add(id);
            ProfessionData data = ModAttachments.get(online);
            if (data.currentProfession() != type) {
                continue;
            }
            int needed = data.isMaxLevel() ? -1 : type.xpToNextLevel(data.level());
            rows.add(new Row(online.getName().getString(), true, data.level(), data.xp(), needed,
                    type.totalXp(data.level(), data.xp()), playHours(server, id)));
        }

        Path dir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                files.map(p -> p.getFileName().toString())
                        .filter(n -> n.endsWith(".dat"))
                        .forEach(n -> {
                            UUID id;
                            try {
                                id = UUID.fromString(n.substring(0, n.length() - 4));
                            } catch (IllegalArgumentException e) {
                                return;
                            }
                            if (!seen.add(id)) {
                                return;
                            }
                            PlayerDataStore.Info info = PlayerDataStore.load(server, id);
                            if (info == null || info.profession() == null
                                    || !info.profession().equals(type.getSerializedName())) {
                                return;
                            }
                            int needed = info.level() >= ProfessionType.MAX_LEVEL ? -1 : info.xpToNextLevel();
                            String name = info.name() != null ? info.name() : id.toString().substring(0, 8);
                            rows.add(new Row(name, false, info.level(), info.xp(), needed,
                                    type.totalXp(info.level(), info.xp()), playHours(server, id)));
                        });
            } catch (IOException ignored) {
                // каталог недоступен - покажем хотя бы онлайн
            }
        }

        rows.sort(Comparator.comparingLong(Row::totalXp).reversed()
                .thenComparing(Row::name, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    /** Наигранное время из ванильной статистики мира (stats/&lt;uuid&gt;.json), в часах; -1 если нет. */
    private static double playHours(MinecraftServer server, UUID id) {
        Path file = server.getWorldPath(LevelResource.ROOT).resolve("stats").resolve(id + ".json");
        if (!Files.exists(file)) {
            return -1;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject stats = root.has("stats") ? root.getAsJsonObject("stats") : root;
            JsonObject custom = stats.getAsJsonObject("minecraft:custom");
            if (custom == null) {
                return -1;
            }
            JsonElement ticks = custom.has("minecraft:play_time")
                    ? custom.get("minecraft:play_time") : custom.get("minecraft:play_one_minute");
            return ticks == null ? -1 : ticks.getAsLong() / 72000.0; // 20 тиков * 3600 сек
        } catch (IOException | RuntimeException e) {
            return -1;
        }
    }
}
