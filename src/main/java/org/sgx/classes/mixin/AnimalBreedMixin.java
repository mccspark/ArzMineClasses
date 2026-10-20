package org.sgx.classes.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.sgx.classes.item.HarvestBookItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Книга урожая: считает разведённых животных. finalizeSpawnChildFromBreeding вызывается один раз
 * на пару родителей; игрока, который их кормил, хранит любой из родителей (getLoveCause).
 */
@Mixin(Animal.class)
public abstract class AnimalBreedMixin {

    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("HEAD"))
    private void arzClasses$recordBred(ServerLevel level, Animal partner, AgeableMob baby, CallbackInfo ci) {
        Animal self = (Animal) (Object) this;
        ServerPlayer player = self.getLoveCause();
        if (player == null) {
            player = partner.getLoveCause();
        }
        if (player != null) {
            HarvestBookItem.recordBred(player, self.getType());
        }
    }
}
