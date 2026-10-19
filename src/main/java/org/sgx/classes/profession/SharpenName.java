package org.sgx.classes.profession;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** Суффикс заточки « +n» в названии предмета: переводимое имя + " +n", без курсива. */
public final class SharpenName {

    private SharpenName() {
    }

    public static int level(ItemStack stack) {
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        if (name == null) {
            return 0;
        }
        String text = name.getString();
        for (int n = 10; n >= 1; n--) {
            if (text.endsWith(" +" + n)) {
                return n;
            }
        }
        return 0;
    }

    public static void apply(ItemStack stack, int newLevel) {
        Component custom = stack.get(DataComponents.CUSTOM_NAME);
        Component base;
        if (custom == null) {
            base = stack.getItemName();
        } else {
            List<Component> siblings = custom.getSiblings();
            if (siblings.size() == 2 && siblings.get(1).getString().matches(" \\+\\d+")) {
                // уже наш формат: берём базовое имя как есть
                base = siblings.get(0);
            } else if (level(stack) > 0) {
                // старый формат (склеенная строка) — восстанавливаем переводимое имя
                base = stack.getItemName();
            } else {
                // имя, данное игроком (наковальня) — сохраняем
                base = custom;
            }
        }
        Component result = Component.empty()
                .append(base)
                .append(Component.literal(" +" + newLevel))
                .withStyle(style -> style.withItalic(false));
        stack.set(DataComponents.CUSTOM_NAME, result);
    }
}