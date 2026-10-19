package org.sgx.classes.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.sgx.classes.network.SharpeningResultPayload;
import org.sgx.classes.network.SharpeningZone;

/**
 * Тайминг-бар с тремя вложенными зонами вокруг центра: PERFECT (центр), MEDIUM, BAD.
 * Нужно нажать ПРОБЕЛ, когда ползунок в зоне — чем ближе к центру, тем лучше результат.
 * Мимо всех зон = MISS.
 *
 * Последствия (заточка): PERFECT — предмет затачивается (+звук наковальни); MEDIUM — −25%
 * прочности; BAD — −75% прочности; MISS — прочность падает до 1. Текст с правилами
 * показывается на экране с самого старта.
 *
 * repair переключает палитру, заголовок и текст правил (ремонт): на подсчёт зон не влияет.
 *
 * Позиция ползунка считается от System.nanoTime() на каждом кадре рендера (не от тиков).
 * Результат нажатия считается по позиции, КОТОРУЮ ИГРОК ВИДЕЛ на последнем отрисованном
 * кадре, а не по времени обработки события клавиши — так нет сдвига на задержку кадра.
 */
public final class SharpeningScreen extends Screen {

    private static final int BAR_WIDTH = 260;
    private static final int BAR_HEIGHT = 20;
    private static final int RULE_LINE_HEIGHT = 11;

    /** Защита от случайного нажатия: ПРОБЕЛ в первые мс после открытия экрана игнорируется. */
    private static final long INPUT_DELAY_NANOS = 300_000_000L;

    private final float center;
    private final float perfectWidth;
    private final float mediumWidth;
    private final float badWidth;
    private final float barSpeed;
    private final boolean repair;

    private final long startTimeNanos = System.nanoTime();
    private boolean resolved = false;
    private float shownPos = -1f; // позиция на последнем отрисованном кадре (-1 = кадра ещё не было)

    public SharpeningScreen(float center, float perfectWidth, float mediumWidth, float badWidth,
                            float barSpeed, boolean repair) {
        super(Component.translatable(repair
                ? "screen.arz_classes.repair_title"
                : "screen.arz_classes.sharpening_title"));
        this.center = center;
        this.perfectWidth = perfectWidth;
        this.mediumWidth = mediumWidth;
        this.badWidth = badWidth;
        this.barSpeed = barSpeed;
        this.repair = repair;
    }

    /** Треугольная волна 0..1..0, период = 2/barSpeed секунд, посчитана от реального времени. */
    private float currentSliderPos() {
        double elapsedSeconds = (System.nanoTime() - startTimeNanos) / 1_000_000_000.0;
        double t = elapsedSeconds * barSpeed;
        double phase = t % 2.0;
        return (float) (phase <= 1.0 ? phase : 2.0 - phase);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_SPACE) {
            if (System.nanoTime() - startTimeNanos >= INPUT_DELAY_NANOS) {
                resolve();
            }
            return true;
        }
        return super.keyPressed(event);
    }

    private void resolve() {
        if (resolved) {
            return;
        }
        resolved = true;
        float pos = shownPos >= 0f ? shownPos : currentSliderPos();
        ClientPlayNetworking.send(new SharpeningResultPayload(zoneAt(pos)));
        this.onClose();
    }

    private SharpeningZone zoneAt(float pos) {
        float dist = Math.abs(pos - center);
        if (dist <= perfectWidth / 2f) {
            return SharpeningZone.PERFECT;
        }
        if (dist <= mediumWidth / 2f) {
            return SharpeningZone.MEDIUM;
        }
        if (dist <= badWidth / 2f) {
            return SharpeningZone.BAD;
        }
        return SharpeningZone.MISS;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);

        float pos = currentSliderPos();
        shownPos = pos;

        int barX = (this.width - BAR_WIDTH) / 2;
        int barY = this.height / 2;

        graphics.centeredText(this.font, this.getTitle(), this.width / 2, barY - 30, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.translatable("screen.arz_classes.sharpen_hint"),
                this.width / 2, barY - 16, 0xFFFFFF55);

        int badColor = repair ? 0xFF3A6FB0 : 0xFFD8B400;
        int mediumColor = repair ? 0xFF3F8FE0 : 0xFF4C9A3A;
        int perfectColor = repair ? 0xFF6FD0FF : 0xFF7CFF6B;

        graphics.fill(barX, barY, barX + BAR_WIDTH, barY + BAR_HEIGHT, 0xFF202020);
        drawZone(graphics, barX, barY, badWidth, badColor);
        drawZone(graphics, barX, barY, mediumWidth, mediumColor);
        drawZone(graphics, barX, barY, perfectWidth, perfectColor);

        int sliderX = barX + Math.round(pos * BAR_WIDTH);
        graphics.fill(sliderX - 1, barY - 3, sliderX + 1, barY + BAR_HEIGHT + 3, 0xFFFFFFFF);

        // Правила: что будет при каждом результате.
        String prefix = repair ? "screen.arz_classes.repair_rules_" : "screen.arz_classes.sharpen_rules_";
        int y = barY + BAR_HEIGHT + 14;
        int cx = this.width / 2;
        graphics.centeredText(this.font, Component.translatable(prefix + "perfect"), cx, y, perfectColor);
        y += RULE_LINE_HEIGHT;
        graphics.centeredText(this.font, Component.translatable(prefix + "medium"), cx, y, mediumColor);
        y += RULE_LINE_HEIGHT;
        graphics.centeredText(this.font, Component.translatable(prefix + "bad"), cx, y, badColor);
        y += RULE_LINE_HEIGHT;
        graphics.centeredText(this.font, Component.translatable(prefix + "miss"), cx, y, 0xFFE05050);
    }

    private void drawZone(GuiGraphicsExtractor graphics, int barX, int barY, float width, int color) {
        int zoneX = barX + Math.round((center - width / 2f) * BAR_WIDTH);
        int zoneW = Math.round(width * BAR_WIDTH);
        graphics.fill(zoneX, barY, zoneX + zoneW, barY + BAR_HEIGHT, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}