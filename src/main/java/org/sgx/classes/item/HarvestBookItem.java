package org.sgx.classes.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.Set;

/**
 * Книга урожая: крафтит только Фермер. Первое открытие (ПКМ) привязывает её к нику игрока;
 * дальше урожай считается только пока книга у владельца — в слоте Trinkets или в инвентаре.
 * Общая логика (привязка, поиск, запись) — в {@link ClassBookItem}; здесь только учёт культур и животных.
 */
public final class HarvestBookItem extends ClassBookItem {

    public HarvestBookItem(Properties properties) {
        super(properties, BookKind.HARVEST);
    }

    /**
     * Культуры-блоки, которые НЕ считаются «по блоку»: у кустов перцев Rustic Delight цвет урожая
     * случайный, поэтому перцы считаются поштучно по выпавшим предметам ({@link #COUNTED_DROPS}).
     */
    private static final Set<String> UNCOUNTED_CROP_BLOCKS = Set.of(
            "rusticdelight:bell_peppers",
            "rusticdelight:pale_bell_peppers",
            "rusticdelight:dark_bell_peppers");

    /** Предметы, которые считаются поштучно по дропу с культуры (ключ в книге — "item:&lt;id&gt;"). */
    public static final Set<String> COUNTED_DROPS = Set.of(
            "rusticdelight:bell_pepper_green",
            "rusticdelight:bell_pepper_yellow",
            "rusticdelight:bell_pepper_red",
            "rusticdelight:bell_pepper_orange",
            "rusticdelight:bell_pepper_white",
            "rusticdelight:bell_pepper_pink",
            "rusticdelight:bell_pepper_blue",
            "rusticdelight:bell_pepper_purple",
            "rusticdelight:bell_pepper_black");

    /** +1 к счётчику книги урожая. */
    public static void record(ServerPlayer player, String key) {
        record(player, BookKind.HARVEST, key, 1);
    }

    public static void record(ServerPlayer player, String key, int amount) {
        record(player, BookKind.HARVEST, key, amount);
    }

    public static void recordCrop(ServerPlayer player, Block crop) {
        String id = BuiltInRegistries.BLOCK.getKey(crop).toString();
        if (UNCOUNTED_CROP_BLOCKS.contains(id)) {
            return;
        }
        record(player, "crop:" + id);
    }

    /**
     * Считает поштучно отслеживаемые дропы (перцы), только что выпавшие у блока {@code pos}.
     * Вызывать СРАЗУ после сбора культуры: выбираются ItemEntity, созданные в этом же тике (tickCount == 0).
     */
    public static void recordNearbyDrops(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!isTracking(player, BookKind.HARVEST)) {
            return;
        }
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0),
                entity -> entity.tickCount == 0)) {
            ItemStack stack = e.getItem();
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (COUNTED_DROPS.contains(id)) {
                record(player, "item:" + id, stack.getCount());
            }
        }
    }

    /** Убитое мирное животное: "kill:&lt;id типа&gt;". */
    public static void recordKill(ServerPlayer player, EntityType<?> type) {
        record(player, "kill:" + BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    /** Разведённое животное: "bred:&lt;id типа&gt;". */
    public static void recordBred(ServerPlayer player, EntityType<?> type) {
        record(player, "bred:" + BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    public static void recordGolden(ServerPlayer player, Item golden) {
        record(player, "gold:" + BuiltInRegistries.ITEM.getKey(golden));
    }
}
