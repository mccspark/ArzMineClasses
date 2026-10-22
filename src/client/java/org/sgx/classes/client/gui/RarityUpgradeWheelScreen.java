package org.sgx.classes.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.network.OpenRarityUpgradeWheelPayload;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RarityUpgradeWheelScreen extends Screen {

    private static final int PANEL_SIZE = 160;
    private static final float SPIN_DURATION_TICKS = 120f;

    private static final int SUCCESS_COLOR = 0xFF3FBF4F;
    private static final int FAIL_COLOR = 0xFF7A1010;

    private final boolean success;
    private final ItemRarity target;
    private final float successChance;
    private final List<RarityWheelRenderer.Sector> wheelSectors;
    private final RarityWheelAnimation anim;
    private final RarityWheelRenderer.Burst burst = new RarityWheelRenderer.Burst();

    private int left, top, centerX, centerY, radius;

    public RarityUpgradeWheelScreen(OpenRarityUpgradeWheelPayload payload) {
        super(Component.translatable("screen.arz_classes.rarity_upgrade_wheel"));
        this.success = payload.success();
        this.target = ItemRarity.values()[payload.targetRarityOrdinal()];
        this.successChance = payload.successChance();
        // секторы: 0 — успех, 1 — провал
        this.wheelSectors = List.of(
                new RarityWheelRenderer.Sector(SUCCESS_COLOR, successChance),
                new RarityWheelRenderer.Sector(FAIL_COLOR, 1.0 - successChance));

        // Случайная точка остановки внутри выигравшего сектора (не по центру)
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        float successEndAngle = successChance * 360f;
        float from = success ? 0f : successEndAngle;
        float to = success ? successEndAngle : 360f;
        float width = to - from;
        float margin = Math.min(width * 0.2f, 6f);
        float resultAngle = from + margin + rnd.nextFloat() * Math.max(0f, width - 2f * margin);
        float targetRotation = (360f - resultAngle) % 360f;
        this.anim = new RarityWheelAnimation(targetRotation, 6 + rnd.nextInt(4), SPIN_DURATION_TICKS); // 6–9 оборотов
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_SIZE) / 2;
        this.top = (this.height - PANEL_SIZE) / 2 - 20;
        this.centerX = left + PANEL_SIZE / 2;
        this.centerY = top + PANEL_SIZE / 2;
        this.radius = PANEL_SIZE / 2 - 6;

        if (anim.isFinished()) {
            addBackButton();
            return;
        }
        if (anim.isSpinning()) {
            return; // во время вращения кнопок нет (например, после изменения размера окна)
        }
        addRenderableWidget(new StyledButton(centerX - 95, top + PANEL_SIZE + 20, 90, 20,
                Component.translatable("screen.arz_classes.rarity_wait"), this::startSpin));
        addRenderableWidget(new StyledButton(centerX + 5, top + PANEL_SIZE + 20, 90, 20,
                Component.translatable("screen.arz_classes.rarity_skip"), this::skip));
    }

    private void addBackButton() {
        addRenderableWidget(new StyledButton(centerX - 45, top + PANEL_SIZE + 20, 90, 20,
                Component.translatable("screen.arz_classes.back"), this::onClose));
    }

    private void startSpin() {
        if (anim.isSpinning() || anim.isFinished()) return;
        anim.start();
        clearWidgets();
        playClick(0.5f, 1.2f);
    }

    private void skip() {
        anim.skip();
        if (anim.consumeJustFinished()) {
            onFinished(false);
        }
    }

    private void playClick(float volume, float pitch) {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(
                    net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), volume, pitch);
        }
    }

    private void onFinished(boolean withSound) {
        clearWidgets();
        addBackButton();
        if (success) {
            burst.burst(centerX, centerY - radius, target.argb(), 50 + target.ordinal() * 12);
        }
        if (withSound && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.playSound(
                    success ? net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP : net.minecraft.sounds.SoundEvents.ITEM_BREAK.value(),
                    0.6f, 1.0f);
        }
    }

    @Override
    public void tick() {
        super.tick();
        anim.tick();
        burst.tick();

        if (anim.isSpinning() && anim.pegsCrossed() > 0) {
            int idx = RarityWheelRenderer.sectorIndexAtNeedle(anim.rotation(0f), wheelSectors);
            burst.spark(centerX, centerY - radius + 2, wheelSectors.get(idx).color(), Math.min(3, anim.pegsCrossed()));
            playClick(0.3f, 0.8f + 0.9f * anim.speed());
        }
        if (anim.consumeJustFinished()) {
            onFinished(true);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        RarityWheelRenderer.drawPanel(graphics, left - 10, top - 10, left + PANEL_SIZE + 10, top + PANEL_SIZE + 10);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        graphics.centeredText(this.font, this.title, centerX, top - 20, BookStyle.title(0xFFFFFFFF));

        boolean finished = anim.isFinished();
        float time = finished ? anim.finishTicks() + a : anim.openTicks() + a;
        RarityWheelRenderer.Options options = new RarityWheelRenderer.Options(
                finished ? (success ? 0 : 1) : -1, time, anim.speed());

        float scale = anim.entranceScale(a);
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) centerX, (float) centerY);
        graphics.pose().scale(scale);
        graphics.pose().translate((float) -centerX, (float) -centerY);
        RarityWheelRenderer.draw(graphics, centerX, centerY, radius, anim.rotation(a), wheelSectors, options);
        graphics.pose().popMatrix();

        burst.draw(graphics);

        if (finished) {
            float ft = anim.finishTicks() + a;
            float labelScale = (float) Math.max(0.3, Math.min(2.0, 1.0 + 0.7 * Math.exp(-0.3 * ft) * Math.cos(0.8 * ft)));
            Component label = success
                    ? Component.translatable("screen.arz_classes.rarity_upgrade_success",
                    Component.translatable(target.translationKey()))
                    : Component.translatable("screen.arz_classes.rarity_upgrade_fail");
            int color = success ? target.argb() : FAIL_COLOR;
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) centerX, (float) (top + PANEL_SIZE + 8));
            graphics.pose().scale(labelScale);
            graphics.centeredText(this.font, label, 0, -4, color);
            graphics.pose().popMatrix();

            if (success) {
                int alpha = (int) (0x60 * Math.max(0f, 1f - ft / 10f));
                if (alpha > 0) {
                    graphics.fill(0, 0, this.width, this.height, (alpha << 24) | (target.argb() & 0xFFFFFF));
                }
            }
        }
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(null);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return anim.isFinished();
    }
}