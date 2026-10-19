package org.sgx.classes.block;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.profession.ProfessionFeedback;
import org.sgx.classes.profession.ProfessionType;

import java.util.List;

public class CarpenterWorkbenchMenu extends AbstractContainerMenu {
    private static final int IN = CarpenterWorkbenchEntity.INPUT_START;
    private static final int IN_END = IN + CarpenterWorkbenchEntity.INPUT_SIZE;
    private static final int OUT = CarpenterWorkbenchEntity.OUTPUT_START;
    private static final int OUT_END = OUT + CarpenterWorkbenchEntity.OUTPUT_SIZE;

    /** Опыт Лесоруба за один крафт (за каждый скрафченный рецепт). */
    private static final int XP_PER_CRAFT = 10;

    /** id кнопки меню: скрафтить выбранный рецепт один раз. */
    public static final int BUTTON_CRAFT = 0;
    /** id кнопки меню: скрафтить выбранный рецепт на сколько хватит ресурсов, но не больше одного стака результата (Shift+клик). */
    public static final int BUTTON_CRAFT_MAX = 1000;

    /** Верх блока слотов (под полосой выбора рецепта) и инвентаря игрока — как в фабрике, но высота окна 203. */
    private static final int SLOTS_Y = 55;
    private static final int PLAYER_INV_Y = 121;
    private static final int HOTBAR_Y = 179;

    private final Container container;

    /** Выбранный рецепт, синхронизируется на клиент: 0 = не выбран, иначе индекс + 1 (как в FactoryRecipe). */
    private final DataSlot selectedRecipe = DataSlot.standalone();

    /** Клиентский конструктор (для MenuType) */
    public CarpenterWorkbenchMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(CarpenterWorkbenchEntity.TOTAL_SIZE));
    }

    public CarpenterWorkbenchMenu(int containerId, Inventory playerInventory, Container container) {
        super(ModMenus.CARPENTER_WORKBENCH, containerId);
        checkContainerSize(container, CarpenterWorkbenchEntity.TOTAL_SIZE);
        this.container = container;

        // Входные слоты: 3 ряда x 4 столбца (индексы 0..11)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                addSlot(new Slot(container, IN + col + row * 4, 8 + col * 18, SLOTS_Y + row * 18));
            }
        }

        // Выходные слоты: 2 столбца x 3 ряда (индексы 12..17), класть в них нельзя
        for (int col = 0; col < 2; col++) {
            for (int row = 0; row < 3; row++) {
                addSlot(new Slot(container, OUT + row + col * 3, 134 + col * 18, SLOTS_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }

        // Инвентарь игрока (индексы 18..44) и хотбар (45..53)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, PLAYER_INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, HOTBAR_Y));
        }

        addDataSlot(selectedRecipe);
    }

    // ---------- выбранный рецепт ----------

    /** Выбранный рецепт или null. Работает и на клиенте (значение синхронизируется DataSlot'ом). */
    public CarpenterRecipe getSelectedRecipe() {
        List<CarpenterRecipe> all = CarpenterRecipe.Registry.ALL;
        int value = selectedRecipe.get();
        return (value >= 1 && value <= all.size()) ? all.get(value - 1) : null;
    }

    // ---------- логика крафта ----------

    public int countMatching(CarpenterRecipe.Input input) {
        int total = 0;
        for (int i = IN; i < IN_END; i++) {
            ItemStack s = container.getItem(i);
            if (!s.isEmpty() && input.matcher().test(s)) total += s.getCount();
        }
        return total;
    }

    private boolean hasRoomFor(ItemStack result) {
        int room = 0;
        for (int i = OUT; i < OUT_END; i++) {
            ItemStack cur = container.getItem(i);
            if (cur.isEmpty()) room += result.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(cur, result)) room += cur.getMaxStackSize() - cur.getCount();
        }
        return room >= result.getCount();
    }

    /** Вызывается и на клиенте (для подсветки), и на сервере. */
    public boolean canCraft(CarpenterRecipe recipe) {
        for (CarpenterRecipe.Input in : recipe.inputs()) {
            if (countMatching(in) < in.count()) return false;
        }
        return hasRoomFor(recipe.result());
    }

    /** Возвращает true, если крафт состоялся. */
    private boolean craft(CarpenterRecipe recipe) {
        if (!canCraft(recipe)) return false;

        // списываем входы
        for (CarpenterRecipe.Input in : recipe.inputs()) {
            int left = in.count();
            for (int i = IN; i < IN_END && left > 0; i++) {
                ItemStack s = container.getItem(i);
                if (!s.isEmpty() && in.matcher().test(s)) {
                    int take = Math.min(left, s.getCount());
                    s.shrink(take);
                    left -= take;
                }
            }
        }

        // кладём результат в выходные слоты
        ItemStack out = recipe.result().copy();
        for (int i = OUT; i < OUT_END && !out.isEmpty(); i++) {
            ItemStack cur = container.getItem(i);
            if (cur.isEmpty()) {
                int n = Math.min(out.getCount(), out.getMaxStackSize());
                container.setItem(i, out.copyWithCount(n));
                out.shrink(n);
            } else if (ItemStack.isSameItemSameComponents(cur, out)) {
                int n = Math.min(out.getCount(), cur.getMaxStackSize() - cur.getCount());
                cur.grow(n);
                out.shrink(n);
            }
        }
        container.setChanged();
        return true;
    }

    /** Крафтит подряд, пока хватает ресурсов и места, но не больше одного стака результата. Возвращает число крафтов. */
    private int craftMax(CarpenterRecipe recipe) {
        ItemStack result = recipe.result();
        int limit = Math.max(1, result.getMaxStackSize() / Math.max(1, result.getCount()));
        int crafted = 0;
        while (crafted < limit && craft(recipe)) {
            crafted++;
        }
        return crafted;
    }

    /**
     * Кнопки из GUI (выполняются на сервере):
     * BUTTON_CRAFT = скрафтить выбранный рецепт, BUTTON_CRAFT_MAX = скрафтить на сколько хватит (до стака),
     * 1+i = выбрать рецепт i.
     */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_CRAFT || id == BUTTON_CRAFT_MAX) {
            CarpenterRecipe recipe = getSelectedRecipe();
            if (recipe == null || !canCraft(recipe)) return false;

            int crafted = (id == BUTTON_CRAFT_MAX) ? craftMax(recipe) : (craft(recipe) ? 1 : 0);

            // Опыт Лесоруба: один вызов на весь клик, чтобы Shift-крафт не слал десятки пакетов и сохранений.
            // grantXp сам проверяет, что у игрока сейчас класс Лесоруб.
            if (crafted > 0 && player instanceof ServerPlayer serverPlayer) {
                ProfessionFeedback.grantXp(serverPlayer, ProfessionType.LUMBERJACK, XP_PER_CRAFT * crafted);
            }
            return crafted > 0;
        }
        int index = id - 1;
        if (index >= 0 && index < CarpenterRecipe.Registry.ALL.size()) {
            selectedRecipe.set(index + 1);
            return true;
        }
        return false;
    }

    // ---------- шифт-клик ----------

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            int containerEnd = CarpenterWorkbenchEntity.TOTAL_SIZE;

            if (index < containerEnd) {
                // из верстака -> в инвентарь игрока
                if (!moveItemStackTo(stack, containerEnd, slots.size(), true)) return ItemStack.EMPTY;
            } else {
                // из инвентаря -> во входные слоты
                if (!moveItemStackTo(stack, IN, IN_END, false)) return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
            else slot.setChanged();

            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}