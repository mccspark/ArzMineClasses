package org.sgx.classes.event;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.item.ModItems;
import org.sgx.classes.network.OpenSharpeningPayload;
import org.sgx.classes.network.SharpeningResultPayload;
import org.sgx.classes.profession.ArmorSharpening;
import org.sgx.classes.profession.ItemRepair;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;
import org.sgx.classes.profession.RepairMaterials;
import org.sgx.classes.profession.SharpeningCaps;
import org.sgx.classes.profession.SubclassType;
import org.sgx.classes.profession.WeaponSharpening;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class SharpeningTrigger {

    private enum Mode { SHARPEN, ARMOR_SHARPEN, REPAIR }

    private record Session(Mode mode, Item repairMaterial) {
    }

    private static final Map<UUID, Session> ACTIVE_SESSIONS = new ConcurrentHashMap<>();
    private static final int HAMMER_DAMAGE_PER_USE = 1;

    /** Пауза между повторными сообщениями-отказами (в тиках), чтобы не спамить при зажатой ПКМ. */
    private static final int NOTICE_COOLDOWN_TICKS = 40;
    private static final Map<UUID, Integer> LAST_NOTICE_TICK = new ConcurrentHashMap<>();

    private SharpeningTrigger() {
    }

    /** Сообщение-отказ в action bar (над хотбаром), не чаще раза в NOTICE_COOLDOWN_TICKS. */
    private static void notice(ServerPlayer player, Component message) {
        int now = player.tickCount;
        Integer last = LAST_NOTICE_TICK.get(player.getUUID());
        if (last != null && now - last < NOTICE_COOLDOWN_TICKS && now >= last) {
            return;
        }
        LAST_NOTICE_TICK.put(player.getUUID(), now);
        player.sendSystemMessage(message, true);
    }

    public static void init() {
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || world.isClientSide()) {
                return InteractionResult.PASS;
            }
            if (!player.isShiftKeyDown() || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(
                    world.getBlockState(hitResult.getBlockPos()).getBlock());
            if (!blockId.getPath().equals("smithing_table")) {
                return InteractionResult.PASS;
            }
            ProfessionData data = ModAttachments.get(serverPlayer);
            if (data.currentProfession() != ProfessionType.BLACKSMITH) {
                return InteractionResult.PASS;
            }

            ItemStack mainHand = player.getMainHandItem();
            ItemStack offHand = player.getOffhandItem();
            if (mainHand.isEmpty()) {
                return InteractionResult.PASS;
            }

            // деревянные и каменные инструменты/оружие не затачиваются и не чинятся
            if (isEquipment(mainHand) && isTier(mainHand, "wooden_", "wood_", "stone_")) {
                notice(serverPlayer, Component.translatable("message.arz_classes.tool_tier_too_low"));
                return InteractionResult.SUCCESS;
            }

            boolean isHammer = offHand.is(ModItems.BLACKSMITH_HAMMER) || offHand.is(ModItems.COPPER_HAMMER);
            SubclassType subclass = data.currentSubclass();
            int level = data.level();

            Mode mode;
            Item repairMaterial = null;

            if (isArmor(mainHand)) {
                boolean canSharpen = subclass == SubclassType.ARMORER && isHammer;

                if (canSharpen) {
                    // на последней прочности затачивать нельзя — только чинить
                    if (ItemRepair.isLastDurability(mainHand)) {
                        notice(serverPlayer, Component.translatable("message.arz_classes.sharpen_too_damaged"));
                        return InteractionResult.SUCCESS;
                    }
                    int cap = SharpeningCaps.armorCap(data);
                    if (cap <= 0) {
                        serverPlayer.sendSystemMessage(Component.translatable("message.arz_classes.sharpen_not_learned"));
                        return InteractionResult.SUCCESS;
                    }
                    if (ArmorSharpening.currentLevel(mainHand) >= cap) {
                        serverPlayer.sendSystemMessage(Component.translatable("message.arz_classes.already_max_sharpened"));
                        return InteractionResult.SUCCESS;
                    }
                    mode = Mode.ARMOR_SHARPEN;
                } else if (isHammer) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.arz_classes.armor_not_sharpenable"));
                    return InteractionResult.SUCCESS;
                } else if (mainHand.getDamageValue() <= 0) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.arz_classes.already_full_durability"));
                    return InteractionResult.SUCCESS;
                } else if (isValidRepairMaterial(mainHand, offHand)) {
                    if (!data.hasAbility("blacksmith_unlimited_repair") && isBeyondIronTier(mainHand)) {
                        notice(serverPlayer, Component.translatable("message.arz_classes.repair_locked_tier"));
                        return InteractionResult.SUCCESS;
                    }
                    mode = Mode.REPAIR;
                    repairMaterial = offHand.getItem();
                } else {
                    serverPlayer.sendSystemMessage(Component.translatable("message.arz_classes.repair_wrong_material"));
                    return InteractionResult.SUCCESS;
                }
            } else if (!isEquipment(mainHand)) {
                return InteractionResult.PASS; // не оружие/инструмент/броня — точило игнорирует
            } else if (isHammer && (mainHand.is(net.minecraft.world.item.Items.BOW)
                    || mainHand.is(net.minecraft.world.item.Items.CROSSBOW))) {
                // лук и арбалет не затачиваются (их только чинят палками), без сообщения
                return InteractionResult.PASS;
            } else if (isHammer) {
                // на последней прочности затачивать нельзя — только чинить
                if (ItemRepair.isLastDurability(mainHand)) {
                    notice(serverPlayer, Component.translatable("message.arz_classes.sharpen_too_damaged"));
                    return InteractionResult.SUCCESS;
                }
                int cap = SharpeningCaps.weaponOrToolCap(mainHand, data);
                if (cap <= 0) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.arz_classes.sharpen_not_learned"));
                    return InteractionResult.SUCCESS;
                }
                if (WeaponSharpening.currentLevel(mainHand) >= cap) {
                    serverPlayer.sendSystemMessage(Component.translatable("message.arz_classes.already_max_sharpened"));
                    return InteractionResult.SUCCESS;
                }
                mode = Mode.SHARPEN;
            } else if (isValidRepairMaterial(mainHand, offHand) && mainHand.getDamageValue() > 0) {
                if (!data.hasAbility("blacksmith_unlimited_repair") && isBeyondIronTier(mainHand)) {
                    notice(serverPlayer, Component.translatable("message.arz_classes.repair_locked_tier"));
                    return InteractionResult.SUCCESS;
                }
                mode = Mode.REPAIR;
                repairMaterial = offHand.getItem();
            } else {
                return InteractionResult.PASS;
            }

            ThreadLocalRandom rnd = ThreadLocalRandom.current();

            float materialFactor = materialFactor(mainHand);
            float levelEase = Mth.clamp((float) level / ProfessionType.MAX_LEVEL, 0f, 1f);

            float sharpenBonusPercent = 0f;
            if (mode == Mode.SHARPEN) {
                sharpenBonusPercent = (float) (data.customScalarTotal("blacksmith.sharpen_efficiency_percent")
                        + data.customScalarTotal("weaponsmith.sharpen_efficiency_percent"));
            } else if (mode == Mode.ARMOR_SHARPEN) {
                sharpenBonusPercent = (float) (data.customScalarTotal("blacksmith.sharpen_efficiency_percent")
                        + data.customScalarTotal("armorer.sharpen_efficiency_percent"));
            }
            float sharpenMultiplier = 1f + sharpenBonusPercent / 100f;

            float brightBase = (Mth.lerp(materialFactor, 0.045f, 0.025f) + levelEase * 0.02f) * sharpenMultiplier;
            float greenExtra = (Mth.lerp(materialFactor, 0.10f, 0.16f) + levelEase * 0.05f) * sharpenMultiplier;
            float yellowExtra = (Mth.lerp(materialFactor, 0.14f, 0.24f) + levelEase * 0.08f) * sharpenMultiplier;

            float brightWidth = brightBase + rnd.nextFloat() * 0.02f;
            float greenWidth = brightWidth + greenExtra + rnd.nextFloat() * 0.04f;
            float yellowWidth = Math.min(0.9f, greenWidth + yellowExtra + rnd.nextFloat() * 0.06f);

            float half = yellowWidth / 2f;
            float center = half + rnd.nextFloat() * (1f - yellowWidth);

            float baseSpeed = Mth.lerp(materialFactor, 1.2f, 2.1f);
            float barSpeed = Math.max(0.7f, baseSpeed - levelEase * 0.5f);

            ACTIVE_SESSIONS.put(serverPlayer.getUUID(), new Session(mode, repairMaterial));
            ServerPlayNetworking.send(serverPlayer,
                    new OpenSharpeningPayload(center, brightWidth, greenWidth, yellowWidth, barSpeed, mode == Mode.REPAIR));
            return InteractionResult.SUCCESS;
        });

        ServerPlayNetworking.registerGlobalReceiver(SharpeningResultPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    Session session = ACTIVE_SESSIONS.remove(player.getUUID());
                    if (session == null) {
                        return;
                    }
                    if (ModAttachments.get(player).currentProfession() != ProfessionType.BLACKSMITH) {
                        return;
                    }
                    switch (session.mode()) {
                        case SHARPEN -> {
                            WeaponSharpening.applyResult(player, payload.zone());
                            damageHammer(player);
                        }
                        case ARMOR_SHARPEN -> {
                            ArmorSharpening.applyResult(player, payload.zone());
                            damageHammer(player);
                        }
                        case REPAIR -> ItemRepair.applyResult(player, payload.zone(), session.repairMaterial());
                    }
                }));
    }

    private static void damageHammer(ServerPlayer player) {
        ItemStack hammer = player.getOffhandItem();
        if (!hammer.is(ModItems.BLACKSMITH_HAMMER) && !hammer.is(ModItems.COPPER_HAMMER)) {
            return;
        }
        hammer.hurtAndBreak(HAMMER_DAMAGE_PER_USE, (ServerLevel) player.level(), player, item ->
                player.sendSystemMessage(Component.translatable("message.arz_classes.hammer_broke")));
    }

    private static boolean isValidRepairMaterial(ItemStack toRepair, ItemStack material) {
        return RepairMaterials.isValid(toRepair, material);
    }

    private static boolean isTier(ItemStack stack, String... prefixes) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        for (String prefix : prefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBeyondIronTier(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return path.startsWith("diamond_") || path.startsWith("netherite_");
    }

    private static float materialFactor(ItemStack stack) {
        String path = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        if (path.startsWith("netherite_")) return 1.0f;
        if (path.startsWith("diamond_")) return 0.65f;
        if (path.startsWith("iron_")) return 0.35f;
        if (path.startsWith("copper_")) return 0.25f;
        if (path.startsWith("stone_")) return 0.15f;
        if (path.startsWith("golden_") || path.startsWith("gold_")) return 0.05f;
        if (path.startsWith("wooden_") || path.startsWith("wood_")) return 0.0f;
        return 0.3f;
    }

    private static boolean isArmor(ItemStack stack) {
        return stack.getItem().builtInRegistryHolder().is(net.minecraft.tags.ItemTags.TRIMMABLE_ARMOR)
                || stack.is(net.minecraft.tags.ItemTags.HEAD_ARMOR)
                || stack.is(net.minecraft.tags.ItemTags.CHEST_ARMOR)
                || stack.is(net.minecraft.tags.ItemTags.LEG_ARMOR)
                || stack.is(net.minecraft.tags.ItemTags.FOOT_ARMOR);
    }

    private static boolean isEquipment(ItemStack stack) {
        return stack.getMaxDamage() > 0
                && (stack.getItem().builtInRegistryHolder().is(net.minecraft.tags.ItemTags.SWORDS)
                || stack.getItem().builtInRegistryHolder().is(net.minecraft.tags.ItemTags.AXES)
                || stack.getItem().builtInRegistryHolder().is(net.minecraft.tags.ItemTags.PICKAXES)
                || stack.getItem().builtInRegistryHolder().is(net.minecraft.tags.ItemTags.SHOVELS)
                || stack.getItem().builtInRegistryHolder().is(net.minecraft.tags.ItemTags.HOES)
                || stack.is(net.minecraft.world.item.Items.TRIDENT)
                || WeaponSharpening.isSpear(stack)
                || WeaponSharpening.isKnife(stack)
                || stack.is(net.minecraft.world.item.Items.BOW)
                || stack.is(net.minecraft.world.item.Items.CROSSBOW));
    }
}