package org.sgx.classes.profession;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.sgx.classes.Arz_classes;

import java.util.List;

/**
 * Зачарование Shyllow (data/arz_classes/enchantment/shyllow.json) — косметическое, без эффектов.
 *
 * Выпадает ТОЛЬКО на булаву и удочку и только когда ванильных чар на этой силе нет («пустой» вариант стола
 * зачарований, на котором ваниль ничего не делала). Предмет с Shyllow получает радужную анимированную модель
 * (assets/arz_classes/items/shyllow_*.json) через компонент item_model, ванильное свечение отключается.
 */
public final class ShyllowEnchant {

    public static final ResourceKey<Enchantment> KEY =
            ResourceKey.create(Registries.ENCHANTMENT, Arz_classes.id("shyllow"));

    private ShyllowEnchant() {
    }

    public static boolean isEligible(ItemStack stack) {
        return stack.is(Items.FISHING_ROD) || stack.is(Items.MACE);
    }

    /** Если ванильный список пуст и предмет подходит — возвращает [Shyllow I], иначе исходный список. */
    public static List<EnchantmentInstance> fallbackIfEmpty(RegistryAccess access, ItemStack stack,
                                                            List<EnchantmentInstance> list) {
        if (!list.isEmpty() || !isEligible(stack)) {
            return list;
        }
        return access.lookupOrThrow(Registries.ENCHANTMENT).get(KEY)
                .<List<EnchantmentInstance>>map(holder -> List.of(new EnchantmentInstance(holder, 1)))
                .orElse(list);
    }

    public static boolean hasShyllow(ItemStack stack) {
        return stack.getEnchantments().keySet().stream().anyMatch(holder -> holder.is(KEY));
    }

    /**
     * Синхронизирует радужный вид с наличием чара: есть Shyllow — радужная модель и без ванильного свечения;
     * чара нет (его снял точило) — обычный вид. Безопасно вызывать на любом стеке.
     */
    public static void applyVisuals(ItemStack stack) {
        if (!isEligible(stack)) {
            return;
        }
        Identifier model = Arz_classes.id(stack.is(Items.MACE) ? "shyllow_mace" : "shyllow_fishing_rod");
        if (hasShyllow(stack)) {
            stack.set(DataComponents.ITEM_MODEL, model);
            stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        } else if (model.equals(stack.get(DataComponents.ITEM_MODEL))) {
            stack.remove(DataComponents.ITEM_MODEL);
            stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        }
    }
}