package org.sgx.classes.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.minecraft.commands.Commands.literal;

/**
 * /discord и /dynmap — выдают игроку кликабельную ссылку.
 * <p>
 * Оформление лежит в config/arz_classes/links.json и перечитывается при каждом вызове команды,
 * так что править его можно без перезапуска сервера.
 * <p>
 * Теги в "message":
 * <ul>
 *   <li>{@code <gradient:#RRGGBB:#RRGGBB>} — градиент до {@code </gradient>} или до конца сообщения</li>
 *   <li>{@code <url>} — кликабельная ссылка (текст берётся из "text", иначе из "url")</li>
 *   <li>{@code <player>} — ник того, кто вызвал команду</li>
 * </ul>
 * Любые другие теги (например {@code <fcolor:1>}) молча вырезаются.
 */
public final class LinkCommands {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Pattern TAG = Pattern.compile("<([^<>]+)>");

    private LinkCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("discord").executes(ctx -> send(ctx, "discord")));
        dispatcher.register(literal("dynmap").executes(ctx -> send(ctx, "dynmap")));
    }

    private static int send(CommandContext<CommandSourceStack> ctx, String key) {
        CommandSourceStack source = ctx.getSource();
        JsonObject root = loadConfig();
        JsonObject def = defaults().getAsJsonObject(key);
        JsonObject entry = root.has(key) && root.get(key).isJsonObject() ? root.getAsJsonObject(key) : def;

        String url = str(entry, "url", str(def, "url", ""));
        String text = str(entry, "text", str(def, "text", url));
        String message = str(entry, "message", str(def, "message", "<url>"));

        Component component = render(message, url, text, source.getTextName());
        source.sendSuccess(() -> component, false);
        return 1;
    }

    // ==================== конфиг ====================

    private static Path configFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("arz_classes").resolve("links.json");
    }

    private static JsonObject defaults() {
        JsonObject root = new JsonObject();

        JsonObject discord = new JsonObject();
        discord.addProperty("url", "https://discord.gg/mnGJn8JQ8D");
        discord.addProperty("text", "discord.gg/mnGJn8JQ8D");
        discord.addProperty("message", "<gradient:#a6ffcd:#d9ffea>💬 Наш Discord: <url>");
        root.add("discord", discord);

        JsonObject dynmap = new JsonObject();
        dynmap.addProperty("url", "https://map.arzmine.ru");
        dynmap.addProperty("text", "map.arzmine.ru");
        dynmap.addProperty("message", "<gradient:#a6ffcd:#d9ffea>🗺 Динамическая карта: <url>");
        root.add("dynmap", dynmap);

        return root;
    }

    private static JsonObject loadConfig() {
        Path file = configFile();
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(defaults()));
            }
            return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        } catch (IOException | RuntimeException e) {
            return defaults(); // битый конфиг не должен ломать команду
        }
    }

    private static String str(JsonObject obj, String key, String fallback) {
        if (obj != null && obj.has(key) && obj.get(key).isJsonPrimitive()) {
            return obj.get(key).getAsString();
        }
        return fallback;
    }

    // ==================== разбор шаблона ====================

    private record Gradient(int start, int end, int from, int to) {
    }

    static MutableComponent render(String template, String url, String linkText, String player) {
        StringBuilder text = new StringBuilder();
        List<Gradient> gradients = new ArrayList<>();
        List<int[]> links = new ArrayList<>();

        int gradStart = -1;
        int gradFrom = 0;
        int gradTo = 0;
        int last = 0;

        Matcher m = TAG.matcher(template);
        while (m.find()) {
            text.append(template, last, m.start());
            last = m.end();

            String tag = m.group(1).trim();
            String lower = tag.toLowerCase(Locale.ROOT);

            if (lower.startsWith("gradient:")) {
                String[] parts = tag.split(":");
                Integer a = parts.length >= 3 ? parseColor(parts[1]) : null;
                Integer b = parts.length >= 3 ? parseColor(parts[2]) : null;
                if (a != null && b != null) {
                    if (gradStart >= 0 && text.length() > gradStart) {
                        gradients.add(new Gradient(gradStart, text.length(), gradFrom, gradTo));
                    }
                    gradStart = text.length();
                    gradFrom = a;
                    gradTo = b;
                }
            } else if (lower.equals("/gradient")) {
                if (gradStart >= 0 && text.length() > gradStart) {
                    gradients.add(new Gradient(gradStart, text.length(), gradFrom, gradTo));
                }
                gradStart = -1;
            } else if (lower.equals("url")) {
                int s = text.length();
                text.append(linkText);
                links.add(new int[]{s, text.length()});
            } else if (lower.equals("player")) {
                text.append(player);
            }
            // остальные теги игнорируем
        }
        text.append(template.substring(last));
        if (gradStart >= 0 && text.length() > gradStart) {
            gradients.add(new Gradient(gradStart, text.length(), gradFrom, gradTo));
        }

        URI uri = null;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException ignored) {
            // без кликабельности, но текст покажем
        }

        MutableComponent result = Component.empty();
        String s = text.toString();
        int i = 0;
        while (i < s.length()) {
            int cp = s.codePointAt(i);
            Style style = Style.EMPTY;

            for (Gradient g : gradients) {
                if (i >= g.start() && i < g.end()) {
                    int span = g.end() - g.start();
                    double t = span <= 1 ? 0.0 : (double) (i - g.start()) / (span - 1);
                    style = style.withColor(TextColor.fromRgb(lerp(g.from(), g.to(), t)));
                }
            }

            for (int[] l : links) {
                if (i >= l[0] && i < l[1]) {
                    if (style.getColor() == null) {
                        style = style.withColor(ChatFormatting.AQUA);
                    }
                    style = style.withUnderlined(true);
                    if (uri != null) {
                        style = style.withClickEvent(new ClickEvent.OpenUrl(uri))
                                .withHoverEvent(new HoverEvent.ShowText(Component.literal(url)));
                    }
                }
            }

            result.append(Component.literal(new String(Character.toChars(cp))).withStyle(style));
            i += Character.charCount(cp);
        }
        return result;
    }

    private static Integer parseColor(String raw) {
        String hex = raw.startsWith("#") ? raw.substring(1) : raw;
        if (hex.length() != 6) {
            return null;
        }
        try {
            return Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int lerp(int from, int to, double t) {
        int r = (int) Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
        int g = (int) Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
        int b = (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return (r << 16) | (g << 8) | b;
    }
}