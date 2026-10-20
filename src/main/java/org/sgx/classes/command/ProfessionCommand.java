package org.sgx.classes.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.integration.LuckPermsIntegration;
import org.sgx.classes.network.ProfessionNetworking;
import org.sgx.classes.profession.BoosterTimers;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;
import org.sgx.classes.profession.SubclassType;
import org.sgx.classes.profession.SubclassUnlockAll;
import org.sgx.classes.profession.XPModifiers;
import org.sgx.classes.profession.skill.SkillAttributeSync;
import org.sgx.classes.storage.PlayerDataStore;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class ProfessionCommand {

    // --- Цвета оформления ---
    private static final ChatFormatting HEADER_COLOR = ChatFormatting.GOLD;
    private static final ChatFormatting LABEL_COLOR = ChatFormatting.GRAY;
    private static final ChatFormatting VALUE_COLOR = ChatFormatting.WHITE;
    private static final ChatFormatting ACCENT_COLOR = ChatFormatting.AQUA;
    private static final ChatFormatting GOOD_COLOR = ChatFormatting.GREEN;
    private static final ChatFormatting BAD_COLOR = ChatFormatting.RED;
    private static final ChatFormatting SUBCLASS_COLOR = ChatFormatting.LIGHT_PURPLE;

    private static final String DIVIDER_CHARS = "▬".repeat(28);

    /** Только игроки с правом администратора команд видят и могут вызывать эти ветки. */
    private static final Predicate<CommandSourceStack> IS_ADMIN =
            source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN);
    /** unlock/lock вызываются в том числе из командных блоков (у них уровень gamemaster). */
    private static final Predicate<CommandSourceStack> IS_GAMEMASTER =
            source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);

    private ProfessionCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        // /discord и /dynmap — ссылки, доступны всем
        LinkCommands.register(dispatcher);

        dispatcher.register(literal("profession")
                // /profession info — доступно всем
                .then(literal("info")
                        .executes(ProfessionCommand::info))

                // /profession set <ник> <class> — только админ
                .then(literal("set")
                        .requires(IS_ADMIN)
                        .then(argument("target", GameProfileArgument.gameProfile())
                                .then(argument("class", StringArgumentType.word())
                                        .suggests(classSuggestions())
                                        .executes(ProfessionCommand::adminSetProfession))))

                // /profession subclass <ник> <subclass> — только админ, выдаёт подкласс в обход блокировки
                .then(literal("subclass")
                        .requires(IS_ADMIN)
                        .then(argument("target", GameProfileArgument.gameProfile())
                                .then(argument("subclass", StringArgumentType.word())
                                        .suggests(subclassSuggestions())
                                        .executes(ProfessionCommand::adminSetSubclass))))

                // /profession unlock <цель> | all | @a — открыть выбор подкласса игроку / ВСЕМ (и оффлайн, и не заходившим)
                // /profession lock <цель> | all | @a — закрыть
                .then(literal("unlock")
                        .requires(IS_GAMEMASTER)
                        .then(literal("all")
                                .executes(ctx -> setSubclassLockAll(ctx, true)))
                        .then(literal("@a")
                                .executes(ctx -> setSubclassLockAll(ctx, true)))
                        .then(argument("target", GameProfileArgument.gameProfile())
                                .executes(ctx -> setSubclassLock(ctx, true))))
                .then(literal("lock")
                        .requires(IS_GAMEMASTER)
                        .then(literal("all")
                                .executes(ctx -> setSubclassLockAll(ctx, false)))
                        .then(literal("@a")
                                .executes(ctx -> setSubclassLockAll(ctx, false)))
                        .then(argument("target", GameProfileArgument.gameProfile())
                                .executes(ctx -> setSubclassLock(ctx, false))))

                // /profession level add|remove <ник> <число>
                .then(literal("level")
                        .requires(IS_ADMIN)
                        .then(literal("add")
                                .then(argument("target", GameProfileArgument.gameProfile())
                                        .then(argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> changeLevel(ctx, true)))))
                        .then(literal("remove")
                                .then(argument("target", GameProfileArgument.gameProfile())
                                        .then(argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> changeLevel(ctx, false))))))

                // /profession xp add|remove <ник> <число>
                .then(literal("xp")
                        .requires(IS_ADMIN)
                        .then(literal("add")
                                .then(argument("target", GameProfileArgument.gameProfile())
                                        .then(argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> changeXp(ctx, true)))))
                        .then(literal("remove")
                                .then(argument("target", GameProfileArgument.gameProfile())
                                        .then(argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> changeXp(ctx, false))))))

                // /profession boosters — админ: все активные бустеры (глобальный и личные)
                .then(literal("boosters")
                        .requires(IS_ADMIN)
                        .executes(ProfessionCommand::boostersInfo))

                // /profession admin <ник> — просмотр инфы об одном игроке
                // /profession admin @a [страница] (или all) — список всех, кто заходил на сервер (онлайн и оффлайн), книгой по 3 на страницу
                .then(literal("admin")
                        .requires(IS_ADMIN)
                        .then(AdminPlayerList.node("@a"))
                        .then(AdminPlayerList.node("all"))
                        .then(argument("target", GameProfileArgument.gameProfile())
                                .executes(ProfessionCommand::adminInfo)))

                // /profession globalXP set <множитель> [время] | return — только админ
                .then(literal("globalXP")
                        .requires(IS_ADMIN)
                        .then(literal("set")
                                .then(argument("multiplier", DoubleArgumentType.doubleArg(0.0))
                                        .executes(ctx -> globalXpSet(ctx, null))
                                        .then(argument("duration", StringArgumentType.word())
                                                .suggests(durationSuggestions())
                                                .executes(ctx -> globalXpSet(ctx, StringArgumentType.getString(ctx, "duration"))))))
                        .then(literal("return")
                                .executes(ProfessionCommand::globalXpReturn)))

                // /profession localXP <ник> set <множитель> [время] | <ник> return — только админ
                .then(literal("localXP")
                        .requires(IS_ADMIN)
                        .then(argument("target", GameProfileArgument.gameProfile())
                                .then(literal("set")
                                        .then(argument("multiplier", DoubleArgumentType.doubleArg(0.0))
                                                .executes(ctx -> localXpSet(ctx, null))
                                                .then(argument("duration", StringArgumentType.word())
                                                        .suggests(durationSuggestions())
                                                        .executes(ctx -> localXpSet(ctx, StringArgumentType.getString(ctx, "duration"))))))
                                .then(literal("return")
                                        .executes(ProfessionCommand::localXpReturn)))));
    }

    private static SuggestionProvider<CommandSourceStack> classSuggestions() {
        return (ctx, builder) -> {
            for (ProfessionType type : ProfessionType.values()) {
                builder.suggest(type.getSerializedName());
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> subclassSuggestions() {
        return (ctx, builder) -> {
            for (SubclassType type : SubclassType.values()) {
                builder.suggest(type.getSerializedName());
            }
            return builder.buildFuture();
        };
    }

    private static SuggestionProvider<CommandSourceStack> durationSuggestions() {
        return (ctx, builder) -> {
            for (String s : new String[]{"30m", "1h", "6h", "24h", "3d"}) {
                builder.suggest(s);
            }
            return builder.buildFuture();
        };
    }

    /** Сохранить данные, пересчитать бонусы навыков и отправить клиенту актуальное состояние. */
    private static void applyAndSync(ServerPlayer target, ProfessionData data) {
        SkillAttributeSync.reapply(target, data);
        PlayerDataStore.save(target);
        ProfessionNetworking.syncToClient(target);
    }

    // ==================== /profession info ====================

    private static int info(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ProfessionData data = ModAttachments.get(player);
        ProfessionType current = data.currentProfession();

        if (current == ProfessionType.NONE) {
            source.sendSuccess(() -> errorLine("Класс не выбран."), false);
            return 1;
        }

        int level = data.level();
        int xp = data.xp();
        int needed = current.xpToNextLevel(level);
        int remaining = data.isMaxLevel() ? 0 : Math.max(0, needed - xp);

        source.sendSuccess(ProfessionCommand::divider, false);
        source.sendSuccess(() -> header("Ваша профессия"), false);
        source.sendSuccess(ProfessionCommand::divider, false);
        source.sendSuccess(() -> line("Класс", current.getSerializedName(), ACCENT_COLOR), false);
        source.sendSuccess(() -> line("Уровень", level + " / " + ProfessionType.MAX_LEVEL, GOOD_COLOR), false);
        source.sendSuccess(() -> line("Опыт",
                data.isMaxLevel() ? "МАКСИМУМ" : xp + " / " + needed + " (осталось " + remaining + ")",
                VALUE_COLOR), false);

        if (data.xpBlockedBySubclassGate()) {
            source.sendSuccess(() -> Component.literal("Опыт не копится, пока не открыт выбор подкласса.")
                    .withStyle(BAD_COLOR, ChatFormatting.ITALIC), false);
        }

        if (data.currentSubclass() != null) {
            source.sendSuccess(() -> line("Подкласс", data.currentSubclass().getSerializedName(), SUBCLASS_COLOR), false);
        } else if (data.needsSubclassSelection()) {
            source.sendSuccess(() -> errorLine("Подкласс не выбран."), false);
            if (data.subclassUnlocked()) {
                source.sendSuccess(() -> Component.literal("Выбор подкласса открыт — выберите его в дереве навыков.")
                        .withStyle(GOOD_COLOR, ChatFormatting.ITALIC), false);
            } else {
                source.sendSuccess(() -> Component.literal("Выбор подкласса пока закрыт.")
                        .withStyle(LABEL_COLOR, ChatFormatting.ITALIC), false);
            }
        }
        source.sendSuccess(ProfessionCommand::divider, false);
        return 1;
    }

    // ==================== /profession set <ник> <class> ====================

    private static int adminSetProfession(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        GameProfile profile = firstProfile(ctx);
        String raw = StringArgumentType.getString(ctx, "class");

        ServerPlayer target = onlinePlayer(source, profile);
        if (target == null) {
            source.sendFailure(errorLine(profile.name() + " сейчас не в сети. Игрок должен быть онлайн."));
            return 0;
        }

        for (ProfessionType type : ProfessionType.values()) {
            if (type.getSerializedName().equals(raw)) {
                ProfessionData data = ModAttachments.get(target);
                data.setCurrentProfession(type);
                applyAndSync(target, data);
                LuckPermsIntegration.syncGroup(target, data); // так же, как при выборе класса через GUI
                org.sgx.classes.profession.ClassAdvancements.sync(target, data);

                String targetName = profile.name();
                source.sendSuccess(() -> successLine(targetName + ": класс изменён на " + type.getSerializedName()), true);
                target.sendSystemMessage(Component.literal("Администратор изменил ваш класс на: ")
                        .withStyle(LABEL_COLOR)
                        .append(Component.literal(type.getSerializedName()).withStyle(ACCENT_COLOR, ChatFormatting.BOLD)));
                return 1;
            }
        }

        source.sendFailure(errorLine("Неизвестный класс: " + raw));
        return 0;
    }

    // ==================== /profession subclass <ник> <subclass> ====================

    private static int adminSetSubclass(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        GameProfile profile = firstProfile(ctx);
        String raw = StringArgumentType.getString(ctx, "subclass");

        ServerPlayer target = onlinePlayer(source, profile);
        if (target == null) {
            source.sendFailure(errorLine(profile.name() + " сейчас не в сети. Игрок должен быть онлайн."));
            return 0;
        }

        ProfessionData data = ModAttachments.get(target);

        for (SubclassType type : SubclassType.values()) {
            if (type.getSerializedName().equals(raw)) {
                if (data.chooseSubclass(type, true)) { // админ обходит блокировку выбора
                    applyAndSync(target, data);

                    String targetName = profile.name();
                    source.sendSuccess(() -> successLine(targetName + ": подкласс изменён на " + type.getSerializedName()), true);
                    target.sendSystemMessage(Component.literal("Администратор назначил вам подкласс: ")
                            .withStyle(LABEL_COLOR)
                            .append(Component.literal(type.getSerializedName()).withStyle(SUBCLASS_COLOR, ChatFormatting.BOLD)));
                    return 1;
                } else {
                    source.sendFailure(errorLine(profile.name() + ": подкласс не подходит — нужен класс "
                            + type.parent().getSerializedName() + ", уровень "
                            + ProfessionData.SUBCLASS_BRANCH_LEVEL + "+ и ещё не выбранный подкласс."));
                    return 0;
                }
            }
        }

        source.sendFailure(errorLine("Неизвестный подкласс: " + raw));
        return 0;
    }

    // ==================== /profession unlock | lock <цель> ====================

    private static int setSubclassLock(CommandContext<CommandSourceStack> ctx, boolean unlock) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        int changed = 0;
        for (var p : GameProfileArgument.getGameProfiles(ctx, "target")) {
            ServerPlayer target = source.getServer().getPlayerList().getPlayer(p.id());
            if (target == null) {
                source.sendFailure(errorLine(p.name() + " сейчас не в сети. Игрок должен быть онлайн."
                        + " Чтобы открыть всем сразу, включая оффлайн: /profession unlock all"));
                continue;
            }
            ProfessionData data = ModAttachments.get(target);
            if (data.subclassUnlocked() == unlock) {
                continue; // уже в нужном состоянии — не спамим
            }
            data.setSubclassUnlocked(unlock);
            PlayerDataStore.save(target);
            ProfessionNetworking.syncToClient(target); // без этого клиент видел старое состояние до перезахода

            if (unlock) {
                target.sendSystemMessage(Component.literal("Вам открыт выбор подкласса! Загляните в дерево навыков.")
                        .withStyle(SUBCLASS_COLOR, ChatFormatting.BOLD));
            } else {
                target.sendSystemMessage(Component.literal("Выбор подкласса снова закрыт.").withStyle(LABEL_COLOR));
            }
            String name = p.name();
            source.sendSuccess(() -> successLine(name + ": выбор подкласса " + (unlock ? "открыт" : "закрыт")), true);
            changed++;
        }
        return changed;
    }

    // ==================== /profession unlock | lock all ====================

    private static int setSubclassLockAll(CommandContext<CommandSourceStack> ctx, boolean unlock) {
        CommandSourceStack source = ctx.getSource();
        int changed = unlock
                ? SubclassUnlockAll.unlockAll(source.getServer())
                : SubclassUnlockAll.lockAll(source.getServer());
        source.sendSuccess(() -> successLine("Выбор подкласса " + (unlock ? "открыт" : "закрыт")
                + " для ВСЕХ игроков (в сети изменено: " + changed
                + "; оффлайн и новые игроки — при входе)"), true);
        return 1;
    }

    // ==================== /profession level add|remove ====================

    private static int changeLevel(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        GameProfile profile = firstProfile(ctx);
        int amount = IntegerArgumentType.getInteger(ctx, "amount");

        ServerPlayer target = onlinePlayer(source, profile);
        if (target == null) {
            source.sendFailure(errorLine(profile.name() + " сейчас не в сети. Игрок должен быть онлайн."));
            return 0;
        }
        ProfessionData data = ModAttachments.get(target);
        if (data.currentProfession() == ProfessionType.NONE) {
            source.sendFailure(errorLine(profile.name() + ": класс не выбран."));
            return 0;
        }

        int before = data.level();
        if (add) {
            data.addLevel(amount);
        } else {
            data.removeLevel(amount);
        }
        int after = data.level();

        if (after == before) {
            source.sendFailure(errorLine(profile.name() + ": уровень не изменился (" + before + ")."));
            return 0;
        }

        applyAndSync(target, data);
        String targetName = profile.name();
        source.sendSuccess(() -> successLine(targetName + ": уровень " + before + " → " + after), true);
        target.sendSystemMessage(Component.literal("Администратор изменил ваш уровень: ")
                .withStyle(LABEL_COLOR)
                .append(Component.literal(before + " → " + after).withStyle(ACCENT_COLOR, ChatFormatting.BOLD)));
        return 1;
    }

    // ==================== /profession xp add|remove ====================

    private static int changeXp(CommandContext<CommandSourceStack> ctx, boolean add) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        GameProfile profile = firstProfile(ctx);
        int amount = IntegerArgumentType.getInteger(ctx, "amount");

        ServerPlayer target = onlinePlayer(source, profile);
        if (target == null) {
            source.sendFailure(errorLine(profile.name() + " сейчас не в сети. Игрок должен быть онлайн."));
            return 0;
        }
        ProfessionData data = ModAttachments.get(target);
        if (data.currentProfession() == ProfessionType.NONE) {
            source.sendFailure(errorLine(profile.name() + ": класс не выбран."));
            return 0;
        }

        int levelBefore = data.level();
        int xpBefore = data.xp();
        if (add) {
            data.addXp(amount); // ровно столько, без множителей бустеров и колец
        } else {
            data.removeXp(amount);
        }
        int levelAfter = data.level();
        int xpAfter = data.xp();

        if (levelAfter == levelBefore && xpAfter == xpBefore) {
            if (add && data.xpBlockedBySubclassGate()) {
                source.sendFailure(errorLine(profile.name() + ": опыт не засчитывается — не открыт выбор подкласса "
                        + "(/profession unlock " + profile.name() + ")."));
            } else {
                source.sendFailure(errorLine(profile.name() + ": опыт не изменился."));
            }
            return 0;
        }

        applyAndSync(target, data);
        String targetName = profile.name();
        String change = "ур. " + levelBefore + " (" + xpBefore + " xp) → ур. " + levelAfter + " (" + xpAfter + " xp)";
        source.sendSuccess(() -> successLine(targetName + ": " + change), true);
        target.sendSystemMessage(Component.literal("Администратор изменил ваш опыт: ")
                .withStyle(LABEL_COLOR)
                .append(Component.literal(change).withStyle(ACCENT_COLOR, ChatFormatting.BOLD)));
        return 1;
    }

    // ==================== /profession boosters ====================

    private static int boostersInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        source.sendSuccess(ProfessionCommand::divider, false);
        source.sendSuccess(() -> header("Бустеры опыта класса"), false);
        source.sendSuccess(ProfessionCommand::divider, false);

        // --- глобальный ---
        double global = XPModifiers.globalMultiplier();
        long globalLeft = BoosterTimers.remainingGlobal();
        if (global == XPModifiers.DEFAULT_MULTIPLIER && globalLeft < 0) {
            source.sendSuccess(() -> line("Глобальный", "нет", LABEL_COLOR), false);
        } else {
            String time = globalLeft >= 0
                    ? " (осталось " + BoosterTimers.formatDuration(globalLeft) + ")"
                    : " (без таймера)";
            String globalText = "x" + formatMultiplier(global) + time;
            source.sendSuccess(() -> line("Глобальный", globalText, ACCENT_COLOR), false);
        }

        // --- личные ---
        Map<UUID, Double> multipliers = XPModifiers.allLocalMultipliers();
        Map<UUID, Long> timers = BoosterTimers.localRemainingSnapshot();
        Set<UUID> all = new LinkedHashSet<>(multipliers.keySet());
        all.addAll(timers.keySet());

        if (all.isEmpty()) {
            source.sendSuccess(() -> line("Личные", "нет", LABEL_COLOR), false);
        } else {
            source.sendSuccess(() -> Component.literal("Личные:").withStyle(LABEL_COLOR), false);
            for (UUID uuid : all) {
                ServerPlayer online = source.getServer().getPlayerList().getPlayer(uuid);
                String name = playerNameFor(source, uuid, online);

                String mult = multipliers.containsKey(uuid)
                        ? "x" + formatMultiplier(multipliers.get(uuid))
                        : "множитель не загружен";

                Long left = timers.get(uuid);
                String time;
                if (left == null) {
                    time = " (без таймера)";
                } else if (left == 0L) {
                    time = online != null ? " (истекает)" : " (истёк, снимется при входе игрока)";
                } else {
                    time = " (осталось " + BoosterTimers.formatDuration(left) + ")";
                }

                Component row = Component.literal(" • ").withStyle(LABEL_COLOR)
                        .append(Component.literal(name).withStyle(HEADER_COLOR))
                        .append(Component.literal(online != null ? " [online]" : " [offline]")
                                .withStyle(online != null ? GOOD_COLOR : LABEL_COLOR))
                        .append(Component.literal(": " + mult + time).withStyle(VALUE_COLOR));
                source.sendSuccess(() -> row, false);
            }
        }

        source.sendSuccess(ProfessionCommand::divider, false);
        return 1;
    }

    private static String playerNameFor(CommandSourceStack source, UUID uuid, ServerPlayer online) {
        if (online != null) {
            return online.getName().getString();
        }
        PlayerDataStore.Info info = PlayerDataStore.load(source.getServer(), uuid);
        return info != null ? info.name() : uuid.toString();
    }

    // ==================== /profession admin <ник> ====================

    private static int adminInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        GameProfile profile = firstProfile(ctx);
        UUID uuid = profile.id();
        String name = profile.name();

        ServerPlayer online = onlinePlayer(source, profile);

        source.sendSuccess(ProfessionCommand::divider, false);
        if (online != null) {
            ProfessionData data = ModAttachments.get(online);
            printAdminOnline(source, name, data);
        } else {
            PlayerDataStore.Info playerInfo = PlayerDataStore.load(source.getServer(), uuid);
            if (playerInfo == null) {
                source.sendFailure(errorLine(name + ": данных о классе не найдено "
                        + "(не заходил после установки мода / не выбирал класс)."));
                return 0;
            }
            printAdminOffline(source, playerInfo);
        }
        printLocalXpLine(source, uuid);
        source.sendSuccess(ProfessionCommand::divider, false);
        return 1;
    }

    private static void printAdminOnline(CommandSourceStack source, String name, ProfessionData data) {
        source.sendSuccess(() -> playerHeader(name, true), false);
        source.sendSuccess(ProfessionCommand::divider, false);

        ProfessionType current = data.currentProfession();
        if (current == ProfessionType.NONE) {
            source.sendSuccess(() -> errorLine("Класс не выбран."), false);
            return;
        }

        int level = data.level();
        int xp = data.xp();
        int needed = current.xpToNextLevel(level);
        int remaining = data.isMaxLevel() ? 0 : Math.max(0, needed - xp);

        source.sendSuccess(() -> line("Класс", current.getSerializedName(), ACCENT_COLOR), false);
        source.sendSuccess(() -> line("Уровень", level + " / " + ProfessionType.MAX_LEVEL, GOOD_COLOR), false);
        source.sendSuccess(() -> line("Опыт",
                data.isMaxLevel() ? "МАКСИМУМ" : xp + " / " + needed + " (осталось " + remaining + ")",
                VALUE_COLOR), false);
        if (data.xpBlockedBySubclassGate()) {
            source.sendSuccess(() -> line("Набор опыта", "заблокирован (нет доступа к подклассу)", BAD_COLOR), false);
        }
        source.sendSuccess(() -> line("Подкласс",
                data.currentSubclass() != null ? data.currentSubclass().getSerializedName() : "не выбран",
                data.currentSubclass() != null ? SUBCLASS_COLOR : BAD_COLOR), false);
        source.sendSuccess(() -> line("Выбор подкласса",
                data.subclassUnlocked() ? "открыт" : "закрыт",
                data.subclassUnlocked() ? GOOD_COLOR : BAD_COLOR), false);
    }

    private static void printAdminOffline(CommandSourceStack source, PlayerDataStore.Info info) {
        source.sendSuccess(() -> playerHeader(info.name(), false), false);
        source.sendSuccess(ProfessionCommand::divider, false);
        source.sendSuccess(() -> line("Класс", info.profession(), ACCENT_COLOR), false);
        source.sendSuccess(() -> line("Уровень", info.level() + " / " + ProfessionType.MAX_LEVEL, GOOD_COLOR), false);
        source.sendSuccess(() -> line("Опыт", info.xp() + " (до след. ур.: " + info.xpToNextLevel() + ")", VALUE_COLOR), false);
        source.sendSuccess(() -> line("Подкласс",
                info.subclass() != null ? info.subclass() : "не выбран",
                info.subclass() != null ? SUBCLASS_COLOR : BAD_COLOR), false);
    }

    private static void printLocalXpLine(CommandSourceStack source, UUID uuid) {
        if (XPModifiers.hasLocalMultiplier(uuid)) {
            long left = BoosterTimers.remainingLocal(uuid);
            String suffix = left >= 0 ? " (осталось " + BoosterTimers.formatDuration(left) + ")" : "";
            source.sendSuccess(() -> line("Личный множитель XP",
                    "x" + formatMultiplier(XPModifiers.localMultiplier(uuid)) + suffix, ChatFormatting.YELLOW), false);
        }
    }

    // ==================== /profession globalXP ====================

    private static int globalXpSet(CommandContext<CommandSourceStack> ctx, String rawDuration) {
        CommandSourceStack source = ctx.getSource();
        double multiplier = DoubleArgumentType.getDouble(ctx, "multiplier");

        long durationMs = BoosterTimers.DEFAULT_MS;
        if (rawDuration != null) {
            var parsed = BoosterTimers.parseDuration(rawDuration);
            if (parsed.isEmpty()) {
                source.sendFailure(errorLine("Неверное время: " + rawDuration + ". Примеры: 30m, 2h, 1d"));
                return 0;
            }
            durationMs = parsed.getAsLong();
        }

        BoosterTimers.setGlobal(source.getServer(), multiplier, durationMs);

        String timeText = BoosterTimers.formatDuration(durationMs);
        source.sendSuccess(ProfessionCommand::divider, true);
        source.sendSuccess(() -> Component.literal("Глобальный множитель опыта класса: ")
                .withStyle(LABEL_COLOR)
                .append(Component.literal("x" + formatMultiplier(multiplier)).withStyle(ACCENT_COLOR, ChatFormatting.BOLD))
                .append(Component.literal(" на " + timeText).withStyle(LABEL_COLOR)), true);
        source.sendSuccess(() -> Component.literal("Действует на всех игроков.")
                .withStyle(LABEL_COLOR, ChatFormatting.ITALIC), true);
        source.sendSuccess(ProfessionCommand::divider, true);
        return 1;
    }

    private static int globalXpReturn(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();

        XPModifiers.resetGlobalMultiplier(source.getServer());
        BoosterTimers.clearGlobal();

        source.sendSuccess(ProfessionCommand::divider, true);
        source.sendSuccess(() -> Component.literal("Глобальный множитель опыта класса сброшен: ")
                .withStyle(LABEL_COLOR)
                .append(Component.literal("x" + formatMultiplier(XPModifiers.DEFAULT_MULTIPLIER))
                        .withStyle(GOOD_COLOR, ChatFormatting.BOLD)), true);
        source.sendSuccess(ProfessionCommand::divider, true);
        return 1;
    }

    // ==================== /profession localXP ====================

    private static int localXpSet(CommandContext<CommandSourceStack> ctx, String rawDuration) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        GameProfile profile = firstProfile(ctx);
        double multiplier = DoubleArgumentType.getDouble(ctx, "multiplier");

        long durationMs = BoosterTimers.DEFAULT_MS;
        if (rawDuration != null) {
            var parsed = BoosterTimers.parseDuration(rawDuration);
            if (parsed.isEmpty()) {
                source.sendFailure(errorLine("Неверное время: " + rawDuration + ". Примеры: 30m, 2h, 1d"));
                return 0;
            }
            durationMs = parsed.getAsLong();
        }

        XPModifiers.setLocalMultiplier(profile.id(), multiplier);
        BoosterTimers.setLocal(profile.id(), durationMs);

        String timeText = BoosterTimers.formatDuration(durationMs);
        ServerPlayer online = onlinePlayer(source, profile);
        if (online != null) {
            ModAttachments.get(online).setLocalXpMultiplier(multiplier);
            PlayerDataStore.save(online);
            ProfessionNetworking.syncToClient(online);
            online.sendSystemMessage(Component.literal("Ваш личный множитель опыта класса: ")
                    .withStyle(LABEL_COLOR)
                    .append(Component.literal("x" + formatMultiplier(multiplier)).withStyle(ACCENT_COLOR, ChatFormatting.BOLD))
                    .append(Component.literal(" на " + timeText).withStyle(LABEL_COLOR)));
        }

        String targetName = profile.name();
        source.sendSuccess(() -> successLine(targetName + ": личный множитель x" + formatMultiplier(multiplier)
                + " на " + timeText), true);
        return 1;
    }

    private static int localXpReturn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        GameProfile profile = firstProfile(ctx);

        XPModifiers.resetLocalMultiplier(profile.id());

        ServerPlayer online = onlinePlayer(source, profile);
        if (online != null) {
            ModAttachments.get(online).setLocalXpMultiplier(XPModifiers.DEFAULT_MULTIPLIER);
            BoosterTimers.clearLocal(profile.id());
            PlayerDataStore.save(online);
            ProfessionNetworking.syncToClient(online);
            online.sendSystemMessage(Component.literal("Ваш личный множитель опыта класса сброшен администратором.")
                    .withStyle(LABEL_COLOR));
        } else {
            BoosterTimers.forceExpireLocal(profile.id()); // сотрётся из данных игрока при следующем заходе
        }

        String targetName = profile.name();
        source.sendSuccess(() -> successLine(targetName + ": личный множитель опыта сброшен до x"
                + formatMultiplier(XPModifiers.DEFAULT_MULTIPLIER)), true);
        return 1;
    }

    // ==================== ВСПОМОГАТЕЛЬНОЕ ====================

    private static GameProfile firstProfile(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        var profiles = GameProfileArgument.getGameProfiles(ctx, "target");
        var first = profiles.iterator().next();
        return new GameProfile(first.id(), first.name());
    }

    private static ServerPlayer onlinePlayer(CommandSourceStack source, GameProfile profile) {
        return source.getServer().getPlayerList().getPlayer(profile.id());
    }

    private static String formatMultiplier(double value) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    private static Component divider() {
        return Component.literal(DIVIDER_CHARS).withStyle(HEADER_COLOR, ChatFormatting.BOLD);
    }

    private static Component header(String title) {
        return Component.literal(title).withStyle(HEADER_COLOR, ChatFormatting.BOLD);
    }

    private static Component playerHeader(String name, boolean online) {
        Component status = online
                ? Component.literal(" [online]").withStyle(GOOD_COLOR)
                : Component.literal(" [offline]").withStyle(LABEL_COLOR);
        return Component.literal(name).withStyle(HEADER_COLOR, ChatFormatting.BOLD).append(status);
    }

    private static Component line(String label, String value, ChatFormatting valueColor) {
        return Component.literal(label + ": ").withStyle(LABEL_COLOR)
                .append(Component.literal(value).withStyle(valueColor));
    }

    private static Component successLine(String text) {
        return Component.literal("✔ " + text).withStyle(GOOD_COLOR);
    }

    private static Component errorLine(String text) {
        return Component.literal("✘ " + text).withStyle(BAD_COLOR);
    }
}