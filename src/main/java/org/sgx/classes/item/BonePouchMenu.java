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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemContainerContents;
import org.sgx.classes.block.ModMenus;

import java.util.ArrayList;
import java.util.List;

public class BonePouchMenu extends AbstractContainerMenu {

    public static final int SLOTS = 9;

    private final Container container;
    private final Player owner;
    private final InteractionHand hand;

    /** Клиентский конструктор (нужен MenuType-фабрике) — пустая заглушка. */
    public BonePouchMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(SLOTS), null);
    }

    public BonePouchMenu(int containerId, Inventory playerInventory, Container container, InteractionHand hand) {
        super(ModMenus.BONE_POUCH, containerId);
        this.container = container;
        this.owner = playerInventory.player;
        this.hand = hand;

        for( int i = 0;
        i < SLOTS;
        i++){
            addSlot(new Slot(container, i, 8 + i * 18, 31) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return stack.is(Items.BONE_MEAL);
                }
            });
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 77 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 135));
        }
    }
    public Container container() {
        return container;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack original = slot.getItem();
            newStack = original.copy();
            if (index < SLOTS) {
                if (!moveItemStackTo(original, SLOTS, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                if (!original.is(Items.BONE_MEAL) || !moveItemStackTo(original, 0, SLOTS, false)) {
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
        ItemStack current = owner.getItemInHand(hand);
        return current.getItem() instanceof BonePouchItem;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide() && owner != null && hand != null) {
            ItemStack pouch = owner.getItemInHand(hand);
            if (pouch.getItem() instanceof BonePouchItem) {
                List<ItemStack> items = new ArrayList<>();
                for (int i = 0; i < container.getContainerSize(); i++) {
                    items.add(container.getItem(i));
                }
                pouch.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
            }
        }
    }
}