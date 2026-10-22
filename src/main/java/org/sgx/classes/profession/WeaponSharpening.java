package org.sgx.classes.profession;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.network.SharpeningZone;

public final class WeaponSharpening {

    // Единственный тип заточки (бывшая "яркая зелёная"): теперь это просто PERFECT.
    private static final double DURABILITY_PERCENT = 4.0;
    private static final float DAMAGE_PER_LEVEL = 0.5f;
    private static final float EFFICIENCY_PER_LEVEL = 0.3f;

    private static final int XP_MEDIUM = 75;
    private static final int XP_PERFECT = 225;

    private static final Identifier ATTACK_BONUS_ID =
            Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "sharpen_attack_bonus");

    private static final Identifier EFFICIENCY_BONUS_ID =
            Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "sharpen_efficiency_bonus");

    private WeaponSharpening() {
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
                ProfessionFeedback.grantXp(player, ProfessionType.BLACKSMITH, XP_MEDIUM);
            }
            case PERFECT -> upgrade(player, stack);
        }
    }

    /** Любое копьё: по id предмета (wooden_spear, iron_spear, ... и модовые *_spear). */
    public static boolean isSpear(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_spear");
    }

    /** Любой нож (farmersdelight:iron_knife, flint_knife и т.д.): по id предмета, как и копья. */
    public static boolean isKnife(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("_knife");
    }

    private static void upgrade(ServerPlayer player, ItemStack stack) {
        org.sgx.classes.profession.ProfessionData data = org.sgx.classes.attachment.ModAttachments.get(player);
        int cap = SharpeningCaps.weaponOrToolCap(stack, data);

        int level = currentLevel(stack);
        if (level >= cap) {
            player.sendSystemMessage(Component.translatable("message.arz_classes.already_max_sharpened"));
            return;
        }
        int newLevel = level + 1;

        // Прочность: % считается от БАЗОВОГО (заводского) максимума предмета.
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

        boolean isWeapon = stack.is(ItemTags.SWORDS) || stack.is(Items.TRIDENT) || stack.is(ItemTags.AXES)
                || isSpear(stack) || isKnife(stack);
        boolean isTool = stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS)
                || stack.is(ItemTags.HOES) || stack.is(ItemTags.AXES);

        // ИНКРЕМЕНТ: читаем накопленный бонус и прибавляем бонус этого апгрейда.
        ItemAttributeModifiers existing = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        float currentAttackBonus = 0f;
        float currentEfficiencyBonus = 0f;
        for (ItemAttributeModifiers.Entry entry : existing.modifiers()) {
            if (entry.modifier().id().equals(ATTACK_BONUS_ID)) {
                currentAttackBonus = (float) entry.modifier().amount();
            } else if (entry.modifier().id().equals(EFFICIENCY_BONUS_ID)) {
                currentEfficiencyBonus = (float) entry.modifier().amount();
            } else {
                builder.add(entry.attribute(), entry.modifier(), entry.slot());
            }
        }

        if (isWeapon) {
            builder.add(Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(ATTACK_BONUS_ID, currentAttackBonus + DAMAGE_PER_LEVEL,
                            AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
        if (isTool) {
            builder.add(Attributes.MINING_EFFICIENCY,
                    new AttributeModifier(EFFICIENCY_BONUS_ID, currentEfficiencyBonus + EFFICIENCY_PER_LEVEL,
                            AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());

        SharpenName.apply(stack, newLevel);

        // Звук наковальни — только при удачной заточке. Играем на сервере, слышат все рядом.
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.ANVIL_USE, SoundSource.PLAYERS, 1f, 1f);
        player.sendSystemMessage(Component.translatable("message.arz_classes.sharpen_success", newLevel));

        org.sgx.classes.item.ClassBookHooks.onSharpened(player, "weapon");
        ProfessionFeedback.grantXp(player, ProfessionType.BLACKSMITH, XP_PERFECT);
    }

    private static int baseMaxDamage(ItemStack stack, int fallback) {
        ItemStack fresh = new ItemStack(stack.getItem());
        int freshMax = fresh.getMaxDamage();
        return freshMax > 0 ? freshMax : fallback;
    }
}