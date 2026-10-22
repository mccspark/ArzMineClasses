package org.sgx.classes.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.network.OpenRarityWheelPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RarityWheelScreen extends Screen {

    private record Sector(ItemRarity rarity, float weight, float startAngle, float endAngle) {
    }

    private static final int PANEL_SIZE = 180;
    private static final int LEGEND_WIDTH = 130;
    /** Длительность раскрутки в тиках (20 тиков = 1 секунда). */
    private static final float SPIN_DURATION_TICKS = 140f;

    private final List<Sector> sectors = new ArrayList<>();
    private final List<RarityWheelRenderer.Sector> wheelSectors = new ArrayList<>();
    private final ItemRarity result;
    private final int resultIndex;
    private final RarityWheelAnimation anim;
    private final RarityWheelRenderer.Burst burst = new RarityWheelRenderer.Burst();

    private int left, top, centerX, centerY, radius, legendLeft;

    public RarityWheelScreen(OpenRarityWheelPayload payload) {
        super(Component.translatable("screen.arz_classes.rarity_wheel"));

        ItemRarity[] order = ItemRarity.values();
        float[] weights = {
                Math.max(0f, payload.commonWeight()),
                Math.max(0f, payload.rareWeight()),
                Math.max(0f, payload.epicWeight()),
                Math.max(0f, payload.legendaryWeight()),
                Math.max(0f, payload.mythicWeight())
        };
        float total = 0f;
        for (float w : weights) total += w;
        if (total <= 0f) total = 1f;

        float cursor = 0f;
        for (int i = 0; i < order.length; i++) {
            if (weights[i] <= 0f) {
                continue;
            }
            float start = cursor * 360f;
            cursor += weights[i] / total;
            float end = cursor * 360f;
            sectors.add(new Sector(order[i], weights[i], start, end));
            wheelSectors.add(new RarityWheelRenderer.Sector(order[i].argb(), weights[i] / total));
        }

        this.result = order[payload.resultOrdinal()];

        // Стрелка останавливается в СЛУЧАЙНОЙ точке выигравшего сектора (не по центру). Сервер решил только сектор.
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        float resultAngle = 0f;
        int index = 0;
        for (int i = 0; i < sectors.size(); i++) {
            Sector s = sectors.get(i);
            if (s.rarity() == result) {
                float width = s.endAngle() - s.startAngle();
                float margin = Math.min(width * 0.2f, 6f);
                resultAngle = s.startAngle() + margin + rnd.nextFloat() * Math.max(0f, width - 2f * margin);
                index = i;
                break;
            }
        }
        this.resultIndex = index;
        float targetRotation = (360f - resultAngle) % 360f;
        this.anim = new RarityWheelAnimation(targetRotation, 7 + rnd.nextInt(4), SPIN_DURATION_TICKS); // 7–10 оборотов
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_SIZE - LEGEND_WIDTH - 16) / 2;
        this.top = (this.height - PANEL_SIZE) / 2 - 20;
        this.centerX = left + PANEL_SIZE / 2;
        this.centerY = top + PANEL_SIZE / 2;
        this.radius = PANEL_SIZE / 2 - 6;
        this.legendLeft = left + PANEL_SIZE + 16;

        if (anim.isFinished()) {
            addBackButton();
            return;
        }
        if (anim.isSpinning()) {
            return; // во время вращения кнопок нет (например, после изменения размера окна)
        }
        addRenderableWidget(new StyledButton(left + PANEL_SIZE / 2 - 95, top + PANEL_SIZE + 20, 90, 20,
                Component.translatable("screen.arz_classes.rarity_wait"), this::startSpin));
        addRenderableWidget(new StyledButton(left + PANEL_SIZE / 2 + 5, top + PANEL_SIZE + 20, 90, 20,
                Component.translatable("screen.arz_classes.rarity_skip"), this::skip));
    }

    private void addBackButton() {
        addRenderableWidget(new StyledButton(left + PANEL_SIZE / 2 - 45, top + PANEL_SIZE + 20, 90, 20,
                Component.translatable("screen.arz_classes.back"), this::onClose));
    }

    private void startSpin() {
        if (anim.isSpinning() || anim.isFinished()) {
            return;
        }
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
        if (result != ItemRarity.COMMON) {
            burst.burst(centerX, centerY - radius, result.argb(), 40 + result.ordinal() * 14);
            if (withSound && Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.playSound(net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP, 0.6f, 1.0f);
            }
        } else if (withSound) {
            playClick(0.5f, 0.6f);
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
            // щелчок: чем быстрее колесо, тем выше тон; в конце — редкие низкие «тук»
            playClick(0.3f, 0.8f + 0.9f * anim.speed());
        }
        if (anim.consumeJustFinished()) {
            onFinished(true);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        RarityWheelRenderer.drawPanel(graphics, left - 10, top - 10, legendLeft + LEGEND_WIDTH + 10, top + PANEL_SIZE + 10);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        graphics.centeredText(this.font, this.title, left + PANEL_SIZE / 2, top - 20, BookStyle.title(0xFFFFFF));

        boolean finished = anim.isFinished();
        float time = finished ? anim.finishTicks() + a : anim.openTicks() + a;
        RarityWheelRenderer.Options options = new RarityWheelRenderer.Options(
                finished ? resultIndex : -1, time, anim.speed());

        // колесо «выпрыгивает» при открытии
        float scale = anim.entranceScale(a);
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) centerX, (float) centerY);
        graphics.pose().scale(scale);
        graphics.pose().translate((float) -centerX, (float) -centerY);
        RarityWheelRenderer.draw(graphics, centerX, centerY, radius, anim.rotation(a), wheelSectors, options);
        graphics.pose().popMatrix();

        burst.draw(graphics);
        drawLegend(graphics, finished, time);

        if (finished) {
            // название редкости «выпрыгивает» с пружиной
            float ft = anim.finishTicks() + a;
            float labelScale = (float) Math.max(0.3, Math.min(2.0, 1.0 + 0.7 * Math.exp(-0.3 * ft) * Math.cos(0.8 * ft)));
            Component label = Component.translatable("rarity.arz_classes." + result.getSerializedName());
            graphics.pose().pushMatrix();
            graphics.pose().translate((float) (left + PANEL_SIZE / 2), (float) (top + PANEL_SIZE + 8));
            graphics.pose().scale(labelScale);
            graphics.centeredText(this.font, label, 0, -4, result.argb());
            graphics.pose().popMatrix();

            // короткая вспышка цвета редкости на всё окно
            if (result != ItemRarity.COMMON) {
                int alpha = (int) (0x60 * Math.max(0f, 1f - ft / 10f));
                if (alpha > 0) {
                    graphics.fill(0, 0, this.width, this.height, (alpha << 24) | (result.argb() & 0xFFFFFF));
                }
            }
        }
    }

    private void drawLegend(GuiGraphicsExtractor graphics, boolean finished, float time) {
        int rowY = top + 4;
        graphics.text(this.font, Component.translatable("screen.arz_classes.rarity_legend_title"),
                legendLeft, rowY, BookStyle.body(0xFFCCCCCC));
        rowY += 14;

        for (int i = 0; i < sectors.size(); i++) {
            Sector s = sectors.get(i);
            if (finished && i == resultIndex) {
                int alpha = (int) (0x30 + 0x18 * Math.sin(time * 0.3));
                graphics.fill(legendLeft - 4, rowY - 2, legendLeft + LEGEND_WIDTH - 12, rowY + 22,
                        (alpha << 24) | (s.rarity().argb() & 0xFFFFFF));
            }

            graphics.fill(legendLeft, rowY, legendLeft + 10, rowY + 10, s.rarity().argb());
            graphics.outline(legendLeft, rowY, 10, 10, 0xFF000000);

            Component name = Component.translatable("rarity.arz_classes." + s.rarity().getSerializedName());
            graphics.text(this.font, name, legendLeft + 14, rowY + 1, s.rarity().argb());

            String pct = String.format("%.1f%%", (s.weight() / totalWeight()) * 100f);
            graphics.text(this.font, pct, legendLeft + 14, rowY + 10, BookStyle.dim(0xFFAAAAAA));

            rowY += 24;
        }
    }

    private float totalWeight() {
        float total = 0f;
        for (Sector s : sectors) total += s.weight();
        return total;
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