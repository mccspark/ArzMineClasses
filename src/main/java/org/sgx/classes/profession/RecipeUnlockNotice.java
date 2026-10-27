package org.sgx.classes.profession;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Сообщает игроку, что после прокачки навыка ему открылся новый рецепт.
 * Соответствие «навык -> рецепт» держим в синхроне с ModRecipeBook (client) — там те же условия unlocked().
 * Рецепты без условия (всегда открытые) здесь не нужны.
 */
public final class RecipeUnlockNotice {

    /** id навыка -> id рецепта (ключ перевода recipe.arz_classes.<id>). */
    private static final Map<String, String> ABILITY_TO_RECIPE = new LinkedHashMap<>();

    static {
        ABILITY_TO_RECIPE.put("farmer_bone_pouch", "bone_pouch");
        ABILITY_TO_RECIPE.put("farmer_food_pouch", "food_pouch");
        ABILITY_TO_RECIPE.put("miner_helmet", "miner_helmet");
        ABILITY_TO_RECIPE.put("lumberjack_coal_chance", "firewood");
        ABILITY_TO_RECIPE.put("lumberjack_decor", "carpenter_workbench");
        ABILITY_TO_RECIPE.put("mystic_iron_ring_craft", "ring_iron");
        ABILITY_TO_RECIPE.put("mystic_gold_ring_craft", "ring_gold");
    }

    private RecipeUnlockNotice() {
    }

    /** Снимок: какие из «рецептовых» навыков уже открыты. Вызывать ДО открытия навыка. */
    public static Map<String, Boolean> snapshot(ProfessionData data) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String ability : ABILITY_TO_RECIPE.keySet()) {
            result.put(ability, data.hasAbility(ability));
        }
        return result;
    }

    /** Сравнивает со снимком и пишет игроку о каждом новом рецепте. Вызывать ПОСЛЕ открытия навыка. */
    public static void notifyNew(ServerPlayer player, ProfessionData data, Map<String, Boolean> before) {
        boolean any = false;
        for (Map.Entry<String, String> e : ABILITY_TO_RECIPE.entrySet()) {
            if (before.getOrDefault(e.getKey(), false) || !data.hasAbility(e.getKey())) {
                continue;
            }
            any = true;
            player.sendSystemMessage(Component.translatable("message.arz_classes.recipe_unlocked",
                    Component.translatable("recipe.arz_classes." + e.getValue())
                            .withStyle(net.minecraft.ChatFormatting.GOLD))
                    .withStyle(net.minecraft.ChatFormatting.GREEN));
        }
        if (any) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
                    SoundSource.PLAYERS, 0.5f, 1.2f);
        }
    }
}
