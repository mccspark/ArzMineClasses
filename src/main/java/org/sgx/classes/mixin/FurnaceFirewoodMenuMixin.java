package org.sgx.classes.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.inventory.SimpleContainerData;
import org.sgx.classes.item.FirewoodChargesData;
import org.sgx.classes.item.FirewoodSlot;
import org.sgx.classes.item.FurnaceFirewood;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Добавляет слот firewood и синхронизируемый счётчик использований в меню обычной печи. */
@Mixin(FurnaceMenu.class)
public abstract class FurnaceFirewoodMenuMixin implements FurnaceFirewood.MenuAccess {

    @Unique
    private ContainerData arzClasses$firewoodData = new SimpleContainerData(1);

    /** Клиент: своего контейнера печи нет, содержимое слота и счётчик приходят синхронизацией меню. */
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V", at = @At("TAIL"))
    private void arzClasses$clientSlot(int containerId, Inventory inventory, CallbackInfo ci) {
        arzClasses$setup(new SimpleContainer(1), 0, new SimpleContainerData(1));
    }

    /** Сервер: слот привязан к 4-му слоту контейнера самой печи, счётчик читается из печи. */
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/Container;Lnet/minecraft/world/inventory/ContainerData;)V",
            at = @At("TAIL"))
    private void arzClasses$serverSlot(int containerId, Inventory inventory, Container container,
                                       ContainerData data, CallbackInfo ci) {
        if (container.getContainerSize() > FurnaceFirewood.SLOT) {
            ContainerData charges = container instanceof FurnaceFirewood.State state
                    ? new FirewoodChargesData(state) : new SimpleContainerData(1);
            arzClasses$setup(container, FurnaceFirewood.SLOT, charges);
        } else {
            arzClasses$setup(new SimpleContainer(1), 0, new SimpleContainerData(1));
        }
    }

    @Unique
    private void arzClasses$setup(Container container, int index, ContainerData charges) {
        AbstractContainerMenuInvoker invoker = (AbstractContainerMenuInvoker) (Object) this;
        invoker.arzClasses$addSlot(new FirewoodSlot(container, index));
        invoker.arzClasses$addDataSlots(charges);
        this.arzClasses$firewoodData = charges;
    }

    @Override
    public ContainerData arzClasses$firewoodData() {
        return arzClasses$firewoodData;
    }
}