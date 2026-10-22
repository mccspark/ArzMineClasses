package org.sgx.classes.profession;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.attachment.ModAttachments;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Достижения классов (data/arz_classes/advancement):
 * <ul>
 *   <li>{@code root} — вкладка «Классы», открывается всем;</li>
 *   <li>{@code choose_class} — «Выбор пути», выдаётся, как только выбран любой класс;</li>
 *   <li>{@code <класс>/...} — скрытая вкладка выбранного класса. Видна только у текущего класса: при смене
 *       класса ВСЕ достижения старого класса (и корень, и вложенные) сбрасываются, вкладка закрывается —
 *       в том же духе, что и полный сброс прогресса класса.</li>
 * </ul>
 * sync идемпотентна — вызывается при входе (игрокам, уже выбравшим класс, всё выдастся автоматически)
 * и при любой смене класса.
 */
public final class ClassAdvancements {

    private static final String CRITERION = "chosen";

    /** Через сколько тиков после входа повторить синхронизацию (см. {@link #init()}). */
    private static final int JOIN_DELAY_TICKS = 40;
    /** Страховка: раз в 10 секунд досинхронизируем всех. */
    private static final int PERIODIC_TICKS = 200;

    private static final Map<UUID, Integer> PENDING = new HashMap<>();
    private static int tickCounter = 0;

    private ClassAdvancements() {
    }

    /**
     * Выдача на самом событии входа не срабатывает: ванильный вход игрока позже перечитывает его прогресс
     * достижений из файла и затирает то, что мы выдали в JOIN. Поэтому повторяем синхронизацию спустя
     * пару секунд после входа и потом периодически (sync идемпотентна и дешёвая, когда всё уже выдано).
     */
    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                PENDING.put(handler.getPlayer().getUUID(), JOIN_DELAY_TICKS));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                PENDING.remove(handler.getPlayer().getUUID()));
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!PENDING.isEmpty()) {
                for (var it = PENDING.entrySet().iterator(); it.hasNext(); ) {
                    var e = it.next();
                    if (e.getValue() > 1) {
                        e.setValue(e.getValue() - 1);
                        continue;
                    }
                    it.remove();
                    ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
                    if (player != null) {
                        sync(player, ModAttachments.get(player));
                    }
                }
            }
            if (++tickCounter >= PERIODIC_TICKS) {
                tickCounter = 0;
                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    sync(player, ModAttachments.get(player));
                }
            }
        });
    }

    public static void sync(ServerPlayer player, ProfessionData data) {
        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return;
        }
        PlayerAdvancements advancements = player.getAdvancements();
        ProfessionType current = data.currentProfession();

        if (current != ProfessionType.NONE) {
            award(server, advancements, "choose_class");
            award(server, advancements, current.getSerializedName() + "/root");
        }
        for (ProfessionType type : ProfessionType.values()) {
            if (type != ProfessionType.NONE && type != current) {
                revokeClassTree(server, advancements, type.getSerializedName() + "/");
            }
        }
    }

    /** Выдаёт достижение (если ещё не выдано). Об «announce_to_chat» и тосте заботится сам ванильный механизм. */
    private static void award(MinecraftServer server, PlayerAdvancements advancements, String path) {
        AdvancementHolder holder = server.getAdvancements().get(Arz_classes.id(path));
        if (holder != null && !advancements.getOrStartProgress(holder).isDone()) {
            advancements.award(holder, CRITERION);
        }
    }

    /** Забирает у игрока все выполненные критерии всех достижений класса (id вида arz_classes:<класс>/...). */
    private static void revokeClassTree(MinecraftServer server, PlayerAdvancements advancements, String pathPrefix) {
        for (AdvancementHolder holder : new ArrayList<>(server.getAdvancements().getAllAdvancements())) {
            var id = holder.id();
            if (!id.getNamespace().equals(Arz_classes.MOD_ID) || !id.getPath().startsWith(pathPrefix)) {
                continue;
            }
            List<String> done = new ArrayList<>();
            advancements.getOrStartProgress(holder).getCompletedCriteria().forEach(done::add);
            for (String criterion : done) {
                advancements.revoke(holder, criterion);
            }
        }
    }
}
