package org.sgx.classes.profession;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.item.ModDataComponents;

/**
 * Чинит предметы, у которых максимальная прочность раздулась из-за бага с объединением (см. RepairMerge).
 *
 * Точный «правильный» максимум восстановить нельзя (бонус кузнеца на момент крафта нигде не хранился),
 * поэтому считаем ПОТОЛОК: база предмета * (1 + максимально возможный бонус кузнеца/бронника/оружейника)
 * * множитель редкости + бонус заточки по уровню в названии. Всё, что выше потолка, срезается до него,
 * а износ пересчитывается пропорционально. Легальные предметы под потолком не затрагиваются, поэтому
 * проход безопасно запускать сколько угодно раз.
 */
public final class DurabilityNormalizer {

    /** Макс. суммарный бонус прочности при крафте: blacksmith 2x7% + оружейник/бронник до 4x7%. */
    private static final double MAX_CRAFT_BONUS = 0.42;
    private static final double WEAPON_SHARPEN_PERCENT = 4.0;
    private static final double ARMOR_SHARPEN_PERCENT = 1.0;

    private DurabilityNormalizer() {
    }

    public static int normalizePlayer(ServerPlayer player) {
        return normalizeContainer(player.getInventory()) + normalizeContainer(player.getEnderChestInventory());
    }

    /** Сундуки, бочки, воронки, стойки и т.п.: проходит по всем слотам, возвращает число исправленных. */
    public static int normalizeContainer(Container container) {
        int fixed = 0;
        for (int i = 0; i < container.getContainerSize(); i++) {
            if (normalize(container.getItem(i))) {
                fixed++;
            }
        }
        if (fixed > 0) {
            container.setChanged();
        }
        return fixed;
    }

    /** @return true, если максимум прочности был срезан. */
    public static boolean normalize(ItemStack stack) {
        if (stack.isEmpty() || !RarityEligibility.isEligible(stack)) {
            return false;
        }
        int max = stack.getMaxDamage();
        int base = new ItemStack(stack.getItem()).getMaxDamage();
        if (base <= 0 || max <= base) {
            return false;
        }

        ItemRarity rarity = stack.get(ModDataComponents.ITEM_RARITY);
        double rarityMul = 1.0 + (rarity == null ? 0.0 : Math.max(0.0, rarity.statBonusPercent()));
        double sharpenPercent = RarityEligibility.isArmor(stack) ? ARMOR_SHARPEN_PERCENT : WEAPON_SHARPEN_PERCENT;
        int sharpenLevel = SharpenName.level(stack);

        long cap = Math.round(base * (1.0 + MAX_CRAFT_BONUS) * rarityMul)
                + Math.round(base * sharpenPercent * sharpenLevel / 100.0);
        if (max <= cap) {
            return false;
        }

        int newMax = (int) cap;
        int newDamage = (int) Math.round(stack.getDamageValue() * (double) newMax / max);
        stack.set(DataComponents.MAX_DAMAGE, newMax);
        stack.setDamageValue(Math.min(newDamage, Math.max(0, newMax - 1)));
        return true;
    }
}
