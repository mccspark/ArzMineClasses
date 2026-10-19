package org.sgx.classes.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.sgx.classes.Arz_classes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Общая отрисовка колеса для RarityWheelScreen (крафт) и RarityUpgradeWheelScreen (улучшение).
 *
 *  - сектора сплошного цвета, тонкие тёмные разделители, плоский ободок со штырьками и ступица в центре;
 *  - стрелка (спрайт rarity/needle) качается и щёлкает о штырьки ТОЛЬКО пока колесо раскручено кнопкой «Начать»;
 *  - мягкое свечение вокруг колеса: во время вращения цвета сектора под стрелкой, после остановки цвета победителя;
 *  - после остановки победивший сектор остаётся ярким, остальные тускнеют;
 *  - Burst — искры: мелкие от стрелки при щелчках и большая россыпь при хорошем результате.
 */
public final class RarityWheelRenderer {

    /** Сектор: цвет (ARGB) и доля круга (0..1). Секторы идут по часовой стрелке от верха. */
    public record Sector(int color, double fraction) {
    }

    /**
     * highlight — индекс выигравшего сектора после остановки (-1, пока ждём/крутится);
     * time — тики для пульсаций; speed — скорость вращения 0..1 (0 = колесо не раскручено, стрелка неподвижна).
     */
    public record Options(int highlight, float time, float speed) {
        public static final Options IDLE = new Options(-1, 0f, 0f);
    }

    /** Спрайт textures/gui/sprites/rarity/needle.png. */
    private static final Identifier NEEDLE_SPRITE = Arz_classes.id("rarity/needle");
    private static final int NEEDLE_W = 20;
    private static final int NEEDLE_H = 24;
    private static final int NEEDLE_OVERLAP = 8;
    private static final int NEEDLE_PIVOT_Y = 5;
    /** Максимальный наклон стрелки (радианы). Знак меняйте, если качается не в ту сторону. */
    private static final float NEEDLE_MAX_TILT = -0.35f;
    /** За сколько градусов до штырька стрелка начинает отклоняться / сколько длится отскок после. */
    private static final double RAMP_DEG = 7.0;
    private static final double REBOUND_DEG = 5.0;

    private static final int OUTLINE = 0xFF000000;
    private static final int RING_BODY = 0xFF7A7A7A;
    private static final int PEG = 0xFFE6E6E6;
    private static final int HUB_BODY = 0xFF7A7A7A;
    private static final int SEPARATOR = 0xFF000000;
    private static final double HUB_RADIUS = 9.0;
    /** Насколько тускнеют проигравшие секторы после остановки. */
    private static final double DIM_FACTOR = 0.5;

    /** Свечение: GLOW_LAYERS слоёв, каждый шире предыдущего на GLOW_STEP пикселей (итого 16 px). */
    private static final int GLOW_LAYERS = 8;
    private static final int GLOW_STEP = 2;

    /** Толщина ободка: 1 обводка + 4 тело + 1 внутренняя обводка. */
    public static final int RING_WIDTH = 6;

    private RarityWheelRenderer() {
    }

    /** Панель в стиле майнкрафтовского тултипа (тёмный фон и фиолетовая рамка). */
    public static void drawPanel(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1) {
        g.fill(x0, y0, x1, y1, 0xF0100010);
        g.fill(x0, y0, x1, y0 + 1, 0xA05000FF);        // верх
        g.fill(x0, y0, x0 + 1, y1, 0xA05000FF);        // лево
        g.fill(x0, y1 - 1, x1, y1, 0xA028007F);        // низ
        g.fill(x1 - 1, y0, x1, y1, 0xA028007F);        // право
    }

    /** Индекс сектора под стрелкой при данном повороте колеса. */
    public static int sectorIndexAtNeedle(float rotationDeg, List<Sector> sectors) {
        double pointer = ((-rotationDeg % 360.0) + 360.0) % 360.0;
        double acc = 0.0;
        for (int i = 0; i < sectors.size(); i++) {
            acc += sectors.get(i).fraction() * 360.0;
            if (pointer < acc) {
                return i;
            }
        }
        return sectors.size() - 1;
    }

    /** Колесо радиусом radius (с ободком) с центром в (cx, cy), повёрнутое на rotationDeg по часовой. */
    public static void draw(GuiGraphicsExtractor graphics, int cx, int cy, int radius,
                            float rotationDeg, List<Sector> sectors, Options options) {
        double[] bounds = new double[sectors.size()];
        double acc = 0.0;
        for (int i = 0; i < sectors.size(); i++) {
            acc += sectors.get(i).fraction() * 360.0;
            bounds[i] = acc;
        }

        drawGlow(graphics, cx, cy, radius, rotationDeg, sectors, options);

        for (int dy = -radius; dy < radius; dy++) {
            int runStart = 0;
            int runColor = 0;
            for (int dx = -radius; dx <= radius; dx++) {
                int color = dx == radius ? 0
                        : pixelColor(dx + 0.5, dy + 0.5, radius, rotationDeg, sectors, bounds, options);
                if (color != runColor) {
                    if (runColor != 0) {
                        graphics.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, runColor);
                    }
                    runStart = dx;
                    runColor = color;
                }
            }
        }

