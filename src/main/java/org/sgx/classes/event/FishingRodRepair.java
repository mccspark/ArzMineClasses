package org.sgx.classes.event;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.sgx.classes.item.ModItems;

/**
 * Починка удочки набором: удочка в одной руке, набор в другой, ПКМ -> +10% от максимальной
 * прочности, набор тратится. Доступно любому игроку (без привязки к классу).
 * Если удочка целая — ничего не происходит, ПКМ забрасывает её как обычно.
 */
public final class FishingRodRepair {

    /** Доля максимальной прочности, которую возвращает один набор. */
    private static final float REPAIR_FRACTION = 0.20f;

    private FishingRodRepair() {
    }

    public static void init() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            // обрабатываем один раз, на основной руке — она идёт первой и перебивает заброс удочки
            if (hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();

            ItemStack rod;
            ItemStack kit;
            if (main.is(Items.FISHING_ROD) && off.is(ModItems.FISHING_ROD_REPAIR_KIT)) {
                rod = main;
                kit = off;
            } else if (main.is(ModItems.FISHING_ROD_REPAIR_KIT) && off.is(Items.FISHING_ROD)) {
                rod = off;
                kit = main;
            } else {
                return InteractionResult.PASS;
            }

            // целая удочка: набор не трогаем, пусть работает обычный заброс
            if (rod.getDamageValue() <= 0) {
                return InteractionResult.PASS;
            }

            if (world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.SUCCESS; // на клиенте только гасим заброс, сам ремонт — на сервере
            }

            int maxDamage = rod.getMaxDamage();
            int before = rod.getDamageValue();
            int wanted = Math.max(1, Math.round(maxDamage * REPAIR_FRACTION));
            int restored = Math.min(wanted, before);
            rod.setDamageValue(before - restored);

            if (!serverPlayer.isCreative()) {
                kit.shrink(1);
            }

            float shown = Math.round(restored * 1000f / maxDamage) / 10f;
            String text = shown == Math.floor(shown) ? Integer.toString((int) shown) : Float.toString(shown);
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.arz_classes.rod_repair_success", text), true);
            world.playSound(null, serverPlayer.blockPosition(), SoundEvents.ANVIL_USE,
                    SoundSource.PLAYERS, 0.6f, 1.4f);
            return InteractionResult.SUCCESS;
        });
    }
}