package org.sgx.classes.profession;

import java.util.EnumMap;
import java.util.Map;

public final class ProfessionColors {

    private static final Map<ProfessionType, Integer> COLORS = new EnumMap<>(ProfessionType.class);

    static {
        COLORS.put(ProfessionType.FARMER, 0xFF7CC576);      // зелёный — поля
        COLORS.put(ProfessionType.MINER, 0xFFB0B0B0);       // серый — камень
        COLORS.put(ProfessionType.BLACKSMITH, 0xFFE07B39);  // оранжевый — горн/металл
        COLORS.put(ProfessionType.LUMBERJACK, 0xFF9C6B32);  // коричневый — дерево
        COLORS.put(ProfessionType.MYSTIC, 0xFFA35CE0);      // фиолетовый — магия
    }

    private ProfessionColors() {
    }
    public static int of(ProfessionType type) {
        return COLORS.getOrDefault(type, 0xFFCCCCCC);
    }
}