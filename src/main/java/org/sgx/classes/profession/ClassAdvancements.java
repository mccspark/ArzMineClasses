package org.sgx.classes.profession;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.sgx.classes.Arz_classes;

import java.util.ArrayList;
import java.util.List;

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

    private ClassAdvancements() {
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
