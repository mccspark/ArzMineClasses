package org.sgx.classes.profession.impl;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionBehavior;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class LumberjackBehavior implements ProfessionBehavior {

    private static final int XP_PER_LOG = 20;
    private static final int TREECAPITATOR_MAX_LOGS = 128;
    private static final float GOLDEN_APPLE_CHANCE = 0.01f;

    private static final int MIN_TICKS_PER_LOG = 2;
    private static final int MAX_TICKS_PER_LOG = 6;
    private static final int SLOW_START_LOGS = 12;
    private static final int LOGS_PER_EXTRA_TICK = 12;

    private static int ticksPerLog(int totalLogs) {
        if (totalLogs <= SLOW_START_LOGS) {
            return MIN_TICKS_PER_LOG;
        }
        int extra = 1 + (totalLogs - SLOW_START_LOGS - 1) / LOGS_PER_EXTRA_TICK;
        return Math.min(MAX_TICKS_PER_LOG, MIN_TICKS_PER_LOG + extra);
    }

    /** Дерево, которое сейчас валится: оставшиеся брёвна (в порядке рубки) и таймер до следующего. */
    private static final class FellingTask {
        final UUID playerId;
        final ServerLevel world;
        final Block logType;
        final ProfessionData data;
        final Deque<BlockPos> remaining;
        final int ticksPerLog;
        int cooldown;

        FellingTask(UUID playerId, ServerLevel world, Block logType, ProfessionData data,
                    Deque<BlockPos> remaining, int ticksPerLog) {
            this.playerId = playerId;
            this.world = world;
            this.logType = logType;
            this.data = data;
            this.remaining = remaining;
            this.ticksPerLog = ticksPerLog;
            this.cooldown = ticksPerLog;
        }
    }

    private static final List<FellingTask> FELLING = new ArrayList<>();

    static {
        ServerTickEvents.END_SERVER_TICK.register(LumberjackBehavior::tickFelling);
    }

    @Override
    public ProfessionType type() {
        return ProfessionType.LUMBERJACK;
    }

    @Override
    public int onBlockBroken(ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        boolean isLog = block.builtInRegistryHolder().is(BlockTags.LOGS);
        boolean isLeaves = block.builtInRegistryHolder().is(BlockTags.LEAVES);

        if (!isLog && !isLeaves) {
            return 0;
        }

        ProfessionData data = ModAttachments.get(player);

        if (isLeaves) {
            boolean brokenWithShears = player.getMainHandItem().is(Items.SHEARS);
            if (brokenWithShears) {
                return 0;
            }

            double leafChance = data.customScalarTotal("lumberjack.leaf_drop_chance_percent");
            boolean leafDropUnlocked = leafChance > 0;

            if (leafDropUnlocked && world.getRandom().nextFloat() < (float) Math.min(0.75, leafChance / 100.0)) {
                dropStack(world, pos, new ItemStack(Items.APPLE));
            }

            if (leafDropUnlocked && world.getRandom().nextFloat() < GOLDEN_APPLE_CHANCE) {
                dropStack(world, pos, new ItemStack(Items.GOLDEN_APPLE));
            }

            return 0;
        }

        // --- дальше только брёвна ---

        // Бревно поставил игрок: ни опыта, ни бонусов, ни трикопитейтора
        PlacedLogTracker tracker = PlacedLogTracker.get(world);
        if (tracker.isPlaced(pos)) {
            tracker.unmark(pos);
            return 0;
        }

        rollCoalDrop(world, pos, data);

        boolean holdingAxe = player.getMainHandItem().is(ItemTags.AXES);
        boolean treecapitatorReady = holdingAxe && data.hasAbility("lumberjack_treecapitator")
                && !player.isShiftKeyDown();

        if (treecapitatorReady) {
            int totalLogs = startFelling(player, world, pos, block, data);
            return XP_PER_LOG * totalLogs;
        }

        return XP_PER_LOG;
    }

    /** Шанс угля катится на КАЖДОЕ отдельное бревно (и при обычном сломе, и при каждом
     * блоке внутри трикопитейтор-рубки). */
    private static void rollCoalDrop(ServerLevel world, BlockPos pos, ProfessionData data) {
        if (!data.hasAbility("lumberjack_coal_chance")) {
            return;
        }
        double coalChance = data.customScalarTotal("lumberjack.coal_drop_chance_percent");
        if (coalChance <= 0) {
            return;
        }
        float chance = (float) Math.min(0.75, coalChance / 100.0);
        if (world.getRandom().nextFloat() < chance) {
            dropStack(world, pos, new ItemStack(Items.CHARCOAL));
        }
    }

    /**
     * Находит всё дерево (BFS от исходного бревна) и ставит его в очередь на рубку.
     * Исходный блок уже сломан стандартным путём; остальные падают по одному раз в N тиков,
     * где N растёт с размером дерева (см. ticksPerLog).
     * Возвращает общее число брёвен (включая исходное) — для опыта.
     */
    private int startFelling(ServerPlayer player, ServerLevel world, BlockPos origin, Block logType, ProfessionData data) {
        PlacedLogTracker tracker = PlacedLogTracker.get(world);
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        List<BlockPos> found = new ArrayList<>();

        visited.add(origin);
        queue.add(origin);

        while (!queue.isEmpty() && found.size() < TREECAPITATOR_MAX_LOGS) {
            BlockPos current = queue.poll();
            found.add(current);

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
                        BlockState nextState = world.getBlockState(next);
                        if (nextState.getBlock() == logType && !tracker.isPlaced(next)) {
                            visited.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }

        Deque<BlockPos> remaining = new ArrayDeque<>();
        for (BlockPos p : found) {
            if (!p.equals(origin)) {
                remaining.add(p);
            }
        }
        if (!remaining.isEmpty()) {
            FELLING.add(new FellingTask(player.getUUID(), world, logType, data, remaining,
                    ticksPerLog(found.size())));
        }

        return found.size();
    }

    /** Раз в тик двигает все активные рубки. */
    private static void tickFelling(MinecraftServer server) {
        if (FELLING.isEmpty()) {
            return;
        }
        Iterator<FellingTask> it = FELLING.iterator();
        while (it.hasNext()) {
            FellingTask task = it.next();

            ServerPlayer player = server.getPlayerList().getPlayer(task.playerId);
            // игрок вышел / умер / сменил измерение / убрал топор — рубка прекращается
            if (player == null || !player.isAlive() || player.level() != task.world
                    || !player.getMainHandItem().is(ItemTags.AXES)) {
                it.remove();
                continue;
            }

            if (--task.cooldown > 0) {
                continue;
            }
            task.cooldown = task.ticksPerLog;

            PlacedLogTracker tracker = PlacedLogTracker.get(task.world);
            BlockPos next = null;
            while (!task.remaining.isEmpty()) {
                BlockPos candidate = task.remaining.poll();
                if (task.world.getBlockState(candidate).getBlock() == task.logType
                        && !tracker.isPlaced(candidate)) {
                    next = candidate;
                    break;
                }
            }
            if (next == null) {
                it.remove();
                continue;
            }

            task.world.destroyBlock(next, true, player);
            org.sgx.classes.item.ClassBookHooks.onFelledLog(player, task.logType);
            rollCoalDrop(task.world, next, task.data);

            ItemStack tool = player.getMainHandItem();
            tool.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);

            if (task.remaining.isEmpty() || player.getMainHandItem().isEmpty()) {
                it.remove();
            }
        }
    }

    private static void dropStack(ServerLevel world, BlockPos pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        world.addFreshEntity(entity);
    }
}