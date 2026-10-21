package org.sgx.classes.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.Collections;

/**
 * Оружейная стойка: 3 слота, в каждом по одному предмету. Реализует Container, чтобы при поломке
 * блока содержимое выпадало так же, как у остальных блоков мода (верстак, стол редкости).
 */
public class WeaponRackBlockEntity extends BlockEntity implements Container {

    public static final int SLOTS = 3;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    public WeaponRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WEAPON_RACK, pos, state);
    }

    // ---------- логика стойки ----------

    /** Кладёт ОДИН предмет из stack: сначала в preferred, если он занят — в первый свободный. */
    public boolean tryInsert(int preferred, ItemStack stack) {
        if (stack.isEmpty()) return false;
        int target = -1;
        if (items.get(preferred).isEmpty()) {
            target = preferred;
        } else {
            for (int i = 0; i < SLOTS; i++) {
                if (items.get(i).isEmpty()) {
                    target = i;
                    break;
                }
            }
        }
        if (target < 0) return false;
        items.set(target, stack.copyWithCount(1));
        syncToClients();
        return true;
    }

    /** Забирает предмет из preferred, а если там пусто — из ближайшего занятого слота. */
    public ItemStack takeAt(int preferred) {
        int source = -1;
        if (!items.get(preferred).isEmpty()) {
            source = preferred;
        } else {
            for (int d = 1; d < SLOTS && source < 0; d++) {
                for (int i : new int[]{preferred - d, preferred + d}) {
                    if (i >= 0 && i < SLOTS && !items.get(i).isEmpty()) {
                        source = i;
                        break;
                    }
                }
            }
        }
        if (source < 0) return ItemStack.EMPTY;
        ItemStack taken = items.get(source);
        items.set(source, ItemStack.EMPTY);
        syncToClients();
        return taken;
    }

    public boolean isFull() {
        for (ItemStack s : items) {
            if (s.isEmpty()) return false;
        }
        return true;
    }

    /** Сохранить + отправить новое содержимое клиентам (для рендера висящих предметов). */
    private void syncToClients() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_ALL);
        }
    }

    // ---------- синхронизация с клиентом ----------

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    // ---------- Container ----------

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
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
        if (!result.isEmpty()) syncToClients();
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        syncToClients();
    }

    @Override
    public boolean stillValid(Player player) {
        return false; // GUI у стойки нет
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
}
