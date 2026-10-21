package org.sgx.classes.client.rack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.sgx.classes.block.WeaponRackBlock;
import org.sgx.classes.block.WeaponRackBlockEntity;
import org.sgx.classes.profession.WeaponSharpening;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;

/** Рисует до трёх предметов, висящих на стойке (как предметы в рамке, но по линии на стене). */
public class WeaponRackRenderer implements BlockEntityRenderer<WeaponRackBlockEntity, WeaponRackRenderState> {

    /** Центры слотов: 4, 8 и 12 пикселей — между столбами и планками (планки на 6 и 10). */
    private static final float SPACING = 0.25f;
    private static final float SCALE = 0.45f;
    /** Во сколько раз «утолщаем» предмет по глубине, чтобы он выглядел объёмной моделью, а не плоским спрайтом. */
    private static final float THICKNESS = 1.0f;
    /** Глубина центра предмета: он продет между планками (z 14–15 в модели), а концы уходят за рамку. */
    private static final float DEPTH = -0.37f;
    /** true — мечи/топоры/кирки/лопаты/копья ставятся вертикально; false — висят диагонально, как в рамке. */
    private static final boolean UPRIGHT = true;
    private static final float UPRIGHT_DEGREES = 45f;
    /**
     * Поворот предмета вокруг вертикальной оси: 0 — плоской стороной к игроку, 90 — плоской стороной вбок
     * (к игроку виден край). -90 — вбок в другую сторону; 45–60 даёт вид «в три четверти», как на рисунке.
     */
    private static final float YAW_DEGREES = 270f;
    /** FIXED-модель предмета смотрит «внутрь» блока; без разворота спрайты зеркальны. */
    private static final boolean TURN_TO_VIEWER = true;

    private final ItemModelResolver itemModelResolver;

    public WeaponRackRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public WeaponRackRenderState createRenderState() {
        return new WeaponRackRenderState();
    }

    @Override
    public void extractRenderState(WeaponRackBlockEntity blockEntity, WeaponRackRenderState state, float partialTicks,
                                   Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.facing = blockEntity.getBlockState().getValue(WeaponRackBlock.FACING);

        int seed = (int) blockEntity.getBlockPos().asLong();
        for (int i = 0; i < WeaponRackBlockEntity.SLOTS; i++) {
            ItemStack stack = blockEntity.getItem(i);
            state.upright[i] = UPRIGHT && isDiagonal(stack);
            if (stack.isEmpty()) {
                state.items[i].clear();
            } else {
                this.itemModelResolver.updateForTopItem(state.items[i], stack, ItemDisplayContext.FIXED,
                        blockEntity.getLevel(), null, seed + i);
            }
        }
    }

    /** Мечи, топоры, кирки, лопаты, мотыги, копья, ножи, трезубец — спрайты нарисованы по диагонали. */
    private static boolean isDiagonal(ItemStack stack) {
        return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.PICKAXES)
                || stack.is(ItemTags.SHOVELS) || stack.is(ItemTags.HOES) || stack.is(Items.TRIDENT)
                || WeaponSharpening.isSpear(stack) || WeaponSharpening.isKnife(stack);
    }

    @Override
    public void submit(WeaponRackRenderState state, PoseStack poseStack, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        for (int i = 0; i < WeaponRackBlockEntity.SLOTS; i++) {
            if (state.items[i].isEmpty()) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.5f, 0.5f);
            // локальная система: +Z — наружу от стены (к игроку), +X — вправо от игрока
            poseStack.mulPose(Axis.YP.rotationDegrees(-state.facing.toYRot()));
            poseStack.translate((i - 1) * SPACING, 0.0f, DEPTH);
            // порядок важен: сначала поворот вокруг вертикальной оси, потом наклон в плоскости самого спрайта
            poseStack.mulPose(Axis.YP.rotationDegrees(YAW_DEGREES));
            if (state.upright[i]) {
                poseStack.mulPose(Axis.ZP.rotationDegrees(UPRIGHT_DEGREES));
            }
            if (TURN_TO_VIEWER) {
                poseStack.mulPose(Axis.YP.rotationDegrees(180f));
            }
            poseStack.scale(SCALE, SCALE, SCALE * THICKNESS);
            state.items[i].submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }
}
