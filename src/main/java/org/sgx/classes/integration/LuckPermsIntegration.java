package org.sgx.classes.integration;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.level.ServerPlayer;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Точка входа для остального кода. ВАЖНО: в этом классе не должно быть ни одного типа из
 * net.luckperms (ни в импортах, ни в лямбдах, ни в локальных переменных). Всё, что трогает LuckPerms,
 * лежит в LuckPermsHook и загружается только если мод LuckPerms реально стоит.
 */
public final class LuckPermsIntegration {

    private static final boolean AVAILABLE =
            FabricLoader.getInstance().isModLoaded("luckperms");

    /** Имена LP-групп по классам. Сисадмин настраивает иконку/префикс на стороне LuckPerms под ЭТИ ЖЕ имена. */
    private static final Map<ProfessionType, String> GROUPS = new EnumMap<>(ProfessionType.class);
    static {
        GROUPS.put(ProfessionType.FARMER, "arz_class_farmer");
        GROUPS.put(ProfessionType.MINER, "arz_class_miner");
        GROUPS.put(ProfessionType.BLACKSMITH, "arz_class_blacksmith");
        GROUPS.put(ProfessionType.LUMBERJACK, "arz_class_lumberjack");
        GROUPS.put(ProfessionType.MYSTIC, "arz_class_mystic");
    }

    private static final Set<String> ALL_GROUPS = Set.copyOf(GROUPS.values());

    private LuckPermsIntegration() {}

    /** Переключает игрока на группу его текущего класса. Вызывать при выборе/смене класса и на джойне. */
    public static void syncGroup(ServerPlayer player, ProfessionData data) {
        if (!AVAILABLE) return;
        LuckPermsHook.sync(player.getUUID(), GROUPS.get(data.currentProfession()), ALL_GROUPS);
    }
}