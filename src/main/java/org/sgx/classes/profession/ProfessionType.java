package org.sgx.classes.profession;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * Классы игрока. Игрок может сменить класс в любое время — прогресс (XP/уровень)
 * каждого класса хранится отдельно и не сбрасывается при переключении.
 */
public enum ProfessionType implements StringRepresentable {
    NONE("none"),
    FARMER("farmer"),
    MINER("miner"),
    BLACKSMITH("blacksmith"),
    LUMBERJACK("lumberjack"),
    MYSTIC("mystic");

    public static final Codec<ProfessionType> CODEC = StringRepresentable.fromEnum(ProfessionType::values);

    /** Максимальный уровень класса. На уровне ProfessionData.SUBCLASS_BRANCH_LEVEL (10) открывается выбор подкласса. */
    public static final int MAX_LEVEL = 20;

    /**
     * Кривая опыта — одна и та же для всех классов. Индекс = текущий уровень (0..19),
     * значение = сколько XP нужно НАБРАТЬ на этом уровне, чтобы перейти на следующий.
     * Сумма всех 20 значений = 100 000 XP на полную прокачку (0 -> MAX_LEVEL).
     * Не выражается простой формулой (прирост между шагами нелинейный), поэтому таблица
     * зашита напрямую, а не вычисляется — соответствует заданным значениям 1-в-1.
     */
    private static final int[] XP_TABLE = {
            4000, 9000, 18000, 32000, 52000, 74900, 114000, 158000, 212000, 277000,
            300000, 327000, 359000, 397000, 442000, 495000, 557000, 629000, 712000, 807000
    };

    private final String id;

    ProfessionType(String id) {
        this.id = id;
    }

    public int xpToNextLevel(int currentLevel) {
        if (currentLevel < 0 || currentLevel >= XP_TABLE.length) {
            return XP_TABLE[XP_TABLE.length - 1]; // защита от выхода за границы — не должно происходить при currentLevel < MAX_LEVEL
        }
        return XP_TABLE[currentLevel];
    }

    /** Весь накопленный опыт: сумма опыта пройденных уровней + опыт на текущем уровне. */
    public long totalXp(int level, int xpOnLevel) {
        long total = xpOnLevel;
        for (int i = 0; i < Math.min(level, XP_TABLE.length); i++) {
            total += XP_TABLE[i];
        }
        return total;
    }

    public Component displayName() {
        return Component.translatable("profession.arz_classes." + id);
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}