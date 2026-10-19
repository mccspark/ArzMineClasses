package org.sgx.classes.client.chest;

import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;

public class WoodChestRenderState extends ChestRenderState {
    /** Текстуры (одиночный/левый/правый) именно этой породы. */
    public MultiblockChestResources<SpriteId> sprites;
}
