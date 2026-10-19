package org.sgx.classes.profession;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

/**
 * Подклассы. Игрок может иметь только один активный подкласс одновременно.
 * Смена подкласса (свап) обнуляет прогресс ТОЛЬКО прокачки этого подкласса,
 * основной класс (ProfessionType) при этом не трогается.
 */
public enum SubclassType implements StringRepresentable {
    // Фермер
    COOK("cook", ProfessionType.FARMER),               // Повар
    DRUNKARD("drunkard", ProfessionType.FARMER),        // Алкаш

    // Шахтёр
    COMBAT_MINER("combat_miner", ProfessionType.MINER), // Боевой
    CLAN_FARMER("clan_farmer", ProfessionType.MINER),   // Клановый фармила

    // Кузнец
    ARMORER("armorer", ProfessionType.BLACKSMITH),      // Бронник
    WEAPONSMITH("weaponsmith", ProfessionType.BLACKSMITH), // Оружейник

    // Лесоруб
    CARPENTER("carpenter", ProfessionType.LUMBERJACK),  // Плотник
    JAEGER("jaeger", ProfessionType.LUMBERJACK),      // Углежог

    // Мистик
    ENCHANTER("enchanter", ProfessionType.MYSTIC),      // Зачарователь
    ALCHEMIST("alchemist", ProfessionType.MYSTIC);       // Алхимик

    public static final Codec<SubclassType> CODEC = StringRepresentable.fromEnum(SubclassType::values);

    public static final int MAX_LEVEL = 20;

    private final String id;
    private final ProfessionType parent;

    SubclassType(String id, ProfessionType parent) {
        this.id = id;
        this.parent = parent;
    }

    public ProfessionType parent() {
        return parent;
    }

    public int xpToNextLevel(int currentLevel) {
        return 150 + currentLevel * 40;
    }

    public Component displayName() {
        return Component.translatable("subclass.arz_classes." + id);
    }

    @Override
    public String getSerializedName() {
        return id;
    }
}
