package org.sgx.classes.event;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ProfessionBlockRestrictions {

    // FIX: "smithing_table" отсутствовал — из-за этого столом кузнеца мог пользоваться
    // кто угодно, ограничение действовало только на наковальню/точило.
    private static final Set<String> BLACKSMITH_BLOCKS = Set.of("smithing_table");
    private static final Set<String> MYSTIC_BLOCKS = Set.of("enchanting_table");
    private static final Set<String> FARMER_BLOCKS = Set.of("cooking_pot"); // farmersdelight:cooking_pot
    private static final long MESSAGE_COOLDOWN_MS = 1500;

    private static final Map<ServerPlayer, Long> lastMessageTime = new HashMap<>();

    private ProfessionBlockRestrictions() {
    }

    public static void init() {
        Arz_classes.LOGGER.info("[arz_classes] ProfessionBlockRestrictions зарегистрирован");
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (!(player instanceof ServerPlayer serverPlayer) || world.isClientSide()) {
                return InteractionResult.PASS;
            }
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(
                    world.getBlockState(hitResult.getBlockPos()).getBlock());
            String path = blockId.getPath();

            ProfessionType required;
            if (BLACKSMITH_BLOCKS.contains(path)) {
                required = ProfessionType.BLACKSMITH;
            } else if (MYSTIC_BLOCKS.contains(path)) {
                required = ProfessionType.MYSTIC;
            } else if (FARMER_BLOCKS.contains(path)) {
                required = ProfessionType.FARMER;
            } else {
                return InteractionResult.PASS;
            }

            ProfessionType active = ModAttachments.get(serverPlayer).currentProfession();
            if (active != required) {
                long now = System.currentTimeMillis();
                long last = lastMessageTime.getOrDefault(serverPlayer, 0L);
                if (now - last > MESSAGE_COOLDOWN_MS) {
                    serverPlayer.sendSystemMessage(Component.translatable(
                            "message.arz_classes.wrong_profession_block", required.displayName()));
                    lastMessageTime.put(serverPlayer, now);
                }
                return InteractionResult.FAIL;
            }
            return InteractionResult.PASS;
        });
    }
}
