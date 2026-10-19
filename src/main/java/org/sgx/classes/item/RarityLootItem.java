package org.sgx.classes.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.sgx.classes.profession.RarityCraftHandler;

import java.util.List;

public class RarityLootItem extends Item {

    private final ItemRarity rarity;

    public RarityLootItem(Properties properties, ItemRarity rarity) {
        super(properties);
        this.rarity = rarity;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        List<Item> eligible = BuiltInRegistries.ITEM.stream()
                .filter(item -> RarityCraftHandler.isEligible(item.getDefaultInstance()))
                .toList();
        if (eligible.isEmpty()) {
            return InteractionResult.FAIL;
        }

        Item chosen = eligible.get(level.getRandom().nextInt(eligible.size()));
        ItemStack result = new ItemStack(chosen);
        result.set(ModDataComponents.ITEM_RARITY, this.rarity);

        if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }

        player.getItemInHand(hand).shrink(1);
        return InteractionResult.CONSUME;
    }
}