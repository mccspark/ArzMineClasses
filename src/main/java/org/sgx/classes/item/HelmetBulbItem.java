package org.sgx.classes.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Лампочка для каски шахтёра. Лампочка в одной руке, каска (заряженная или разряженная) в другой, ПКМ:
 * каска чинится на {@link #RESTORE} прочности, разряженная снова начинает светить, лампочка расходуется.
 */
public class HelmetBulbItem extends Item {

    public static final int RESTORE = 100;

    public HelmetBulbItem(Properties properties) {
        super(properties);
    }

    public static boolean isHelmet(ItemStack stack) {
        return stack.is(ModItems.MINER_HELMET) || stack.is(ModItems.MINER_HELMET_OFF);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack bulb = player.getItemInHand(hand);
        ItemStack helmet = player.getItemInHand(other);
        if (!isHelmet(helmet)) {
            return InteractionResult.PASS;
        }
        if (helmet.getDamageValue() <= 0) {
            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.translatable("message.arz_classes.helmet_bulb_full"), true);
            }
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        int damage = Math.max(0, helmet.getDamageValue() - RESTORE);
        ItemStack result = helmet.is(ModItems.MINER_HELMET_OFF) ? helmet.transmuteCopy(ModItems.MINER_HELMET) : helmet;
        result.setDamageValue(damage);
        player.setItemInHand(other, result);
        if (!player.getAbilities().instabuild) {
            bulb.shrink(1);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1f, 1.2f);
        if (player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.translatable("message.arz_classes.helmet_bulb_replaced"), true);
        }
        return InteractionResult.SUCCESS;
    }
}