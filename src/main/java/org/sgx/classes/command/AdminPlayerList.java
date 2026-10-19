package org.sgx.classes.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.storage.LevelResource;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;
import org.sgx.classes.storage.PlayerDataStore;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /profession admin @a — список ВСЕХ игроков, которые хоть раз заходили на сервер
 * (онлайн и оффлайн, в том числе без выбранного класса).
 * <p>
 * Игроку открывается виртуальная книга: PAGE_SIZE игроков на страницу, листание встроенными
 * стрелками книги — страница заменяется страницей, чат не засоряется.
 * Из консоли / командного блока (нет игрока) выводится обычный текст в чат: /profession admin @a [страница].
 */
final class AdminPlayerList {

    static final int PAGE_SIZE = 3;
    /** Лимит страниц у written_book в ваниле. */
    private static final int BOOK_MAX_PAGES = 100;

    private static final ChatFormatting HEADER = ChatFormatting.GOLD;
    private static final ChatFormatting LABEL = ChatFormatting.GRAY;
    private static final ChatFormatting VALUE = ChatFormatting.WHITE;
    private static final ChatFormatting ACCENT = ChatFormatting.AQUA;
    private static final ChatFormatting GOOD = ChatFormatting.GREEN;
    private static final ChatFormatting SUBCLASS = ChatFormatting.LIGHT_PURPLE;
    private static final String DIVIDER = "▬".repeat(28);

    /** profession == null — игрок заходил, но класс не выбирал. */
    private record Row(String name, boolean online, String profession, int level, int xp, int needed,
                       String subclass) {
        boolean hasClass() {
            return profession != null;
        }
    }

    private AdminPlayerList() {
    }

