package org.sgx.classes.item;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Книга Мистика: опыт и рекорд уровня.
 * <p>
 * Раньше это считал миксин на Player.giveExperiencePoints с require = 0 — если сигнатура метода в версии
 * не совпадала, миксин молча не применялся, и книга не считала опыт вообще. Теперь считаем по тикам:
 * сравниваем Player.totalExperience (растёт только когда игрок получает опыт; трата уровней на чары его
 * не уменьшает) с прошлым значением. От сигнатур методов это не зависит.
 */
public final class MysticBookTracker {

    private static final int PERIOD_TICKS = 10;

    /** Прошлое значение totalExperience по игрокам. */
    private static final Map<UUID, Integer> LAST_TOTAL = new HashMap<>();

    private MysticBookTracker() {
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(MysticBookTracker::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                LAST_TOTAL.remove(handler.getPlayer().getUUID()));
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.tickCount % PERIOD_TICKS != 0) {
                continue;
            }
            int total = player.totalExperience;
            Integer last = LAST_TOTAL.put(player.getUUID(), total);
            // Первый замер, сброс (смерть, команда) или уменьшение — просто новая точка отсчёта.
            if (last != null && total > last) {
                ClassBookHooks.onExperience(player, total - last);
            }
            ClassBookHooks.onLevel(player);
        }
    }
}
