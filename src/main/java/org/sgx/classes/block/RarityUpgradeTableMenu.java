package org.sgx.classes.block;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.item.ModDataComponents;
import org.sgx.classes.item.ModItems;
import org.sgx.classes.profession.RarityUpgradeChances;

public class RarityUpgradeTableMenu extends AbstractContainerMenu {

    /** Слоты стола в меню: 0 — предмет, 1 — осколок, 2 — результат. */
    private static final int TABLE_SLOTS = 3;
    /** Инвентарь игрока: 3..29, хотбар: 30..38. */
    private static final int HOTBAR_START = TABLE_SLOTS + 27;

    private final Container container;

    /** Клиентский конструктор (нужен MenuType-фабрике) — пустой контейнер-заглушка,
     * реальные стеки придут по сети как для любого AbstractContainerMenu. */
    public RarityUpgradeTableMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(3));
    }

    public RarityUpgradeTableMenu(int containerId, Inventory playerInventory, Container container) {
        super(ModMenus.RARITY_UPGRADE_TABLE, containerId);
        this.container = container;
        addSlot(new Slot(container, RarityUpgradeTableBlockEntity.SLOT_ITEM, 62, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                ItemRarity current = stack.getOrDefault(ModDataComponents.ITEM_RARITY, ItemRarity.COMMON);
                return stack.getMaxDamage() > 0 && RarityUpgradeChances.next(current) != null;
            }
        });
        addSlot(new Slot(container, RarityUpgradeTableBlockEntity.SLOT_SHARD, 98, 22) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return ModItems.RARITY_SHARDS.containsValue(stack.getItem());
            }
        });
        addSlot(new Slot(container, RarityUpgradeTableBlockEntity.SLOT_OUTPUT, 80, 58) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false; // только машина кладёт сюда результат, руками нельзя
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 110 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 168));
        }
    }

    public Container container() {
        return container;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < TABLE_SLOTS) {
            // Из стола (предмет, осколок или результат) — в инвентарь, начиная с конца как у ванильных результатов
            if (!this.moveItemStackTo(stack, TABLE_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Из инвентаря — в слоты стола; mayPlace сам отсеет неподходящее (предмет -> слот предмета, осколок -> слот осколка)
            if (!this.moveItemStackTo(stack, 0, 2, false)) {
                // В стол не подошло: переносим между инвентарём и хотбаром
                if (index < HOTBAR_START) {
                    if (!this.moveItemStackTo(stack, HOTBAR_START, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.moveItemStackTo(stack, TABLE_SLOTS, HOTBAR_START, false)) {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        if (stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY; // ничего не сдвинулось
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}