    /** Ветка вида: admin &lt;name&gt; [страница]. Регистрируется под "@a" и "all". */
    static LiteralArgumentBuilder<CommandSourceStack> node(String name) {
        return literal(name)
                .executes(ctx -> run(ctx, 1))
                .then(argument("page", IntegerArgumentType.integer(1))
                        .executes(ctx -> run(ctx, IntegerArgumentType.getInteger(ctx, "page"))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx, int page) {
        CommandSourceStack source = ctx.getSource();
        List<Row> rows = collect(source.getServer());

        if (rows.isEmpty()) {
            source.sendFailure(Component.literal("✘ Игроков не найдено.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        if (player != null) {
            openBook(player, rows);
        } else {
            showInChat(source, rows, page);
        }
        return 1;
    }

    // ==================== книга ====================

    private static void openBook(ServerPlayer player, List<Row> rows) {
        List<Filterable<Component>> pages = new ArrayList<>();
        int total = rows.size();
        for (int from = 0; from < total && pages.size() < BOOK_MAX_PAGES; from += PAGE_SIZE) {
            int to = Math.min(total, from + PAGE_SIZE);
            pages.add(Filterable.passThrough(bookPage(rows.subList(from, to))));
        }

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("Игроки"), "Сервер", 0, pages, true));

        // Клиент открывает книгу из предмета в руке, поэтому на миг подставляем её в руку и сразу возвращаем прежний предмет.
        // broadcastChanges() шлёт изменения слота немедленно, так что порядок пакетов: книга -> открыть -> прежний предмет.
        InteractionHand hand = InteractionHand.MAIN_HAND;
        ItemStack previous = player.getItemInHand(hand);
        player.setItemInHand(hand, book);
        player.containerMenu.broadcastChanges();
        player.openItemGui(book, hand);
        player.setItemInHand(hand, previous);
        player.containerMenu.broadcastChanges();
    }

    /** Одна страница книги — до PAGE_SIZE игроков (бумага светлая, поэтому цвета тёмные). */
    private static Component bookPage(List<Row> rows) {
        MutableComponent page = Component.empty();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            if (i > 0) {
                page.append("\n");
            }
            page.append(Component.literal("● ").withStyle(row.online() ? ChatFormatting.DARK_GREEN : ChatFormatting.GRAY));
            page.append(Component.literal(row.name()).withStyle(ChatFormatting.BLACK, ChatFormatting.BOLD));
            page.append("\n");

            if (!row.hasClass()) {
                page.append(Component.literal("класс не выбран").withStyle(ChatFormatting.DARK_RED));
                page.append("\n");
                continue;
            }

            page.append(Component.literal(row.profession()).withStyle(ChatFormatting.DARK_AQUA));
            page.append(Component.literal(" ур." + row.level() + "/" + ProfessionType.MAX_LEVEL)
                    .withStyle(ChatFormatting.DARK_GREEN));
            page.append("\n");
            page.append(Component.literal("оп. " + (row.needed() < 0 ? "МАКС" : row.xp() + "/" + row.needed()))
                    .withStyle(ChatFormatting.BLACK));
            page.append("\n");
            page.append(Component.literal("под: ").withStyle(ChatFormatting.DARK_GRAY));
            page.append(row.subclass() != null
                    ? Component.literal(row.subclass()).withStyle(ChatFormatting.DARK_PURPLE)
                    : Component.literal("не выбран").withStyle(ChatFormatting.DARK_RED));
        }
        return page;
    }

    // ==================== вывод в чат (консоль) ====================

    private static void showInChat(CommandSourceStack source, List<Row> rows, int requestedPage) {
        int pages = (rows.size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int page = Math.max(1, Math.min(requestedPage, pages));
        int from = (page - 1) * PAGE_SIZE;
        int to = Math.min(rows.size(), from + PAGE_SIZE);

        source.sendSuccess(AdminPlayerList::divider, false);
        source.sendSuccess(() -> Component.literal("Игроки ")
                .withStyle(HEADER, ChatFormatting.BOLD)
                .append(Component.literal("(всего " + rows.size() + ", стр. " + page + " / " + pages + ")")
                        .withStyle(LABEL)), false);
        source.sendSuccess(AdminPlayerList::divider, false);

        for (Row row : rows.subList(from, to)) {
            source.sendSuccess(() -> nameLine(row), false);
            source.sendSuccess(() -> detailLine(row), false);
        }
        source.sendSuccess(AdminPlayerList::divider, false);
    }

    private static Component nameLine(Row row) {
        return Component.literal(" ▪ ").withStyle(LABEL)
                .append(Component.literal(row.name()).withStyle(HEADER, ChatFormatting.BOLD))
                .append(Component.literal(row.online() ? " [online]" : " [offline]")
                        .withStyle(row.online() ? GOOD : LABEL));
    }

    private static Component detailLine(Row row) {
        MutableComponent c = Component.literal("    ");
        if (!row.hasClass()) {
            return c.append(Component.literal("Класс не выбран").withStyle(ChatFormatting.RED));
        }
        c.append(pair("Класс", row.profession(), ACCENT));
        c.append(Component.literal(" · ").withStyle(LABEL));
        c.append(pair("Ур.", row.level() + "/" + ProfessionType.MAX_LEVEL, GOOD));
        c.append(Component.literal(" · ").withStyle(LABEL));
        c.append(pair("Опыт", row.needed() < 0 ? "МАКС" : row.xp() + "/" + row.needed(), VALUE));
        c.append(Component.literal(" · ").withStyle(LABEL));
        c.append(pair("Подкласс", row.subclass() != null ? row.subclass() : "не выбран",
                row.subclass() != null ? SUBCLASS : ChatFormatting.RED));
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

    private static List<Row> collect(MinecraftServer server) {
        List<Row> rows = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();

        // Онлайн — берём живые данные (в том числе тех, кто ещё не выбрал класс)
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            seen.add(online.getGameProfile().id());
            ProfessionData data = ModAttachments.get(online);
            String name = online.getName().getString();
            if (data.currentProfession() == ProfessionType.NONE) {
                rows.add(new Row(name, true, null, 0, 0, -1, null));
                continue;
            }
            int needed = data.isMaxLevel() ? -1 : data.currentProfession().xpToNextLevel(data.level());
            rows.add(new Row(name, true,
                    data.currentProfession().getSerializedName(), data.level(), data.xp(), needed,
                    data.currentSubclass() != null ? data.currentSubclass().getSerializedName() : null));
        }

        // Оффлайн — перебираем всех, кто когда-либо заходил на сервер (по файлам игроков в мире)
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
                            boolean noClass = info == null || info.profession() == null
                                    || info.profession().equals(ProfessionType.NONE.getSerializedName());
                            if (noClass) {
                                String name = info != null && info.name() != null
                                        ? info.name() : lookupName(server, id);
                                rows.add(new Row(name, false, null, 0, 0, -1, null));
                                return;
                            }
                            int needed = info.level() >= ProfessionType.MAX_LEVEL ? -1 : info.xpToNextLevel();
                            rows.add(new Row(info.name(), false, info.profession(), info.level(), info.xp(),
                                    needed, info.subclass()));
                        });
            } catch (IOException ignored) {
                // каталог недоступен — покажем хотя бы онлайн
            }
        }

        // онлайн сверху; внутри — с классом выше, потом по уровню и имени
        rows.sort(Comparator.comparing(Row::online).reversed()
                .thenComparing(Row::hasClass, Comparator.reverseOrder())
                .thenComparing(Comparator.comparingInt(Row::level).reversed())
                .thenComparing(Row::name, String.CASE_INSENSITIVE_ORDER));
        return rows;
    }

    /** Ник по UUID из кэша сервера (usercache); если не нашли — короткий UUID. */
    private static String lookupName(MinecraftServer server, UUID id) {
        try {
            return server.services().nameToIdCache().get(id).map(n -> n.name()).orElse(shortId(id));
        } catch (RuntimeException e) {
            return shortId(id);
        }
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }
}