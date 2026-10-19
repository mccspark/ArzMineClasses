package org.sgx.classes.event;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.integration.LuckPermsIntegration;
import org.sgx.classes.item.ModItems;
import org.sgx.classes.network.ProfessionNetworking;
import org.sgx.classes.profession.ExperienceRingCheck;
import org.sgx.classes.profession.ProfessionBehavior;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionFeedback;
import org.sgx.classes.profession.ProfessionType;
import org.sgx.classes.profession.impl.*;
import org.sgx.classes.profession.skill.SkillAttributeSync;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ProfessionEvents {

    private static final Map<ProfessionType, ProfessionBehavior> BEHAVIORS = new EnumMap<>(ProfessionType.class);

    private static final Identifier TOMATO_VINE_ID =
            Identifier.fromNamespaceAndPath("farmersdelight", "tomatoes");
    private static final Identifier CUTTING_BOARD_ID =
            Identifier.fromNamespaceAndPath("farmersdelight", "cutting_board");
    private static final Pattern RICE_PANICLE_COUNT_PATTERN =
            Pattern.compile("count:(\\d+),id:\"farmersdelight:rice_panicle\"");

    static {
        register(new FarmerBehavior());
        register(new MinerBehavior());
        register(new BlacksmithBehavior());
        register(new LumberjackBehavior());
        register(new MysticBehavior());
    }

    private ProfessionEvents() {
    }

    private static void register(ProfessionBehavior behavior) {
        BEHAVIORS.put(behavior.type(), behavior);
    }

    private static int withRingBonus(ServerPlayer player, int baseXp) {
        return (int) Math.round(baseXp * ExperienceRingCheck.xpMultiplier(player));
    }

    public static void onEnchant(ServerPlayer player, int enchantPower) {
        ProfessionType active = ModAttachments.get(player).currentProfession();
        ProfessionBehavior behavior = BEHAVIORS.get(active);
        if (behavior != null) {
            int xp = behavior.onItemEnchanted(player, (ServerLevel) player.level(), enchantPower);
            if (xp > 0) {
                ProfessionFeedback.grantXp(player, active, withRingBonus(player, xp));
            }
        }
    }

    public static void onPotionTaken(ServerPlayer player, int potionTier) {
        ProfessionType active = ModAttachments.get(player).currentProfession();
        ProfessionBehavior behavior = BEHAVIORS.get(active);
        if (behavior != null) {
            int xp = behavior.onPotionTaken(player, (ServerLevel) player.level(), potionTier);
            if (xp > 0) {
                ProfessionFeedback.grantXp(player, active, withRingBonus(player, xp));
            }
        }
    }

    public static void init() {
        ProfessionBlockRestrictions.init();

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world.isClientSide() || !(world instanceof ServerLevel serverLevel)) {
                return;
            }
            boolean isLog = state.getBlock().builtInRegistryHolder().is(BlockTags.LOGS);
            try {
                if (player instanceof ServerPlayer serverPlayer) {
                    ProfessionType active = ModAttachments.get(serverPlayer).currentProfession();
                    ProfessionBehavior behavior = BEHAVIORS.get(active);
                    if (behavior != null) {
                        int xp = behavior.onBlockBroken(serverPlayer, serverLevel, pos, state);
                        if (xp > 0) {
                            ProfessionFeedback.grantXp(serverPlayer, active, withRingBonus(serverPlayer, xp));
                        }
                    }
                }
            } finally {
                // отметку снимаем ПОСЛЕ проверки профессией
                if (isLog) {
                    PlacedLogTracker.get(serverLevel).unmark(pos);
                }
            }
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() || hand != InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }
            if (!(player instanceof ServerPlayer serverPlayer)) {
                return InteractionResult.PASS;
            }

            BlockPos pos = hitResult.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Block block = state.getBlock();

            // Помидорная лоза: сбор ПКМ. Опыт + шанс золотого помидора.
            // Спелость проверяется по CropBlock ИЛИ по свойству "age" (лоза может не быть CropBlock).
            if (BuiltInRegistries.BLOCK.getKey(block).equals(TOMATO_VINE_ID) && isMaxAge(state)) {
                // Клик с предметом в руке при зажатом шифте ванильно НЕ открывает блок — не засчитываем,
                // иначе можно было бы фармить опыт кликами без сбора.
                boolean blockUseSkipped = serverPlayer.isShiftKeyDown()
                        && (!serverPlayer.getMainHandItem().isEmpty() || !serverPlayer.getOffhandItem().isEmpty());
                ProfessionData data = ModAttachments.get(serverPlayer);
                if (!blockUseSkipped && data.currentProfession() == ProfessionType.FARMER) {
                    ProfessionFeedback.grantXp(serverPlayer, ProfessionType.FARMER,
                            withRingBonus(serverPlayer, FarmerBehavior.XP_PER_HARVEST));

                    double rareChance = data.customScalarTotal("farmer.rare_crop_drop_chance_percent");
                    if (rareChance > 0) {
                        float chance = (float) Math.min(0.5, rareChance / 100.0);
                        if (world.getRandom().nextFloat() < chance) {
                            ItemEntity goldenTomato = new ItemEntity((ServerLevel) world,
                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                    new ItemStack(ModItems.GOLDEN_TOMATO));
                            world.addFreshEntity(goldenTomato);
                        }
                    }
                }
                return InteractionResult.PASS;
            }

            if (BuiltInRegistries.BLOCK.getKey(block).equals(CUTTING_BOARD_ID)) {
                ServerLevel serverLevel = (ServerLevel) world;
                int countBefore = getRicePanicleCount(serverLevel, pos);
                ProfessionData data = ModAttachments.get(serverPlayer);
                if (data.currentProfession() == ProfessionType.FARMER && countBefore > 0) {
                    double rareChance = data.customScalarTotal("farmer.rare_crop_drop_chance_percent");
                    if (rareChance > 0) {
                        // капаем на 2% (не на общие 50%, как у остальных культур) — сейчас
                        // достижимо только 1% (главная цепочка), ветка подкласса позже
                        // добавит ещё +1% к тому же customScalarTotal, доведя до потолка
                        float chance = (float) Math.min(0.02, rareChance / 100.0);
                        pollForRiceCut(serverLevel, pos, countBefore, 0, chance);
                    }
                }
            }

            return InteractionResult.PASS;
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            ProfessionData data = ModAttachments.get(player);
            SkillAttributeSync.reapply(player, data);
            LuckPermsIntegration.syncGroup(player, data);
            ProfessionNetworking.syncToClient(player);
        });
    }

    /** Спелость без привязки к CropBlock: если блок не CropBlock, смотрим целочисленное свойство "age". */
    private static boolean isMaxAge(BlockState state) {
        if (state.getBlock() instanceof CropBlock crop) {
            return crop.isMaxAge(state);
        }
        for (Property<?> property : state.getProperties()) {
            if (property instanceof IntegerProperty ip && ip.getName().equals("age")) {
                int max = ip.getPossibleValues().stream().max(Integer::compare).orElse(-1);
                return state.getValue(ip) == max;
            }
        }
        return false;
    }

    /** Опрашивает count на доске несколько тиков подряд (до 20 = ~1 сек) — разрез
     * на разделочной доске Farmer's Delight происходит не мгновенно в тот же тик. */
    private static void pollForRiceCut(ServerLevel world, BlockPos pos, int lastCount, int ticksWaited, float chance) {
        world.getServer().execute(() -> {
            int current = getRicePanicleCount(world, pos);
            if (current < lastCount) {
                int cuts = lastCount - current;
                for (int i = 0; i < cuts; i++) {
                    if (world.getRandom().nextFloat() < chance) {
                        ItemEntity goldenRice = new ItemEntity(world,
                                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                                new ItemStack(ModItems.GOLDEN_RICE));
                        world.addFreshEntity(goldenRice);
                    }
                }
                return;
            }
            if (ticksWaited < 20) {
                pollForRiceCut(world, pos, current, ticksWaited + 1, chance);
            }
        });
    }

    private static int getRicePanicleCount(ServerLevel world, BlockPos pos) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be == null) {
            return 0;
        }
        String nbt = be.saveWithoutMetadata(world.registryAccess()).toString();
        Matcher m = RICE_PANICLE_COUNT_PATTERN.matcher(nbt);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
}