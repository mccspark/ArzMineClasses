package org.sgx.classes.profession.impl;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionData;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/** Замедляет рубку бревна топором с трикопитейтором: чем больше дерево, тем дольше. */
public final class TreeChopSpeed {

    /** Включить, чтобы раз в секунду видеть в консоли, что считается на клиенте и сервере. */
    private static final boolean DEBUG = false;

    /** Прирост времени рубки за каждое бревно (0.1 = +10%), пока дерево не «большое». */
    private static final double SLOWDOWN_PER_LOG = 0.1;
    /** С какого числа брёвен дерево считается большим и замедление растёт вдвое быстрее. */
    private static final int BIG_TREE_START = 20;
    /** Прирост за каждое бревно сверх BIG_TREE_START. */
    private static final double SLOWDOWN_PER_BIG_LOG = 0.2;
    /** Потолок замедления (16 = максимум в 16 раз дольше обычного). */
    private static final double MAX_SLOWDOWN = 16.0;
    /** Как часто пересчитывать размер дерева, пока игрок рубит (в тиках). */
    private static final int RECOMPUTE_TICKS = 10;

    private static final class Cache {
        UUID player;
        long pos = Long.MIN_VALUE;
        long time = -1000;
        int logs = 1;
    }

    // отдельные кэши: клиентский и серверный потоки не пересекаются
    private static final Cache CLIENT = new Cache();
    private static final Cache SERVER = new Cache();

    /** Источник данных профессии на клиенте. Ставится из Arz_classesClient, на сервере остаётся пустым. */
    private static Supplier<ProfessionData> clientData = () -> null;

    private TreeChopSpeed() {
    }

    public static void setClientDataSupplier(Supplier<ProfessionData> supplier) {
        clientData = supplier;
    }

    public static float adjust(float progress, BlockState state, Player player, BlockGetter level, BlockPos pos) {
        boolean isLog = state.getBlock().builtInRegistryHolder().is(BlockTags.LOGS);
        boolean axe = player.getMainHandItem().is(ItemTags.AXES);
        boolean client = player.level().isClientSide();

        // На клиенте ModAttachments пуст: данные профессии лежат в ClientProfessionData
        ProfessionData data = client ? clientData.get() : ModAttachments.get(player);
        boolean ability = isLog && axe && data != null && data.hasAbility("lumberjack_treecapitator");

        if (DEBUG && isLog && player.level().getGameTime() % 20 == 0) {
            System.out.println("[CHOPDBG] side=" + (client ? "client" : "server")
                    + " progress=" + progress + " axe=" + axe + " shift=" + player.isShiftKeyDown()
                    + " ability=" + ability);
        }

        // Бонус скорости навыков (лесоруб/шахтёр) — применяется всегда, независимо от трикопитейтора
        progress = applySkillSpeed(progress, state, player, data);

        if (progress <= 0f || !isLog || !axe || player.isShiftKeyDown() || !ability) {
            return progress;
        }

        PlacedLogTracker tracker = null;
        if (player.level() instanceof ServerLevel serverLevel) {
            tracker = PlacedLogTracker.get(serverLevel);
            if (tracker.isPlaced(pos)) {
                return progress; // поставленное игроком бревно — без замедления
            }
        }

        int logs = cachedCount(client ? CLIENT : SERVER, player, level, pos, state.getBlock(), tracker);
        int normalLogs = Math.min(logs - 1, BIG_TREE_START - 1);
        int bigLogs = Math.max(0, logs - BIG_TREE_START);
        double slowdown = Math.min(MAX_SLOWDOWN,
                1.0 + normalLogs * SLOWDOWN_PER_LOG + bigLogs * SLOWDOWN_PER_BIG_LOG);

        if (DEBUG && player.level().getGameTime() % 20 == 0) {
            System.out.println("[CHOPDBG] side=" + (client ? "client" : "server")
                    + " logs=" + logs + " slowdown=" + slowdown);
        }
        return (float) (progress / slowdown);
    }

    /**
     * Процентный бонус скорости добычи от навыков:
     *  - лесоруб: топор по деревянным блокам -> lumberjack.wood_break_speed_percent
     *  - шахтёр: кирка по каменным блокам   -> miner.mining_speed_percent
     * Работает и на клиенте (предсказание прогресса), и на сервере, поэтому данные берём
     * из того же источника, что и остальной adjust().
     */
    private static float applySkillSpeed(float progress, BlockState state, Player player, ProfessionData data) {
        if (progress <= 0f || data == null) {
            return progress;
        }
        net.minecraft.world.item.ItemStack tool = player.getMainHandItem();
        double percent = 0;
        if (tool.is(ItemTags.AXES) && state.is(BlockTags.MINEABLE_WITH_AXE)) {
            percent = data.customScalarTotal("lumberjack.wood_break_speed_percent");
        } else if (tool.is(ItemTags.PICKAXES) && state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            percent = data.customScalarTotal("miner.mining_speed_percent");
        }
        if (percent <= 0) {
            return progress;
        }
        return (float) (progress * (1.0 + percent / 100.0));
    }

    private static int cachedCount(Cache cache, Player player, BlockGetter level, BlockPos pos,
                                   Block logType, PlacedLogTracker tracker) {
        long now = player.level().getGameTime();
        long key = pos.asLong();
        if (!player.getUUID().equals(cache.player) || cache.pos != key || now - cache.time >= RECOMPUTE_TICKS) {
            cache.player = player.getUUID();
            cache.pos = key;
            cache.time = now;
            cache.logs = countLogs(level, pos, logType, tracker);
        }
        return cache.logs;
    }

    /** Тот же поиск дерева, что и у валки (соседи dx/dz -1..1, dy -1..2). */
    private static int countLogs(BlockGetter level, BlockPos origin, Block logType, PlacedLogTracker tracker) {
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);
        int count = 0;

        while (!queue.isEmpty() && count < 128) {
            BlockPos current = queue.poll();
            count++;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 2; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos next = current.offset(dx, dy, dz);
                        if (visited.contains(next)) {
                            continue;
                        }
                        if (level.getBlockState(next).getBlock() == logType
                                && (tracker == null || !tracker.isPlaced(next))) {
                            visited.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return count;
    }
}