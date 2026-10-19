package org.sgx.classes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.sgx.classes.item.FurnaceFirewood;
import org.sgx.classes.item.ModItems;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Слот firewood в ОБЫЧНОЙ печи:
 *  - контейнер печи 3 -> 4 слота (сохранение, выпадение при поломке — автоматически);
 *  - когда печь реально плавит и текущее бревно закончилось, берётся новое бревно из слота
 *    и даёт SMELTS_PER_LOG переплавок (это значение "charges", его показывает полоска в GUI);
 *  - пока charges > 0, плавка идёт в 1,5 раза быстрее (+1 тик прогресса каждый второй тик).
 * "charges" хранится в печи и не сохраняется при перезагрузке мира (потеряется остаток бревна).
 */
@Mixin(AbstractFurnaceBlockEntity.class)
public abstract class FurnaceFirewoodBlockEntityMixin implements FurnaceFirewood.State {

    @Shadow
    @Final
    protected ContainerData dataAccess;

    @Unique
    private int arzClasses$charges;
    @Unique
    private int arzClasses$resultBefore;

    // ---- размер контейнера ----
    @ModifyArg(method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/core/NonNullList;withSize(ILjava/lang/Object;)Lnet/minecraft/core/NonNullList;"),
            index = 0)
    private int arzClasses$fourSlots(int size) {
        return size == 3 ? FurnaceFirewood.SLOT + 1 : size;
    }

    // ---- в слот 3 можно класть только firewood (и только в обычной печи) ----
    @Inject(method = "canPlaceItem", at = @At("HEAD"), cancellable = true)
    private void arzClasses$firewoodOnly(int slot, ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (slot == FurnaceFirewood.SLOT) {
            cir.setReturnValue(((Object) this instanceof FurnaceBlockEntity) && stack.is(ModItems.FIREWOOD));
        }
    }

    // ---- взять бревно и ускорить плавку x1.5 ----
    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void arzClasses$speedUp(ServerLevel level, BlockPos pos, BlockState state,
                                           AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        if (!(furnace instanceof FurnaceBlockEntity)) {
            return;
        }
        FurnaceFirewood.State st = (FurnaceFirewood.State) furnace;
        st.arzClasses$setResultBefore(furnace.getItem(2).getCount());

        ContainerData data = st.arzClasses$data();
        int lit = data.get(0);       // осталось горения
        int progress = data.get(2);  // прогресс плавки
        int total = data.get(3);     // всего тиков на предмет
        boolean cooking = lit > 0 && progress > 0;

        // Печь плавит, а бревно закончилось -> берём новое из слота.
        if (st.arzClasses$getCharges() <= 0 && cooking && !furnace.getItem(FurnaceFirewood.SLOT).isEmpty()) {
            furnace.removeItem(FurnaceFirewood.SLOT, 1);
            st.arzClasses$setCharges(FurnaceFirewood.SMELTS_PER_LOG);
            furnace.setChanged();
        }

        // Дополнительный +1 каждый второй тик = 1,5x. Условие progress <= total - 2 нужно, чтобы
        // не перепрыгнуть проверку "progress == total" в ванильном коде (иначе плавка зависнет).
        if (st.arzClasses$getCharges() > 0 && cooking && progress <= total - 2
                && (level.getGameTime() & 1L) == 0L) {
            data.set(2, progress + 1);
        }
    }

    // ---- расход использования после каждой переплавленной вещи ----
    @Inject(method = "serverTick", at = @At("RETURN"))
    private static void arzClasses$consume(ServerLevel level, BlockPos pos, BlockState state,
                                           AbstractFurnaceBlockEntity furnace, CallbackInfo ci) {
        if (!(furnace instanceof FurnaceBlockEntity)) {
            return;
        }
        FurnaceFirewood.State st = (FurnaceFirewood.State) furnace;
        if (furnace.getItem(2).getCount() <= st.arzClasses$getResultBefore()) {
            return; // в этот тик ничего не переплавилось
        }
        int charges = st.arzClasses$getCharges();
        if (charges > 0) {
            st.arzClasses$setCharges(charges - 1);
            furnace.setChanged();
        }
    }

    // ---- реализация интерфейса состояния ----
    @Override
    public int arzClasses$getCharges() {
        return arzClasses$charges;
    }

    @Override
    public void arzClasses$setCharges(int charges) {
        this.arzClasses$charges = charges;
    }

    @Override
    public int arzClasses$getResultBefore() {
        return arzClasses$resultBefore;
    }

    @Override
    public void arzClasses$setResultBefore(int count) {
        this.arzClasses$resultBefore = count;
    }

    @Override
    public ContainerData arzClasses$data() {
        return this.dataAccess;
    }
}