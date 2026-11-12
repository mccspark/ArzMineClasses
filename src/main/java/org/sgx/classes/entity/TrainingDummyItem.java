package org.sgx.classes.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.sgx.classes.event.TrainingDummyEvents;

/** Предмет-манекен: ПКМ по блоку ставит энтити-манекен лицом к игроку. */
public class TrainingDummyItem extends Item {

    public TrainingDummyItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Vec3 at = Vec3.atBottomCenterOf(pos);
        float yaw = context.getRotation() + 180.0f; // стойка смотрит на игрока
        TrainingDummyEvents.spawn(serverLevel, at, yaw);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
