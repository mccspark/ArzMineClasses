package org.sgx.classes.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.item.ModDataComponents;
import org.sgx.classes.item.ModItems;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;
import org.sgx.classes.profession.SubclassType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Slot.class)
public abstract class CraftingResultSlotMixin {

    /** Молоты кузнеца: крафтит только Кузнец. */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void arzClasses$blockNonBlacksmithHammer(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (!(self instanceof ResultSlot)) {
            return;
        }
        ItemStack result = self.getItem();
        if (result.isEmpty() || !(result.is(ModItems.BLACKSMITH_HAMMER) || result.is(ModItems.COPPER_HAMMER))) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (ModAttachments.get(serverPlayer).currentProfession() != ProfessionType.BLACKSMITH) {
            cir.setReturnValue(false);
        }
    }

    /** Костяной мешочек: крафтит только тот, у кого открыт навык "farmer_bone_pouch". */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void arzClasses$blockUnqualifiedBonePouch(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (!(self instanceof ResultSlot)) {
            return;
        }
        ItemStack result = self.getItem();
        if (result.isEmpty() || !result.is(ModItems.BONE_POUCH)) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!ModAttachments.get(serverPlayer).hasAbility("farmer_bone_pouch")) {
            cir.setReturnValue(false);
        }
    }

    /** Мешочек для еды: крафтит только тот, у кого открыт навык "farmer_food_pouch" (7 уровень Фермера). */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void arzClasses$blockUnqualifiedFoodPouch(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (!(self instanceof ResultSlot)) {
            return;
        }
        ItemStack result = self.getItem();
        if (result.isEmpty() || !result.is(ModItems.FOOD_POUCH)) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!ModAttachments.get(serverPlayer).hasAbility("farmer_food_pouch")) {
            cir.setReturnValue(false);
        }
    }

    /** Книга урожая: крафтит только Фермер. */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void arzClasses$blockNonFarmerHarvestBook(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (!(self instanceof ResultSlot)) {
            return;
        }
        ItemStack result = self.getItem();
        if (result.isEmpty() || !result.is(ModItems.HARVEST_BOOK)) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (ModAttachments.get(serverPlayer).currentProfession() != ProfessionType.FARMER) {
            cir.setReturnValue(false);
        }
    }

    /** Звёзды качества на еде, приготовленной Поваром. */
    @Inject(method = "mayPickup", at = @At("HEAD"))
    private void arzClasses$stampMealStars(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (!(self instanceof ResultSlot) && !(self instanceof net.minecraft.world.inventory.FurnaceResultSlot)) {
            return;
        }
        ItemStack result = self.getItem();
        if (result.isEmpty() || !result.has(net.minecraft.core.component.DataComponents.FOOD)) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ProfessionData data = ModAttachments.get(serverPlayer);
        if (data.currentSubclass() != SubclassType.COOK) {
            return;
        }
        if (result.has(ModDataComponents.MEAL_STARS)) {
            return;
        }
        int stars = Math.min(5, (int) data.customScalarTotal("cook.quality_star_count"));
        if (stars <= 0) {
            return;
        }
        result.set(ModDataComponents.MEAL_STARS, stars);
    }

    /** Кольца Мистика: железное и золотое нужны с навыком, незеритовое — просто Мистик. */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void arzClasses$blockUnqualifiedRings(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (!(self instanceof ResultSlot)) {
            return;
        }
        ItemStack result = self.getItem();
        boolean iron = result.is(ModItems.RING_IRON);
        boolean gold = result.is(ModItems.RING_GOLD);
        boolean netherite = result.is(ModItems.RING_NETHERITE);
        if (!iron && !gold && !netherite) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ProfessionData data = ModAttachments.get(serverPlayer);
        boolean allowed = data.currentProfession() == ProfessionType.MYSTIC
                && (iron ? data.hasAbility("mystic_iron_ring_craft")
                : gold ? data.hasAbility("mystic_gold_ring_craft")
                : true);
        if (!allowed) {
            cir.setReturnValue(false);
        }
    }

    /** Дрова: крафтит только Лесоруб с открытым навыком "lumberjack_coal_chance" (3 уровень). */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void arzClasses$blockUnqualifiedFirewood(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (!(self instanceof ResultSlot)) {
            return;
        }
        ItemStack result = self.getItem();
        if (result.isEmpty() || !result.is(ModItems.FIREWOOD)) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ProfessionData data = ModAttachments.get(serverPlayer);
        if (data.currentProfession() != ProfessionType.LUMBERJACK || !data.hasAbility("lumberjack_coal_chance")) {
            cir.setReturnValue(false);
        }
    }

    /** Столярный верстак: крафтит только Лесоруб с открытым навыком "lumberjack_decor" (7 уровень). */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void arzClasses$blockUnqualifiedCarpenterWorkbench(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (!(self instanceof ResultSlot)) {
            return;
        }
        ItemStack result = self.getItem();
        if (result.isEmpty()) {
            return;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(result.getItem());
        if (!id.getNamespace().equals("arz_classes") || !id.getPath().equals("carpenter_workbench")) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ProfessionData data = ModAttachments.get(serverPlayer);
        if (data.currentProfession() != ProfessionType.LUMBERJACK || !data.hasAbility("lumberjack_decor")) {
            cir.setReturnValue(false);
        }
    }

    /** Каска шахтёра: крафтит только Шахтёр с открытым навыком "miner_helmet" (3 уровень). */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void arzClasses$blockUnqualifiedMinerHelmet(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (!(self instanceof ResultSlot)) {
            return;
        }
        ItemStack result = self.getItem();
        if (result.isEmpty() || !result.is(ModItems.MINER_HELMET)) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ProfessionData data = ModAttachments.get(serverPlayer);
        if (data.currentProfession() != ProfessionType.MINER || !data.hasAbility("miner_helmet")) {
            cir.setReturnValue(false);
        }
    }

    /** Стол зачарований (enchanting_table.json): крафтит только Мистик. */
    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void arzClasses$blockNonMysticEnchantingTable(Player player, CallbackInfoReturnable<Boolean> cir) {
        Slot self = (Slot) (Object) this;
        if (!(self instanceof ResultSlot)) {
            return;
        }
        ItemStack result = self.getItem();
        if (result.isEmpty() || !result.is(Items.ENCHANTING_TABLE)) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (ModAttachments.get(serverPlayer).currentProfession() != ProfessionType.MYSTIC) {
            cir.setReturnValue(false);
        }
    }
}