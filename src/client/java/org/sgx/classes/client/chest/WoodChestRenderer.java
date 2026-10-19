package org.sgx.classes.client.chest;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.floats.Float2FloatFunction;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.MultiblockChestResources;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.DoubleBlockCombiner;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.block.chest.WoodChestBlock;

import java.util.HashMap;
import java.util.Map;

/**
 * Рендерер сундуков из разных пород. Копия ванильного ChestRenderer, но текстуру берёт не из
 * ChestMaterialType, а по породе блока. Текстуры: textures/block/chest/<порода>/{normal,left,right,trap,trap_left,trap_right}.png
 * (в атлас сундуков подключаются файлом assets/minecraft/atlases/chest.json).
 */
public class WoodChestRenderer implements BlockEntityRenderer<ChestBlockEntity, WoodChestRenderState> {

    private static final Map<String, MultiblockChestResources<SpriteId>> NORMAL = new HashMap<>();
    private static final Map<String, MultiblockChestResources<SpriteId>> TRAPPED = new HashMap<>();

    private final SpriteGetter sprites;
    private final MultiblockChestResources<ChestModel> models;

    public WoodChestRenderer(BlockEntityRendererProvider.Context context) {
        this.sprites = context.sprites();
        this.models = ChestRenderer.LAYERS.map(layer -> new ChestModel(context.bakeLayer(layer)));
    }

    private static MultiblockChestResources<SpriteId> spritesFor(String wood, boolean trapped) {
        return (trapped ? TRAPPED : NORMAL).computeIfAbsent(wood, w -> {
            String single = trapped ? "trap" : "normal";
            String left = trapped ? "trap_left" : "left";
            String right = trapped ? "trap_right" : "right";
            return new MultiblockChestResources<>(
                    Arz_classes.id(w + "/" + single),
                    Arz_classes.id(w + "/" + left),
                    Arz_classes.id(w + "/" + right)
            ).map(Sheets.CHEST_MAPPER::apply);
        });
    }

    @Override
    public WoodChestRenderState createRenderState() {
        return new WoodChestRenderState();
    }

    @Override
    public void extractRenderState(ChestBlockEntity blockEntity, WoodChestRenderState state, float partialTicks,
                                   Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);

        BlockState blockState = blockEntity.getBlockState();
        state.type = blockState.hasProperty(ChestBlock.TYPE) ? blockState.getValue(ChestBlock.TYPE) : ChestType.SINGLE;
        state.facing = blockState.getValue(ChestBlock.FACING);

        Block block = blockState.getBlock();
        if (block instanceof WoodChestBlock woodChest) {
            state.sprites = spritesFor(woodChest.wood(), woodChest.isTrapped());
        } else {
            state.sprites = spritesFor("oak", false);
        }

        DoubleBlockCombiner.NeighborCombineResult<? extends ChestBlockEntity> combineResult;
        if (blockEntity.getLevel() != null && block instanceof ChestBlock chestBlock) {
            combineResult = chestBlock.combine(blockState, blockEntity.getLevel(), blockEntity.getBlockPos(), true);
        } else {
            combineResult = DoubleBlockCombiner.Combiner::acceptNone;
        }
        Float2FloatFunction openness = combineResult.apply(ChestBlock.opennessCombiner(blockEntity));
        state.open = openness.get(partialTicks);
    }

    @Override
    public void submit(WoodChestRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(ChestRenderer.modelTransformation(state.facing));
        float open = state.open;
        open = 1.0F - open;
        open = 1.0F - open * open * open;
        SpriteId spriteId = state.sprites.select(state.type);
        ChestModel model = this.models.select(state.type);
        collector.submitModel(model, open, poseStack, state.lightCoords, OverlayTexture.NO_OVERLAY, -1,
                spriteId, this.sprites, 0, state.breakProgress);
        poseStack.popPose();
    }
}
