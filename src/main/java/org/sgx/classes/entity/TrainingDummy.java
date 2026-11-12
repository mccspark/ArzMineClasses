package org.sgx.classes.entity;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import org.sgx.classes.event.TrainingDummyEvents;

import java.util.List;

/**
 * Манекен для тренировок. Это стойка для брони: проходима (как ванильная), но удары не пропускает;
 * броню надевают/снимают как на обычной стойке. Сама стойка невидима — вид «пугала» даёт item_display,
 * который едет на ней пассажиром (создаётся в TrainingDummyEvents#spawn).
 *
 * Режим цели хранится в тегах: dummy_undead (гнилая плоть) или dummy_arthropod (паучий глаз).
 */
public class TrainingDummy extends ArmorStand {

    public static final String TAG_UNDEAD = "dummy_undead";
    public static final String TAG_ARTHROPOD = "dummy_arthropod";

    public TrainingDummy(EntityType<? extends ArmorStand> type, Level level) {
        super(type, level);
    }

    public boolean isUndeadMode() {
        return entityTags().contains(TAG_UNDEAD);
    }

    public boolean isArthropodMode() {
        return entityTags().contains(TAG_ARTHROPOD);
    }

    /** Удары (и стрелы/трезубцы) только считаем: манекен не ломается, оружие не изнашивается. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isRemoved()) {
            return false;
        }
        // /kill, пустота и т.п. — как у обычной стойки
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurtServer(level, source, amount);
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            return false;
        }
        boolean melee = source.getDirectEntity() == player;
        boolean ranged = source.getDirectEntity() instanceof Projectile;
        if (!melee && !ranged) {
            return false;
        }
        TrainingDummyEvents.onDummyHit(level, this, player, amount, melee);
        return false;
    }

    /** Изнашивает надетую броню по ванильному правилу: damage / 4 (минимум 1) на каждую деталь. */
    public void damageArmor(float damage) {
        int wear = Math.max(1, (int) (damage / 4.0f));
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = getItemBySlot(slot);
            if (!stack.isEmpty() && stack.isDamageableItem()) {
                stack.hurtAndBreak(wear, this, slot);
            }
        }
    }

    /** Вызывается из UseEntityCallback (см. TrainingDummyEvents#init). PASS — пусть работает ванильная логика стойки. */
    public InteractionResult handleUse(ServerPlayer player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean mode = held.is(Items.ROTTEN_FLESH) || held.is(Items.SPIDER_EYE);
        boolean pickup = player.isShiftKeyDown() && held.isEmpty();
        if (!mode && !pickup) {
            return InteractionResult.PASS;
        }
        if (!(level() instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        if (pickup) {
            // забрать манекен вместе с надетой броней
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                ItemStack stack = getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    spawnAtLocation(serverLevel, stack);
                    setItemSlot(slot, ItemStack.EMPTY);
                }
            }
            if (!player.getAbilities().instabuild) {
                spawnAtLocation(serverLevel, new ItemStack(ModEntities.TRAINING_DUMMY_ITEM));
            }
            discard();
            return InteractionResult.SUCCESS;
        }
        boolean undead = held.is(Items.ROTTEN_FLESH);
        removeTag(TAG_UNDEAD);
        removeTag(TAG_ARTHROPOD);
        addTag(undead ? TAG_UNDEAD : TAG_ARTHROPOD);
        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }
        TrainingDummyEvents.onModeChanged(serverLevel, this, player, undead);
        return InteractionResult.SUCCESS;
    }

    /** Вместе с манекеном исчезает его «пугало»-пассажир (но не при выгрузке чанка). */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (reason.shouldDestroy()) {
            for (Entity passenger : List.copyOf(getPassengers())) {
                passenger.discard();
            }
        }
        super.remove(reason);
    }
}
