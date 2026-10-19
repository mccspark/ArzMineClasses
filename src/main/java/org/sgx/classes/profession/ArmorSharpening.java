package org.sgx.classes.profession;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.network.SharpeningZone;

public final class ArmorSharpening {

    // Единственный тип заточки (бывшая "яркая зелёная"): теперь это просто PERFECT.
    private static final double DURABILITY_PERCENT = 1.0;
    private static final float ARMOR_PER_LEVEL = 0.25f;

    private static final int XP_PERFECT = 225;

    private static final Identifier ARMOR_BONUS_ID =
            Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "armor_sharpen_bonus");

    private ArmorSharpening() {
    }

    public static int currentLevel(ItemStack stack) {
        return SharpenName.level(stack);
    }
    public static void applyResult(ServerPlayer player, SharpeningZone zone) {
        ItemStack stack = player.getMainHandItem();
        if (stack.isEmpty()) {
            return;
        }
        // на последней прочности затачивать нельзя (мог измениться, пока шла мини-игра)
        if (ItemRepair.isLastDurability(stack)) {
            player.sendSystemMessage(Component.translatable("message.arz_classes.sharpen_too_damaged"));
            return;
        }
        SharpeningPenalty.clearLegacyBroken(stack);

        switch (zone) {
            case MISS -> {
                SharpeningPenalty.dropToOneDurability(stack);
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.ITEM_BREAK.value(), SoundSource.PLAYERS, 1f, 1f);
                player.sendSystemMessage(Component.translatable("message.arz_classes.sharpen_miss"));
            }
            case BAD -> {
                SharpeningPenalty.loseDurabilityPercent(stack, SharpeningPenalty.BAD_LOSS_PERCENT);
                player.sendSystemMessage(Component.translatable("message.arz_classes.sharpen_bad"));
            }
            case MEDIUM -> {
                SharpeningPenalty.loseDurabilityPercent(stack, SharpeningPenalty.MEDIUM_LOSS_PERCENT);
                player.sendSystemMessage(Component.translatable("message.arz_classes.sharpen_medium"));
            }
            case PERFECT -> upgrade(player, stack);
        }
    }

    private static void upgrade(ServerPlayer player, ItemStack stack) {
        org.sgx.classes.profession.ProfessionData data = org.sgx.classes.attachment.ModAttachments.get(player);
        int cap = SharpeningCaps.armorCap(data);

        int level = currentLevel(stack);
        if (level >= cap) {
            player.sendSystemMessage(Component.translatable("message.arz_classes.already_max_sharpened"));
            return;
        }
        int newLevel = level + 1;

        int currentMax = stack.getMaxDamage();
        if (currentMax > 0 && DURABILITY_PERCENT > 0) {
            int baseMax = baseMaxDamage(stack, currentMax);
            // накопленный бонус = ровно уровень × процент от БАЗОВОЙ прочности, без накопления ошибок округления
            long bonus = Math.round(baseMax * DURABILITY_PERCENT * newLevel / 100.0)
                    - Math.round(baseMax * DURABILITY_PERCENT * level / 100.0);
            if (bonus > 0) {
                stack.set(DataComponents.MAX_DAMAGE, currentMax + (int) bonus);
            }
        }

        ItemAttributeModifiers existing = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        float currentArmorBonus = 0f;
        for (ItemAttributeModifiers.Entry entry : existing.modifiers()) {
            if (entry.modifier().id().equals(ARMOR_BONUS_ID)) {
                currentArmorBonus = (float) entry.modifier().amount();
            } else {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        builder.add(Attributes.ARMOR,
                new AttributeModifier(ARMOR_BONUS_ID, currentArmorBonus + ARMOR_PER_LEVEL,
                        AttributeModifier.Operation.ADD_VALUE),
                EquipmentSlotGroup.ARMOR);
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());

        SharpenName.apply(stack, newLevel);

        player.level().playSound(null, player.blockPosition(),
                SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1f, 1f);
        player.sendSystemMessage(Component.translatable("message.arz_classes.sharpen_success", newLevel));

        ProfessionFeedback.grantXp(player, ProfessionType.BLACKSMITH, XP_PERFECT);
    }

    private static int baseMaxDamage(ItemStack stack, int fallback) {
        ItemStack fresh = new ItemStack(stack.getItem());
        int freshMax = fresh.getMaxDamage();
        return freshMax > 0 ? freshMax : fallback;
    }
}