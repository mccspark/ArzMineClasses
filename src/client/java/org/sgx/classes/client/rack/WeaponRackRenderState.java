package org.sgx.classes.client.rack;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.Direction;
import org.sgx.classes.block.WeaponRackBlockEntity;

public class WeaponRackRenderState extends BlockEntityRenderState {
    public final ItemStackRenderState[] items = new ItemStackRenderState[WeaponRackBlockEntity.SLOTS];
    /** true — предмет «диагональный» (меч, топор, кирка…) и ставится в стойку вертикально. */
    public final boolean[] upright = new boolean[WeaponRackBlockEntity.SLOTS];
    public Direction facing = Direction.NORTH;

    public WeaponRackRenderState() {
        for (int i = 0; i < items.length; i++) {
            items[i] = new ItemStackRenderState();
        }
    }
}
