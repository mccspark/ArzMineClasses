package org.sgx.classes.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.sgx.classes.profession.impl.PlacedLogTracker;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Точки учёта для книг Кузнеца, Мистика, Шахтёра и Лесоруба — вызываются из событий и миксинов. */
public final class ClassBookHooks {

    private static final Set<String> STONES = new HashSet<>(Arrays.asList(BookHistory.STONES));

    private ClassBookHooks() {
    }

    private static String id(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block).toString();
    }

    private static boolean isOre(String id) {
        return id.endsWith("_ore") || id.equals("minecraft:ancient_debris");
    }

    /**
     * Блок сломан игроком (до логики профессий — нужна отметка «бревно поставлено игроком»).
     * Шахтёр: руды и камень (камень — только киркой). Лесоруб: брёвна, не поставленные игроком.
     */
    public static void onBlockBroken(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        String id = id(block);

        if (isOre(id)) {
            ClassBookItem.record(player, BookKind.MINER, "ore:" + id);
        } else if (STONES.contains(id) && player.getMainHandItem().is(ItemTags.PICKAXES)) {
            ClassBookItem.record(player, BookKind.MINER, "stone:" + id);
        }

        if (block.builtInRegistryHolder().is(BlockTags.LOGS) && !PlacedLogTracker.get(level).isPlaced(pos)) {
            ClassBookItem.record(player, BookKind.LUMBERJACK, "log:" + id);
        }
    }

    /**
     * После логики профессий: яблоки, выпавшие из сломанной листвы (и ванильные, и бонусные от навыка).
     * Выбираются свежие ItemEntity (tickCount == 0) рядом с блоком.
     */
    public static void afterBlockBroken(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.getBlock().builtInRegistryHolder().is(BlockTags.LEAVES)
                || !ClassBookItem.isTracking(player, BookKind.LUMBERJACK)) {
            return;
        }
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0),
                entity -> entity.tickCount == 0)) {
            ItemStack stack = e.getItem();
            if (stack.is(Items.APPLE) || stack.is(Items.GOLDEN_APPLE)) {
                ClassBookItem.record(player, BookKind.LUMBERJACK,
                        "apple:" + BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount());
            }
        }
    }

    /** Бревно, срубленное трикопитейтором (событие слома на него не приходит). */
    public static void onFelledLog(ServerPlayer player, Block log) {
        ClassBookItem.record(player, BookKind.LUMBERJACK, "log:" + id(log));
    }

    /** Игрок поставил саженец. */
    public static void onPlaced(ServerPlayer player, BlockState state) {
        if (state.getBlock() instanceof net.minecraft.world.level.block.SaplingBlock) {
            ClassBookItem.record(player, BookKind.LUMBERJACK, "sapling:" + id(state.getBlock()));
        }
    }

    // ───────────── Кузнец ─────────────

    /** Скрафчено снаряжение, получившее редкость: предмет + выпавшая редкость. */
    public static void onCrafted(ServerPlayer player, ItemStack stack, ItemRarity rarity) {
        if (!ClassBookItem.isTracking(player, BookKind.BLACKSMITH)) {
            return;
        }
        ClassBookItem.record(player, BookKind.BLACKSMITH,
                "craft:" + BuiltInRegistries.ITEM.getKey(stack.getItem()), Math.max(1, stack.getCount()));
        ClassBookItem.record(player, BookKind.BLACKSMITH, "rarity:" + rarity.name());
    }

    /** Редкость улучшена на столе улучшения (только удачные попытки). */
    public static void onRarityUpgraded(ServerPlayer player, ItemRarity rarity) {
        ClassBookItem.record(player, BookKind.BLACKSMITH, "rarity:" + rarity.name());
    }

    /** Заточка: "weapon" или "armor". */
    public static void onSharpened(ServerPlayer player, String what) {
        ClassBookItem.record(player, BookKind.BLACKSMITH, "sharpen:" + what);
    }

    /** Починка предмета (в том числе частичная). */
    public static void onRepaired(ServerPlayer player) {
        ClassBookItem.record(player, BookKind.BLACKSMITH, "repair:total");
    }

    // ───────────── Мистик ─────────────

    private static ItemEnchantments enchantsOf(ItemStack stack) {
        ItemEnchantments e = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (e.isEmpty()) {
            e = stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        }
        return e;
    }

    /** Стол зачарований: каждое чарование предмета на выходе считается (по одному за вид чар). */
    public static void onTableEnchanted(ServerPlayer player, ItemStack enchanted) {
        if (!ClassBookItem.isTracking(player, BookKind.MYSTIC)) {
            return;
        }
        ItemEnchantments result = enchantsOf(enchanted);
        for (Holder<Enchantment> h : result.keySet()) {
            ClassBookItem.record(player, BookKind.MYSTIC, "table:" + h.getRegisteredName());
        }
    }

    /** Наковальня: считаются чары, которых не было на левом предмете или которые стали выше. */
    public static void onAnvilTaken(ServerPlayer player, ItemStack left, ItemStack result) {
        if (!ClassBookItem.isTracking(player, BookKind.MYSTIC)) {
            return;
        }
        ItemEnchantments before = enchantsOf(left);
        ItemEnchantments after = enchantsOf(result);
        for (Holder<Enchantment> h : after.keySet()) {
            if (after.getLevel(h) > before.getLevel(h)) {
                ClassBookItem.record(player, BookKind.MYSTIC, "anvil:" + h.getRegisteredName());
            }
        }
    }

    /** Полученный опыт (очки) и рекорд уровня. */
    public static void onExperience(ServerPlayer player, int points) {
        ClassBookItem.record(player, BookKind.MYSTIC, "xp:total", points);
    }

    public static void onLevel(ServerPlayer player) {
        ClassBookItem.recordMax(player, BookKind.MYSTIC, "xp:maxlevel", player.experienceLevel);
    }
}
