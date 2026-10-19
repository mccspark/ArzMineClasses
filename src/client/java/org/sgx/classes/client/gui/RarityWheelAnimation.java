package org.sgx.classes.client.gui;

/**
 * Состояние и математика анимации колеса — общая для RarityWheelScreen и RarityUpgradeWheelScreen.
 *
 * Фазы:
 *  1) ожидание: колесо лениво вращается само (приманка), стрелка тихо щёлкает о «штырьки»;
 *  2) раскрутка: плавный разгон из текущего положения, много оборотов, долгое драматичное замедление
 *     (последние градусы колесо ползёт, щёлкая по штырькам) — всё считается от времени с partialTicks, без рывков;
 *  3) остановка: колесо чуть «оседает» назад-вперёд, стрелка отскакивает.
 */
final class RarityWheelAnimation {

    /** Расстояние между невидимыми штырьками по ободу (градусы) — о них щёлкает стрелка и играет звук. */
    static final float PEG_SPACING = 15f;
    /** Скорость ленивого вращения до старта (градусов за тик). */
    private static final float IDLE_SPEED = 0.9f;

    private final float targetRotation; // угол колеса (0..360), при котором под стрелкой нужный сектор
    private final float fullSpins;
    private final float duration;

    private float startRotation;
    private float endRotation;

    private int openTicks = 0;
    private int spinTicks = 0;
    private int finishTicks = 0;
    private boolean spinning = false;
    private boolean finished = false;
    private boolean justFinished = false;
    private int lastPeg = 0;
    private int pegsCrossed = 0;

    RarityWheelAnimation(float targetRotation, float fullSpins, float durationTicks) {
        this.targetRotation = targetRotation;
        this.fullSpins = fullSpins;
        this.duration = durationTicks;
    }

    boolean isSpinning() {
        return spinning;
    }

    boolean isFinished() {
        return finished;
    }

    int openTicks() {
        return openTicks;
    }

    int finishTicks() {
        return finishTicks;
    }

    /** Сколько штырьков стрелка прошла за последний тик (для звука и искр). */
    int pegsCrossed() {
        return pegsCrossed;
    }

    /** true один раз, в тик, когда колесо остановилось. */
    boolean consumeJustFinished() {
        boolean r = justFinished;
        justFinished = false;
        return r;
    }

    void start() {
        if (spinning || finished) {
            return;
        }
        prepare();
        spinning = true;
        spinTicks = 0;
    }

    void skip() {
        if (finished) {
            return;
        }
        if (!spinning) {
            prepare();
        }
        finish();
    }

    private void prepare() {
        startRotation = idleRotation(openTicks);
        float delta = (((targetRotation - startRotation) % 360f) + 360f) % 360f;
        endRotation = startRotation + fullSpins * 360f + delta;
        lastPeg = (int) Math.floor(startRotation / PEG_SPACING);
    }

    private void finish() {
        spinning = false;
        finished = true;
        justFinished = true;
        finishTicks = 0;
    }

    void tick() {
        openTicks++;
        pegsCrossed = 0;
        if (finished) {
            finishTicks++;
            return;
        }
        if (!spinning) {
            return;
        }
        spinTicks++;
        int peg = (int) Math.floor(spinRotation(spinTicks) / PEG_SPACING);
        pegsCrossed = Math.min(6, Math.abs(peg - lastPeg));
        lastPeg = peg;
        if (spinTicks >= duration) {
            finish();
        }
    }

    /** Угол колеса для отрисовки; a — partialTick, поэтому движение плавное на любом FPS. */
    float rotation(float a) {
        if (finished) {
            return endRotation + settle(finishTicks + a);
        }
        if (!spinning) {
            return idleRotation(openTicks + a);
        }
        return spinRotation(spinTicks + a);
    }

    /** Текущая скорость вращения 0..1 (для свечения, высоты щелчков). */
    float speed() {
        if (!spinning) {
            return 0f;
        }
        float t = Math.min(1f, spinTicks / duration);
        return (float) (Math.pow(1.0 - t, 3.0) * (1.0 - Math.exp(-18.0 * t)));
    }

    /** Появление: колесо «выпрыгивает» с лёгким перелётом за ~12 тиков. */
    float entranceScale(float a) {
        float p = Math.min(1f, (openTicks + a) / 12f);
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float back = 1f + c3 * (float) Math.pow(p - 1f, 3.0) + c1 * (float) Math.pow(p - 1f, 2.0);
        return 0.3f + 0.7f * back;
    }

    private float spinRotation(float ticks) {
        float t = Math.max(0f, Math.min(1f, ticks / duration));
        return startRotation + ease(t) * (endRotation - startRotation);
    }

    /** Разгон + долгое замедление: скорость в начале 0, быстро растёт, потом квартично затухает. */
    private static float ease(float t) {
        double out = 1.0 - Math.pow(1.0 - t, 4.0);
        double accel = 1.0 - Math.exp(-18.0 * t);
        return (float) (out * accel);
    }

    private static float idleRotation(float ticks) {
        return ticks * IDLE_SPEED;
    }

    /** Колесо после остановки слегка покачивается и успокаивается. */
    private static float settle(float ticks) {
        return (float) (1.6 * Math.exp(-0.28 * ticks) * Math.sin(1.3 * ticks));
    }
}