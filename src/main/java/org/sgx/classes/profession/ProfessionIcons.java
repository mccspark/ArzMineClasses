package org.sgx.classes.profession;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.sgx.classes.item.ModItems;

import java.util.EnumMap;
import java.util.Map;

/**
 * "Карточная" иконка каждого класса в карусели ClassesScreen. Сейчас — ванильные
 * предметы-заглушки; когда появятся свои модели/иконки классов, просто замените
 * значения в static-блоке (можно на любой Item, включая кастомные из ModItems).
 */
public final class ProfessionIcons {

    private static final Map<ProfessionType, Item> ICONS = new EnumMap<>(ProfessionType.class);

    static {
        ICONS.put(ProfessionType.FARMER, Items.WHEAT);
        ICONS.put(ProfessionType.MINER, Items.IRON_PICKAXE);
        ICONS.put(ProfessionType.BLACKSMITH, ModItems.BLACKSMITH_HAMMER);
        ICONS.put(ProfessionType.LUMBERJACK, Items.IRON_AXE);
        ICONS.put(ProfessionType.MYSTIC, ModItems.KNOWLEDGE_SCROLL);
    }

    private ProfessionIcons() {
    }

    public static ItemStack of(ProfessionType type) {
        return new ItemStack(ICONS.getOrDefault(type, Items.BARRIER));
    }
}