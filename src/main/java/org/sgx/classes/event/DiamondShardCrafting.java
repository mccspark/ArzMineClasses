package org.sgx.classes.event;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.item.ModItems;

/**
 * Любой игрок (без привязки к классу) может расколоть алмаз молотом: алмаз в оффхенде,
 * молот (обычный или медный) в главной руке — ПКМ в воздух. Молот теряет 1 прочности
 * через штатный hurtAndBreak (а не ручное manipulation setDamageValue, как было раньше
 * в SharpeningTrigger/DiamondShardCrafting v1 — эта версия чище).
 *
 * Так же работает и разбиение осколка редкости: осколок в оффхенде, молот в главной руке,
 * ПКМ в воздух — осколок распадается на 4 фрагмента той же редкости (обратно рецепту
 * 4 фрагмента -> осколок).
 */
public final class DiamondShardCrafting {

    private static final int SHARDS_PER_DIAMOND = 9;
    private static final int FRAGMENTS_PER_SHARD = 4;
    private static final int HAMMER_DAMAGE_PER_USE = 1;

    private DiamondShardCrafting() {
    }

    /** Если предмет — осколок редкости, возвращает стопку его фрагментов; иначе пустой стек. */
    private static ItemStack fragmentsFor(ItemStack shard) {
        if (shard.isEmpty()) {
            return ItemStack.EMPTY;
        }
        for (var entry : ModItems.RARITY_SHARDS.entrySet()) {
            if (shard.is(entry.getValue())) {
                ItemRarity rarity = entry.getKey();
                return new ItemStack(ModItems.RARITY_FRAGMENTS.get(rarity), FRAGMENTS_PER_SHARD);
            }
        }
        return ItemStack.EMPTY;
    }

    /** Удар молота: звон по наковальне, хруст осколков и россыпь искр перед игроком. */
    private static void playSplitEffects(ServerLevel level, ServerPlayer player) {
        Vec3 pos = player.getEyePosition().add(player.getLookAngle().scale(0.9));
        level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.35f, 1.7f);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.CRIT, pos.x, pos.y - 0.3, pos.z, 14, 0.15, 0.15, 0.15, 0.25);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, pos.x, pos.y - 0.3, pos.z, 10, 0.2, 0.2, 0.2, 0.2);
    }

    public static void init() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world.isClientSide() || hand != InteractionHand.MAIN_HAND || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();

            boolean mainIsHammer = mainHand.is(ModItems.BLACKSMITH_HAMMER) || mainHand.is(ModItems.COPPER_HAMMER);
            if (!mainIsHammer) {
                return InteractionResult.PASS;
            }

            ItemStack output;
            if (offHand.is(Items.DIAMOND)) {
                output = new ItemStack(ModItems.DIAMOND_SHARD, SHARDS_PER_DIAMOND);
            } else {
                output = fragmentsFor(offHand);
                if (output.isEmpty()) {
                    return InteractionResult.PASS;
                }
            }

            offHand.shrink(1);

            if (!player.getInventory().add(output)) {
                player.drop(output, false);
            }

            playSplitEffects((ServerLevel) player.level(), serverPlayer);

            mainHand.hurtAndBreak(HAMMER_DAMAGE_PER_USE, (ServerLevel) player.level(), serverPlayer, item ->
                    serverPlayer.sendSystemMessage(Component.translatable("message.arz_classes.hammer_broke")));

            return InteractionResult.SUCCESS;
        });
    }
}