package org.sgx.classes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.sgx.classes.item.BrewedPotions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Помечает зелья, сваренные в зельеварке. Положенное игроком (или воронкой) готовое зелье
 * пометки не получает, поэтому опыт за него не выдаётся. Пометка снимается при взятии зелья.
 * Пометка хранится в самой зельеварке и не сохраняется при перезагрузке мира.
 */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandBlockEntityMixin implements BrewedPotions.State {

    @Shadow
    private int brewTime;

    @Unique
    private final boolean[] arzClasses$brewed = new boolean[BrewedPotions.POTION_SLOTS];
    @Unique
    private int arzClasses$brewTimeBefore;
    @Unique
    private ItemStack arzClasses$ingredientBefore = ItemStack.EMPTY;

    @Inject(method = "serverTick", at = @At("HEAD"), require = 0)
    private static void arzClasses$before(Level level, BlockPos pos, BlockState state,
                                          BrewingStandBlockEntity stand, CallbackInfo ci) {
        ((BrewedPotions.State) stand).arzClasses$beforeTick();
    }

    @Inject(method = "serverTick", at = @At("RETURN"), require = 0)
    private static void arzClasses$after(Level level, BlockPos pos, BlockState state,
                                         BrewingStandBlockEntity stand, CallbackInfo ci) {
        ((BrewedPotions.State) stand).arzClasses$afterTick();
    }

    @Override
    public void arzClasses$beforeTick() {
        BrewingStandBlockEntity self = (BrewingStandBlockEntity) (Object) this;
        this.arzClasses$brewTimeBefore = this.brewTime;
        this.arzClasses$ingredientBefore = self.getItem(3).copy();
    }

    @Override
    public void arzClasses$afterTick() {
        BrewingStandBlockEntity self = (BrewingStandBlockEntity) (Object) this;
        ItemStack ingredientNow = self.getItem(3);
        // Варка реально завершилась: таймер дошёл до нуля И ингредиент был израсходован
        // (если зелья просто вынули/ингредиент убрали, таймер тоже сбрасывается, но ингредиент цел).
        boolean finished = this.arzClasses$brewTimeBefore == 1 && this.brewTime == 0
                && !this.arzClasses$ingredientBefore.isEmpty()
                && (ingredientNow.isEmpty()
                || ingredientNow.getCount() < this.arzClasses$ingredientBefore.getCount()
                || !ItemStack.isSameItem(ingredientNow, this.arzClasses$ingredientBefore));
        for (int i = 0; i < BrewedPotions.POTION_SLOTS; i++) {
            if (self.getItem(i).isEmpty()) {
                this.arzClasses$brewed[i] = false; // зелье убрали (например, воронкой) - пометка не нужна
            } else if (finished) {
                this.arzClasses$brewed[i] = true;
            }
        }
        this.arzClasses$brewTimeBefore = 0;
    }

    @Override
    public boolean arzClasses$isBrewed(int slot) {
        return slot >= 0 && slot < BrewedPotions.POTION_SLOTS && this.arzClasses$brewed[slot];
    }

    @Override
    public void arzClasses$setBrewed(int slot, boolean brewed) {
        if (slot >= 0 && slot < BrewedPotions.POTION_SLOTS) {
            this.arzClasses$brewed[slot] = brewed;
        }
    }
}
