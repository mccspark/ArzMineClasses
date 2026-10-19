package org.sgx.classes.profession.impl;

import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionBehavior;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

public final class MinerBehavior implements ProfessionBehavior {

    private static final Map<Identifier, Integer> ORE_XP = new HashMap<>();

    /** Ключ скейлера в дереве навыков + предмет бонусного дропа для конкретной руды.
     * Руда без записи здесь — просто пока не имеет узла бонуса, бонус = 0. */
    private record OreBonus(String scalarKey, Item bonusItem) {}

    private static final Map<Identifier, OreBonus> ORE_BONUS = new HashMap<>();

    static {
        ORE_XP.put(id("coal_ore"), 45);
        ORE_XP.put(id("deepslate_coal_ore"), 60);
        ORE_XP.put(id("copper_ore"), 60);
        ORE_XP.put(id("deepslate_copper_ore"), 75);
        ORE_XP.put(id("iron_ore"), 75);
        ORE_XP.put(id("deepslate_iron_ore"),90);
        ORE_XP.put(id("gold_ore"), 120);
        ORE_XP.put(id("deepslate_gold_ore"), 135);
        ORE_XP.put(id("nether_gold_ore"), 135);
        ORE_XP.put(id("redstone_ore"), 90);
        ORE_XP.put(id("deepslate_redstone_ore"), 105);
        ORE_XP.put(id("lapis_ore"), 105);
        ORE_XP.put(id("deepslate_lapis_ore"), 120);
        ORE_XP.put(id("diamond_ore"), 300);
        ORE_XP.put(id("deepslate_diamond_ore"), 225);
        ORE_XP.put(id("emerald_ore"), 300);
        ORE_XP.put(id("deepslate_emerald_ore"), 375);
        ORE_XP.put(id("nether_quartz_ore"), 30);
        ORE_XP.put(id("ancient_debris"), 500);

        ORE_BONUS.put(id("coal_ore"), new OreBonus("miner.bonus_drop_coal", Items.COAL));
        ORE_BONUS.put(id("deepslate_coal_ore"), new OreBonus("miner.bonus_drop_coal", Items.COAL));
        ORE_BONUS.put(id("iron_ore"), new OreBonus("miner.bonus_drop_iron", Items.RAW_IRON));
        ORE_BONUS.put(id("deepslate_iron_ore"), new OreBonus("miner.bonus_drop_iron", Items.RAW_IRON));
        ORE_BONUS.put(id("gold_ore"), new OreBonus("miner.bonus_drop_gold", Items.RAW_GOLD));
        ORE_BONUS.put(id("deepslate_gold_ore"), new OreBonus("miner.bonus_drop_gold", Items.RAW_GOLD));
        ORE_BONUS.put(id("nether_gold_ore"), new OreBonus("miner.bonus_drop_gold", Items.GOLD_NUGGET));
    }

    private static Identifier id(String path) {
        return Identifier.withDefaultNamespace(path);
    }

    @Override
    public ProfessionType type() {
        return ProfessionType.MINER;
    }

    @Override
    public int onBlockBroken(ServerPlayer player, ServerLevel world, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        Identifier blockId = BuiltInRegistries.BLOCK.getKey(block);

        Integer oreXp = ORE_XP.get(blockId);
        if (oreXp == null) {
            return 0; // порода без XP
        }

        ItemStack tool = player.getMainHandItem();
        boolean silkTouch = EnchantmentHelper.getItemEnchantmentLevel(
                world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH),
                tool) > 0;
        if (silkTouch) {
            return 0; // шёлковое касание — без XP и без бонусного дропа
        }

        OreBonus bonus = ORE_BONUS.get(blockId);
        if (bonus != null) {
            ProfessionData data = ModAttachments.get(player);
            int bonusAmount = (int) data.customScalarTotal(bonus.scalarKey());
            if (bonusAmount > 0) {
                ItemEntity entity = new ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                        new ItemStack(bonus.bonusItem(), bonusAmount));
                world.addFreshEntity(entity);
            }
        }

        return oreXp;
    }
}