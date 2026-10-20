package org.sgx.classes.item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.sgx.classes.block.ModMenus;

import java.util.ArrayList;
import java.util.List;

public class FoodPouchMenu extends AbstractContainerMenu {

    public static final int SLOTS = FoodPouchItem.SLOT_COUNT;

    private final Container container;
    private final Player owner;
    private final InteractionHand hand;
    /** Конкретный стек мешочка, открытый на сервере (null на клиенте). */
    private final ItemStack pouchStack;

    /** Клиентский конструктор (нужен MenuType-фабрике). */
    public FoodPouchMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(SLOTS), null, null);
    }

    public FoodPouchMenu(int containerId, Inventory playerInventory, Container container,
                         InteractionHand hand, ItemStack pouchStack) {
        super(ModMenus.FOOD_POUCH, containerId);
        this.container = container;
        this.owner = playerInventory.player;
        this.hand = hand;
        this.pouchStack = pouchStack;

        // 5 слотов по центру: в текстуре они начинаются с x = 44 (176 - 5*18 = 86, /2 = 43, +1 рамка)
        for (int i = 0; i < SLOTS; i++) {
            addSlot(new FoodSlot(container, i, 44 + i * 18, 31));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new LockedPouchSlot(playerInventory, col + row * 9 + 9, 8 + col * 18, 77 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new LockedPouchSlot(playerInventory, col, 8 + col * 18, 135));
        }
    }

    public Container container() {
        return container;
    }

    /** Слот мешочка: только еда, максимум 8 штук. */
    private static final class FoodSlot extends Slot {
        FoodSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return FoodPouchItem.isFood(stack);
        }

        @Override
        public int getMaxStackSize() {
            return FoodPouchItem.SLOT_CAPACITY;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return Math.min(FoodPouchItem.SLOT_CAPACITY, stack.getMaxStackSize());
        }
    }

    /** Слот инвентаря игрока; сам открытый мешочек из него вытащить нельзя (иначе содержимое потеряется). */
    private final class LockedPouchSlot extends Slot {
        LockedPouchSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        private boolean isOpenPouch() {
            return pouchStack != null && getItem() == pouchStack;
        }

        @Override
        public boolean mayPickup(Player player) {
            return !isOpenPouch() && super.mayPickup(player);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !isOpenPouch() && super.mayPlace(stack);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack original = slot.getItem();
            if (pouchStack != null && original == pouchStack) {
                return ItemStack.EMPTY;
            }
            newStack = original.copy();
            if (index < SLOTS) {
                if (!moveItemStackTo(original, SLOTS, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!FoodPouchItem.isFood(original) || !moveItemStackTo(original, 0, SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }
            if (original.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY, newStack);
            } else {
                slot.setChanged();
            }
        }
        return newStack;
    }

    @Override
    public boolean stillValid(Player player) {
        if (hand == null || owner == null) {
            return true;
        }
        return owner.getItemInHand(hand).getItem() instanceof FoodPouchItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide() && owner != null && hand != null) {
            ItemStack pouch = owner.getItemInHand(hand);
            if (pouch.getItem() instanceof FoodPouchItem) {
                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < container.getContainerSize(); i++) {
                    items.add(container.getItem(i));
                }
                pouch.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
                FoodPouchItem.refreshModel(pouch, true);
                FoodPouchItem.playSound(player, net.minecraft.sounds.SoundEvents.BUNDLE_INSERT, 0.7f, 0.9f);
            }
        }
    }
}