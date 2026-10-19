package org.sgx.classes.profession;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.network.SharpeningZone;

import java.util.concurrent.ThreadLocalRandom;

public final class ItemRepair {

    private static final int PIECES_PER_FULL = 10;

    private static final float FULL_PERFECT_PERCENT = 0.5f;

    private static final float PIECE_PERFECT_PERCENT = FULL_PERFECT_PERCENT / PIECES_PER_FULL;

    private static final int XP_BAD = 60;
    private static final int XP_MEDIUM = 150;
    private static final int XP_PERFECT = 300;

    private ItemRepair() {
    }

    public static boolean isLastDurability(ItemStack stack) {
        int max = stack.getMaxDamage();
        return max > 0 && max - stack.getDamageValue() <= 1;
    }

    public static void applyResult(ServerPlayer player, SharpeningZone zone, Item expectedMaterial) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }

        if (!materialStillValid(player, expectedMaterial)) {
            player.sendSystemMessage(Component.translatable("message.arz_classes.repair_material_changed"));
            return;
        }

        RepairMaterials.Tier tier = expectedMaterial != null
                ? RepairMaterials.categorize(expectedMaterial)
                : RepairMaterials.Tier.PIECE;
        SharpeningPenalty.clearLegacyBroken(stack);
        if (isLastDurability(stack) && zone != SharpeningZone.PERFECT) {
            if (zone == SharpeningZone.MISS) {
                consumeOffhandMaterial(player);
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 1f, 1f);
                player.sendSystemMessage(Component.translatable("message.arz_classes.repair_item_broken"));
            } else {
                player.sendSystemMessage(Component.translatable("message.arz_classes.repair_needs_perfect"));
            }
            return;
        }

        float percent = percentFor(zone, tier);
        if (percent <= 0f) {
            player.sendSystemMessage(Component.translatable("message.arz_classes.repair_failed"));
            return;
        }

        int maxDamage = stack.getMaxDamage();
        if (maxDamage <= 0) {
            return;
        }
        int wanted = Math.max(1, Math.round(maxDamage * percent));
        int before = stack.getDamageValue();
        int restored = Math.min(wanted, before);
        stack.setDamageValue(before - restored);
        float shown = restored >= wanted ? percent * 100f : restored * 100f / maxDamage;

        consumeOffhandMaterial(player);
        player.sendSystemMessage(Component.translatable(
                zone == SharpeningZone.PERFECT
                        ? "message.arz_classes.repair_success"
                        : "message.arz_classes.repair_partial",
                formatPercent(shown)));

        int baseXp = switch (zone) {
            case BAD -> XP_BAD;
            case MEDIUM -> XP_MEDIUM;
            case PERFECT -> XP_PERFECT;
            case MISS -> 0;
        };
        int xp = scaledXp(baseXp, tier);
        if (xp > 0) {
            ProfessionFeedback.grantXp(player, ProfessionType.BLACKSMITH, xp);
        }
    }

    private static String formatPercent(float value) {
        float rounded = Math.round(value * 10f) / 10f;
        return rounded == Math.floor(rounded) ? Integer.toString((int) rounded) : Float.toString(rounded);
    }

    private static int scaledXp(int baseXp, RepairMaterials.Tier tier) {
        if (tier != RepairMaterials.Tier.PIECE) {
            return baseXp;
        }
        double value = baseXp / (double) PIECES_PER_FULL;
        int xp = (int) value;
        if (ThreadLocalRandom.current().nextDouble() < value - xp) {
            xp++;
        }
        return xp;
    }

    private static boolean materialStillValid(ServerPlayer player, Item expectedMaterial) {
        if (expectedMaterial == null) {
            return true;
        }
        ItemStack offHand = player.getOffhandItem();
        return !offHand.isEmpty() && offHand.is(expectedMaterial);
    }

    private static float percentFor(SharpeningZone zone, RepairMaterials.Tier tier) {
        float maxPercent = tier == RepairMaterials.Tier.PIECE ? PIECE_PERFECT_PERCENT : FULL_PERFECT_PERCENT;
        return switch (zone) {
            case MISS -> 0f;
            case BAD -> maxPercent / 4f;
            case MEDIUM -> maxPercent / 2f;
            case PERFECT -> maxPercent;
        };
    }

    private static void consumeOffhandMaterial(ServerPlayer player) {
        ItemStack offHand = player.getOffhandItem();
        if (!offHand.isEmpty()) {
            offHand.shrink(1);
        }
    }
}