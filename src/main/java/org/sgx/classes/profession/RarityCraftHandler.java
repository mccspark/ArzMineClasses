package org.sgx.classes.profession;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.item.ModDataComponents;
import org.sgx.classes.network.OpenRarityWheelPayload;

import java.util.List;

public final class RarityCraftHandler {

    private static final int XP_PER_DURABLE_CRAFT = 50;

    private RarityCraftHandler() {
    }

    /** Редкость получают только броня, оружие, инструменты, луки/арбалеты, щиты, удочки, молоты и каска
     * (см. RarityEligibility) — а не любой предмет с прочностью. */
    public static boolean isEligible(ItemStack stack) {
        return RarityEligibility.isEligible(stack);
    }

    public static OpenRarityWheelPayload handle(ServerPlayer player, ItemStack stack) {
        if (!isEligible(stack)) {
            return null;
        }
        ProfessionData data = ModAttachments.get(player);
        List<RarityWeights.Weight> weights = RarityWeights.compute(data);
        ItemRarity result = RarityRoller.roll(weights);

        stack.set(ModDataComponents.ITEM_RARITY, result);
        RarityStatApplier.apply(stack, result);
        applyDurabilityBonus(data, stack);
        RarityDurability.change(stack, null, result); // % прочности от самой редкости, поверх бонуса кузнеца

        // +5 опыта за крафт любого предмета, который может получить редкость —
        // кольцо опыта уже учитывается автоматически внутри grantXp().
        ProfessionFeedback.grantXp(player, ProfessionType.BLACKSMITH, XP_PER_DURABLE_CRAFT);

        float common = 0f, rare = 0f, epic = 0f, legendary = 0f, mythic = 0f;
        for (RarityWeights.Weight w : weights) {
            switch (w.rarity()) {
                case COMMON -> common = (float) w.chance();
                case RARE -> rare = (float) w.chance();
                case EPIC -> epic = (float) w.chance();
                case LEGENDARY -> legendary = (float) w.chance();
                case MYTHIC -> mythic = (float) w.chance();
            }
        }

        return new OpenRarityWheelPayload(common, rare, epic, legendary, mythic, result.ordinal());
    }

    /** blacksmith_durability (scalar "blacksmith.craft_durability_percent") действует на ЛЮБОЙ
     * предмет, способный получить редкость. armorer_durability добавляет СВОЙ бонус сверху на броню и щиты,
     * weaponsmith_durability — на оружие, инструменты, луки/арбалеты, удочки и молоты; общий складывается с ними. */
    private static void applyDurabilityBonus(ProfessionData data, ItemStack stack) {
        if (data.currentProfession() != ProfessionType.BLACKSMITH) {
            return;
        }
        double bonusPercent = data.customScalarTotal("blacksmith.craft_durability_percent");

        boolean armorLike = RarityEligibility.isArmor(stack) || RarityEligibility.isShield(stack);
        boolean weaponOrToolLike = RarityEligibility.isWeapon(stack) || RarityEligibility.isTool(stack)
                || RarityEligibility.isBow(stack) || RarityEligibility.isCrossbow(stack)
                || RarityEligibility.isFishingRod(stack) || RarityEligibility.isHammer(stack);

        if (armorLike) {
            bonusPercent += data.customScalarTotal("armorer.armor_durability_percent");
        }
        if (weaponOrToolLike) {
            bonusPercent += data.customScalarTotal("weaponsmith.tool_durability_percent");
        }

        if (bonusPercent <= 0) {
            return;
        }
        int currentMax = stack.getMaxDamage();
        if (currentMax <= 0) {
            return;
        }
        int newMax = currentMax + (int) Math.round(currentMax * bonusPercent / 100.0);
        stack.set(DataComponents.MAX_DAMAGE, newMax);
    }
}