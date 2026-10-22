package org.sgx.classes.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import org.sgx.classes.profession.DurabilityNormalizer;

import java.util.ArrayList;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /profession fixdurability players        — инвентари и эндер-сундуки всех игроков онлайн.
 * /profession fixdurability area <радиус>  — все контейнеры в квадрате чанков вокруг админа
 *                                            (радиус в чанках, 1..200), в текущем измерении.
 * /profession fixdurability stop           — остановить текущий обход.
 *
 * Обход идёт частями по CHUNKS_PER_TICK чанков за тик, чтобы не вешать сервер. Чанки, которых ещё нет
 * в памяти, подгружаются с диска (уже сгенерированные), контейнеры правятся и чанк помечается к сохранению.
 */
public final class DurabilityFixCommand {

    private static final int MAX_RADIUS = 500;
    private static final int CHUNKS_PER_TICK = 4;

    private static Job job;

    private DurabilityFixCommand() {
    }

    private static final class Job {
        final CommandSourceStack source;
        final ServerLevel level;
        final int centerX;
        final int centerZ;
        final int radius;
        final long total;
        long index;
        int fixedItems;

        Job(CommandSourceStack source, ServerLevel level, int centerX, int centerZ, int radius) {
            this.source = source;
            this.level = level;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.radius = radius;
            long width = 2L * radius + 1;
            this.total = width * width;
        }
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> tick());
    }

    public static LiteralArgumentBuilder<CommandSourceStack> node() {
        return literal("fixdurability")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_ADMIN))
                .then(literal("players").executes(ctx -> {
                    int fixed = 0;
                    for (ServerPlayer player : ctx.getSource().getServer().getPlayerList().getPlayers()) {
                        fixed += DurabilityNormalizer.normalizePlayer(player);
                    }
                    int result = fixed;
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "Прочность исправлена у предметов игроков онлайн: " + result), true);
                    return fixed;
                }))
                .then(literal("area").then(argument("radius", IntegerArgumentType.integer(1, MAX_RADIUS))
                        .executes(ctx -> startArea(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "radius")))))
                .then(literal("stop").executes(ctx -> {
                    if (job == null) {
                        ctx.getSource().sendSuccess(() -> Component.literal("Обход не запущен."), false);
                        return 0;
                    }
                    Job stopped = job;
                    job = null;
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            "Обход остановлен. Исправлено предметов: " + stopped.fixedItems), true);
                    return 1;
                }));
    }

    private static int startArea(CommandSourceStack source, int radius) {
        if (job != null) {
            source.sendFailure(Component.literal(
                    "Обход уже идёт (" + job.index + "/" + job.total + " чанков). Остановить: /profession fixdurability stop"));
            return 0;
        }
        BlockPos pos = BlockPos.containing(source.getPosition());
        job = new Job(source, source.getLevel(), pos.getX() >> 4, pos.getZ() >> 4, radius);
        Job started = job;
        source.sendSuccess(() -> Component.literal(
                "Запущен обход " + started.total + " чанков вокруг вас (радиус " + radius + "). Ждите сообщения о завершении."), true);
        return 1;
    }

    private static void tick() {
        Job current = job;
        if (current == null) {
            return;
        }
        int width = 2 * current.radius + 1;
        for (int n = 0; n < CHUNKS_PER_TICK && current.index < current.total; n++, current.index++) {
            int cx = current.centerX + (int) (current.index % width) - current.radius;
            int cz = current.centerZ + (int) (current.index / width) - current.radius;
            try {
                LevelChunk chunk = current.level.getChunk(cx, cz);
                for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
                    if (be instanceof Container container) {
                        int fixed = DurabilityNormalizer.normalizeContainer(container);
                        if (fixed > 0) {
                            current.fixedItems += fixed;
                            chunk.markUnsaved();
                        }
                    }
                }
            } catch (RuntimeException ignored) {
                // повреждённый чанк не должен ронять весь обход
            }
        }
        if (current.index >= current.total) {
            job = null;
            current.source.sendSuccess(() -> Component.literal(
                    "Обход завершён: чанков " + current.total + ", исправлено предметов: " + current.fixedItems), true);
        }
    }
}
