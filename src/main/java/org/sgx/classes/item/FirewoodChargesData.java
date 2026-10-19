package org.sgx.classes.item;

import net.minecraft.world.inventory.ContainerData;

/** Серверная обёртка: отдаёт меню оставшиеся использования текущего бревна для синхронизации с клиентом. */
public class FirewoodChargesData implements ContainerData {

    private final FurnaceFirewood.State state;

    public FirewoodChargesData(FurnaceFirewood.State state) {
        this.state = state;
    }

    @Override
    public int get(int index) {
        return state.arzClasses$getCharges();
    }

    @Override
    public void set(int index, int value) {
        state.arzClasses$setCharges(value);
    }

    @Override
    public int getCount() {
        return 1;
    }
}