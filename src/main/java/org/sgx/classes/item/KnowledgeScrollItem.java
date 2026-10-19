package org.sgx.classes.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.network.ProfessionNetworking;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;
import org.sgx.classes.profession.skill.SkillAttributeSync;

/**
 * Только творческий режим: даёт +1 уровень активному классу напрямую (без опыта) —
 * для быстрого теста ветвления подкласса на 25 уровне без долгого фарма.
 */
public final class KnowledgeScrollItem extends Item {

    public KnowledgeScrollItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !player.isCreative() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        ProfessionData data = ModAttachments.get(serverPlayer);
        if (!data.addLevel(1)) {
            return InteractionResult.FAIL; // класс не выбран или уже максимум
        }
        SkillAttributeSync.reapply(serverPlayer, data);
        ProfessionNetworking.syncToClient(serverPlayer);
        return InteractionResult.SUCCESS;
    }
}