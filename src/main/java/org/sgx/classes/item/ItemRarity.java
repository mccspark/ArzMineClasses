package org.sgx.classes.item;

import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

/**
 * Редкость предмета, выдаваемая кузнецом при крафте по шансу (см. milestone-узлы
 * blacksmith_rarity_rare/epic, armorer_rarity_legendary/mythic,
 * weaponsmith_rarity_legendary/mythic). Обычные игроки всегда крафтят COMMON —
 * этот компонент на их предметы никогда не проставляется.
 *
 * FIX: раньше хранили ChatFormatting и звали ChatFormatting#getColor() для текста/GUI —
 * этот метод в вашей версии недоступен (версионный риск API). Теперь цвет хранится
 * напрямую как ARGB int (0xFFRRGGBB), не зависит от ChatFormatting вообще. ChatFormatting
 * оставлен отдельным полем ТОЛЬКО для форматирования текста через Component (§-коды),
 * которое работает иначе и не завязано на getColor().
 */
public enum ItemRarity implements StringRepresentable {
    COMMON("common", ChatFormatting.GRAY, 0xFFAAAAAA),
    RARE("rare", ChatFormatting.BLUE, 0xFF5555FF),
    EPIC("epic", ChatFormatting.DARK_PURPLE, 0xFFAA00AA),
    LEGENDARY("legendary", ChatFormatting.GOLD, 0xFFFFAA00),
    MYTHIC("mythic", ChatFormatting.DARK_RED, 0xFFFF3333);

    public static final Codec<ItemRarity> CODEC = StringRepresentable.fromEnum(ItemRarity::values);

    private final String id;
    private final ChatFormatting formatting;
    private final int argb;
    /** Множитель бонуса к характеристикам предмета этой редкости — значения условные, перебалансируем позже. */
    private static final double[] STAT_BONUS_PERCENT = {0.0, 0.05, 0.10, 0.18, 0.28};

    ItemRarity(String id, ChatFormatting formatting, int argb) {
        this.id = id;
        this.formatting = formatting;
        this.argb = argb;
    }

    /** Для окраски текста в GUI/тултипах напрямую по числу — без ChatFormatting#getColor(). */
    public int argb() {
        return argb;
    }

    /** Для §-форматирования Component (например tooltip.withStyle(rarity.formatting())). */
    public ChatFormatting formatting() {
        return formatting;
    }

    public double statBonusPercent() {
        return STAT_BONUS_PERCENT[this.ordinal()];
    }

    public String translationKey() {
        return "rarity.arz_classes." + id;
    }

    @Override
    public String getSerializedName() {
        return id;
    }

    /** Простой lookup по id (для network stream-декодинга — см. ModDataComponents.ITEM_RARITY). */
    public static ItemRarity byId(String id) {
        for (ItemRarity rarity : values()) {
            if (rarity.id.equals(id)) {
                return rarity;
            }
        }
        return COMMON;
    }
}