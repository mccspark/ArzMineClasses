package org.sgx.classes.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Collections;

public class CarpenterWorkbenchEntity extends BlockEntity implements Container, MenuProvider {
    public static final int INPUT_START = 0;
    public static final int INPUT_SIZE = 12;   // 3 ряда x 4 столбца
    public static final int OUTPUT_START = 12;
    public static final int OUTPUT_SIZE = 6;   // 2 столбца x 3 слота
    public static final int TOTAL_SIZE = INPUT_SIZE + OUTPUT_SIZE;

    private final NonNullList<ItemStack> items = NonNullList.withSize(TOTAL_SIZE, ItemStack.EMPTY);

    public CarpenterWorkbenchEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CARPENTER_WORKBENCH, pos, state);
    }

    @Override
    public int getContainerSize() {
        return TOTAL_SIZE;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack result = ContainerHelper.removeItem(items, slot, count);
        if (!result.isEmpty()) setChanged();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clearContent() {
        Collections.fill(items, ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Collections.fill(items, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.arz_classes.carpenter_workbench");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new CarpenterWorkbenchMenu(containerId, inventory, this);
    }
}