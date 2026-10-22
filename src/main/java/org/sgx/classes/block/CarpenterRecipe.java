package org.sgx.classes.block;

import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.sgx.classes.block.chest.ModChests;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public record CarpenterRecipe(List<Input> inputs, ItemStack result) {

    public record Input(Predicate<ItemStack> matcher, ItemStack display, int count) {
        public static Input of(ItemLike item, int count) {
            Item i = item.asItem();
            return new Input(s -> s.is(i), new ItemStack(i), count);
        }

        public static Input tag(TagKey<Item> tag, ItemLike display, int count) {
            return new Input(s -> s.is(tag), new ItemStack(display), count);
        }
    }

    /** Все рецепты верстака. Порядок = порядок кнопок в центре. */
    public static final class Registry {
        private Registry() {}

        public static final List<CarpenterRecipe> ALL = build();

        private static List<CarpenterRecipe> build() {
            List<CarpenterRecipe> list = new ArrayList<>();
            for (String wood : ModChests.WOODS) {
                list.add(new CarpenterRecipe(
                        List.of(Input.of(ModChests.planksFor(wood), 8)),
                        new ItemStack(ModChests.chestItem(wood), 1)));
            }
            // оружейные стойки из разных пород дерева: 6 досок + 2 палки
            for (String wood : ModChests.WOODS) {
                list.add(new CarpenterRecipe(
                        List.of(Input.of(ModChests.planksFor(wood), 6), Input.of(Items.STICK, 2)),
                        new ItemStack(ModWeaponRacks.item(wood), 1)));
            }
            // сундуки-ловушки: свой сундук той же породы + крюк с растяжкой
            for (String wood : ModChests.WOODS) {
                list.add(new CarpenterRecipe(
                        List.of(Input.of(ModChests.chestItem(wood), 1), Input.of(Items.TRIPWIRE_HOOK, 1)),
                        new ItemStack(ModChests.trappedChestItem(wood), 1)));
            }
            // стеклянные двери: 4 доски + 2 стекла -> 3 двери (как у ванильной двери)
            for (String wood : ModGlassDoors.WOODS) {
                list.add(new CarpenterRecipe(
                        List.of(Input.of(ModChests.planksFor(wood), 4), Input.of(Items.GLASS, 2)),
                        new ItemStack(ModGlassDoors.item(wood), 3)));
            }
            return List.copyOf(list);
        }
    }
}