        drawNeedle(graphics, cx, cy, radius, rotationDeg, bounds, options);
    }

    /**
     * Мягкое свечение вокруг колеса: во время вращения — цвет сектора под стрелкой (живо меняется),
     * после остановки — цвет победителя (пульсирует). Слои накладываются от дальнего к ближнему,
     * поэтому яркость плавно спадает от края колеса наружу.
     */
    private static void drawGlow(GuiGraphicsExtractor graphics, int cx, int cy, int radius,
                                 float rotationDeg, List<Sector> sectors, Options options) {
        int index;
        double peak; // суммарная непрозрачность свечения у самого края колеса (0..1)
        if (options.highlight() >= 0 && options.highlight() < sectors.size()) {
            index = options.highlight();
            peak = 0.36 * (0.75 + 0.25 * Math.sin(options.time() * 0.25));
        } else {
            index = sectorIndexAtNeedle(rotationDeg, sectors);
            peak = (0.18 + 0.30 * options.speed()) * (0.85 + 0.15 * Math.sin(options.time() * 0.3));
        }
        int rgb = sectors.get(index).color() & 0xFFFFFF;
        int alpha = Math.max(1, Math.min(255, (int) Math.round(255.0 * peak / GLOW_LAYERS)));
        for (int layer = GLOW_LAYERS; layer >= 1; layer--) {
            fillDisc(graphics, cx, cy, radius + layer * GLOW_STEP, (alpha << 24) | rgb);
        }
    }

    private static void drawNeedle(GuiGraphicsExtractor graphics, int cx, int cy, int radius,
                                   float rotationDeg, double[] bounds, Options options) {
        // стрелка живая только пока колесо раскручено (speed > 0) и результат ещё не показан;
        // до нажатия «Начать» и после остановки она неподвижна
        float tilt = 0f;
        if (options.highlight() < 0 && options.speed() > 0f) {
            // на большой скорости качание слабее, чтобы щелчки не сливались в дрожь
            tilt = needleTilt(rotationDeg, bounds) * (1f - 0.6f * Math.min(1f, options.speed()));
        }
        float topY = cy - radius - NEEDLE_H + NEEDLE_OVERLAP;

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) cx, topY + NEEDLE_PIVOT_Y);
        graphics.pose().rotate(tilt * NEEDLE_MAX_TILT);
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, NEEDLE_SPRITE,
                -NEEDLE_W / 2, -NEEDLE_PIVOT_Y, NEEDLE_W, NEEDLE_H);
        graphics.pose().popMatrix();
    }

    /**
     * Наклон стрелки: пока штырёк (граница сектора или регулярный) приближается к стрелке — она отклоняется 0..1,
     * в момент прохода щёлкает назад и коротко отскакивает в обратную сторону (-0.35..0).
     */
    private static float needleTilt(float rotationDeg, double[] bounds) {
        double pointer = ((-rotationDeg % 360.0) + 360.0) % 360.0;
        double best = pegTilt(pointer, RarityWheelAnimation.PEG_SPACING);
        for (double b : bounds) {
            double d = (((pointer - (b % 360.0)) % 360.0) + 360.0) % 360.0; // [0, 360)
            best = strongest(best, tiltFor(d, 360.0));
        }
        return (float) best;
    }

    private static double pegTilt(double pointer, double spacing) {
        double d = pointer % spacing; // сколько градусов выше ближайшего нижнего штырька
        return tiltFor(d, spacing);
    }

    /** d — на сколько градусов указатель выше штырька (по часовой от штырька), spacing — период. */
    private static double tiltFor(double d, double spacing) {
        if (d < RAMP_DEG) {
            return 1.0 - d / RAMP_DEG;                 // штырёк подпирает стрелку
        }
        double behind = spacing - d;                    // сколько осталось до следующего штырька выше
        if (behind < REBOUND_DEG) {
            return -0.35 * (1.0 - behind / REBOUND_DEG); // отскок после прохода
        }
        return 0.0;
    }

    private static double strongest(double a, double b) {
        return Math.abs(b) > Math.abs(a) ? b : a;
    }

    private static void fillDisc(GuiGraphicsExtractor graphics, int cx, int cy, int r, int color) {
        for (int dy = -r; dy < r; dy++) {
            double y = dy + 0.5;
            int hw = (int) Math.sqrt(Math.max(0.0, (double) r * r - y * y));
            if (hw > 0) {
                graphics.fill(cx - hw, cy + dy, cx + hw, cy + dy + 1, color);
            }
        }
    }

    private static int pixelColor(double x, double y, int radius, float rotationDeg,
                                  List<Sector> sectors, double[] bounds, Options options) {
        double d = Math.sqrt(x * x + y * y);
        if (d > radius) {
            return 0;
        }
        if (d > radius - 1) {
            return OUTLINE;
        }

        double angle = Math.toDegrees(Math.atan2(x, -y)) - rotationDeg;
        angle = ((angle % 360.0) + 360.0) % 360.0;

        // ободок: серое тело с маленькими светлыми штырьками (о них щёлкает стрелка)
        if (d > radius - 5) {
            double rem = angle % RarityWheelAnimation.PEG_SPACING;
            double delta = Math.min(rem, RarityWheelAnimation.PEG_SPACING - rem);
            if (d > radius - 4 && d < radius - 2 && d * Math.toRadians(delta) < 1.0) {
                return PEG;
            }
            return RING_BODY;
        }
        if (d > radius - RING_WIDTH) {
            return OUTLINE;
        }

        // ступица в центре
        if (d < HUB_RADIUS - 1) {
            return HUB_BODY;
        }
        if (d < HUB_RADIUS) {
            return OUTLINE;
        }

        // тонкий тёмный разделитель на границах секторов (не рисуем, если сектор один)
        if (sectors.size() > 1) {
            for (double b : bounds) {
                double delta = Math.abs(angle - (b % 360.0));
                delta = Math.min(delta, 360.0 - delta);
                if (d * Math.toRadians(delta) < 0.9) {
                    return SEPARATOR;
                }
            }
        }

        int index = sectors.size() - 1;
        for (int i = 0; i < sectors.size(); i++) {
            if (angle < bounds[i]) {
                index = i;
                break;
            }
        }
        int base = sectors.get(index).color();

        // после остановки проигравшие секторы тускнеют; победитель остаётся ярким
        if (options.highlight() >= 0 && index != options.highlight()) {
            return scale(base, DIM_FACTOR);
        }
        return base;
    }

    private static int scale(int argb, double f) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, (int) Math.round(((argb >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) Math.round(((argb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) Math.round((argb & 0xFF) * f));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /** Искры: spark() — мелкие от стрелки при щелчках, burst() — большая россыпь при остановке; tick() и draw() каждый кадр/тик. */
    public static final class Burst {

        private static final class Particle {
            float x, y, vx, vy;
            int age, life, rgb;
            int size;
        }

        private final List<Particle> particles = new ArrayList<>();

        /** Несколько искорок вверх-в стороны (щелчок стрелки о штырёк). */
        public void spark(float originX, float originY, int argb, int count) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (int i = 0; i < count; i++) {
                Particle p = new Particle();
                double ang = -Math.PI / 2.0 + (rnd.nextDouble() - 0.5) * 2.2;
                double speed = 0.8 + rnd.nextDouble() * 1.6;
                p.x = originX;
                p.y = originY;
                p.vx = (float) (Math.cos(ang) * speed);
                p.vy = (float) (Math.sin(ang) * speed);
                p.life = 10 + rnd.nextInt(10);
                p.size = 1;
                p.rgb = argb & 0xFFFFFF;
                particles.add(p);
            }
        }

        /** Большая россыпь во все стороны (результат). */
        public void burst(float originX, float originY, int argb, int count) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            for (int i = 0; i < count; i++) {
                Particle p = new Particle();
                double ang = rnd.nextDouble() * Math.PI * 2.0;
                double speed = 1.0 + rnd.nextDouble() * 3.8;
                p.x = originX;
                p.y = originY;
                p.vx = (float) (Math.cos(ang) * speed);
                p.vy = (float) (Math.sin(ang) * speed) - 0.6f;
                p.life = 18 + rnd.nextInt(26);
                p.size = rnd.nextInt(3) == 0 ? 3 : 2;
                p.rgb = rnd.nextInt(5) == 0 ? 0xFFFFFF : (argb & 0xFFFFFF);
                particles.add(p);
            }
        }

        public void tick() {
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                p.x += p.vx;
                p.y += p.vy;
                p.vy += 0.12f;
                p.vx *= 0.97f;
                if (++p.age >= p.life) {
                    it.remove();
                }
            }
        }

        public void draw(GuiGraphicsExtractor graphics) {
            for (Particle p : particles) {
                float t = 1f - (float) p.age / p.life;
                int alpha = Math.max(0, Math.min(255, (int) (255 * t)));
                int size = p.age < p.life / 2 ? p.size : Math.max(1, p.size - 1);
                int px = Math.round(p.x);
                int py = Math.round(p.y);
                graphics.fill(px, py, px + size, py + size, (alpha << 24) | p.rgb);
            }
        }
    }
}