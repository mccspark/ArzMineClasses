package org.sgx.classes.profession;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.item.ItemRarity;

/**
 * Бонус редкости к характеристикам предмета:
 *   Броня (и каска)  -> ARMOR самого предмета += (броня предмета) * процент редкости.
 *                       Только этот предмет, общую броню игрока не умножает. Toughness не трогаем.
 *   Оружие           -> ATTACK_DAMAGE, ADD_MULTIPLIED_TOTAL (оружие одно в руке).
 *   Инструмент       -> MINING_EFFICIENCY += процент редкости * EFFICIENCY_AT_100_PERCENT.
 *   Топор — и то, и другое.
 * Бонусы заточки (sharpen_*) и зачарования не трогаем: стираются только наши старые "rarity_*".
 * Лук/арбалет: считаются на лету (RarityRangedBonuses + миксины). Щит/удочка/молоты: только прочность.
 */
public final class RarityStatApplier {

    /** Сколько единиц эффективности даёт 100% бонуса: мифическая 28% = 2.24, редкая 5% = 0.4. */
    private static final double EFFICIENCY_AT_100_PERCENT = 8.0;

    private RarityStatApplier() {
    }

    public static void apply(ItemStack stack, ItemRarity rarity) {
        double percent = rarity.statBonusPercent();

        ItemAttributeModifiers existing = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        // копируем всё, кроме наших старых rarity-бонусов, и заодно считаем броню самого предмета
        double itemArmor = 0;
        for (ItemAttributeModifiers.Entry entry : existing.modifiers()) {
            String path = entry.modifier().id().getPath();
            if (path.startsWith("rarity_")) {
                continue;
            }
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
            if (entry.attribute().equals(Attributes.ARMOR)
                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                itemArmor += entry.modifier().amount();
            }
        }

        if (percent > 0) {
            if (RarityEligibility.isArmor(stack)) {
                double armorBonus = itemArmor * percent;
                if (armorBonus > 0) {
                    EquipmentSlotGroup slot = EquipmentSlotGroup.bySlot(stack.get(DataComponents.EQUIPPABLE).slot());
                    builder.add(Attributes.ARMOR,
                            new AttributeModifier(Arz_classes.id("rarity_armor_bonus"), armorBonus,
                                    AttributeModifier.Operation.ADD_VALUE),
                            slot);
                }
            }
            if (RarityEligibility.isWeapon(stack)) {
                builder.add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Arz_classes.id("rarity_damage_bonus"), percent,
                                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL),
                        EquipmentSlotGroup.MAINHAND);
            }
            if (RarityEligibility.isTool(stack)) {
                builder.add(Attributes.MINING_EFFICIENCY,
                        new AttributeModifier(Arz_classes.id("rarity_efficiency_bonus"),
                                percent * EFFICIENCY_AT_100_PERCENT,
                                AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND);
            }
        }

        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }
}