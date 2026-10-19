package org.sgx.classes.profession;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.profession.skill.SkillDefinition;
import org.sgx.classes.profession.skill.SkillEffect;

public final class SharpeningCaps {

    /** Абсолютный потолок заточки (дальше SharpenName заточку не читает). */
    private static final int HARD_MAX = 10;

    /**
     * Оружие и инструменты, основная ветка кузнеца: индекс = число купленных узлов
     * "blacksmith_sharpen" (их 5), значение = потолок. +1 за узел → +5.
     */
    private static final int[] BASE_CAP_BY_NODES = {0, 1, 2, 3, 4, 5};

    /**
     * Оружейник: индекс = число купленных узлов "weaponsmith_sharpen" (их 4), значение = прибавка
     * к потолку оружия и инструментов. Последний (мифический) узел даёт +2 → всего 5 + 5 = +10.
     */
    private static final int[] WEAPONSMITH_EXTRA_BY_NODES = {1, 2, 3, 4, 5};

    /**
     * Бронник: индекс = число купленных узлов "armorer_sharpen" (их 5), значение = потолок
     * заточки брони. +2 за узел → +10.
     */
    private static final int[] ARMOR_CAP_BY_NODES = {1, 2, 4, 6, 8, 10};

    private SharpeningCaps() {
    }

    public static boolean isWeaponCategory(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(Items.TRIDENT) || stack.is(ItemTags.AXES)
                || WeaponSharpening.isSpear(stack);
    }

    public static boolean isToolCategory(ItemStack stack) {
        return stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES) || stack.is(ItemTags.AXES);
    }

    /** Единая формула для оружия И инструментов. Основная ветка кузнеца даёт до +5,
     * Оружейник добавляет ещё до +5 (итого +10). */
    public static int weaponOrToolCap(ItemStack stack, ProfessionData data) {
        int base = fromTable(BASE_CAP_BY_NODES,
                purchasedNodesWithScalarKey(data, "blacksmith.sharpen_efficiency_percent"));
        if (data.currentSubclass() == SubclassType.WEAPONSMITH) {
            base += fromTable(WEAPONSMITH_EXTRA_BY_NODES,
                    purchasedNodesWithScalarKey(data, "weaponsmith.sharpen_efficiency_percent"));
        }
        return Math.min(HARD_MAX, base);
    }

    /** Потолок брони: только у Бронника, по таблице ARMOR_CAP_BY_NODES (при полной прокачке +10). */
    public static int armorCap(ProfessionData data) {
        if (data.currentSubclass() != SubclassType.ARMORER) {
            return 0;
        }
        int nodes = purchasedNodesWithScalarKey(data, "armorer.sharpen_efficiency_percent");
        return Math.min(HARD_MAX, fromTable(ARMOR_CAP_BY_NODES, nodes));
    }

    /** Если узлов станет больше, чем строк в таблице, берётся последнее значение. */
    private static int fromTable(int[] table, int nodes) {
        return table[Math.max(0, Math.min(nodes, table.length - 1))];
    }

    /** Считает купленные узлы по НАЛИЧИЮ эффекта с нужным ключом (CustomScalar), а не по
     * id — так капстоун-узлы (scalarAndMilestone, у которых свой фиксированный id) тоже
     * честно учитываются, раз они реально несут этот эффект среди прочих. */
    private static int purchasedNodesWithScalarKey(ProfessionData data, String statKey) {
        int count = 0;
        for (SkillDefinition s : data.unlockedSkills()) {
            for (SkillEffect e : s.effects()) {
                if (e instanceof SkillEffect.CustomScalar cs && cs.key().equals(statKey)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }
}