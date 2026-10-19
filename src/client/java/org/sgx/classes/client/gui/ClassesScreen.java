package org.sgx.classes.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.client.ClientClassStats;
import org.sgx.classes.client.ClientProfessionData;
import org.sgx.classes.client.network.ProfessionClientNetworking;
import org.sgx.classes.profession.ProfessionColors;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionIcons;
import org.sgx.classes.profession.ProfessionType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClassesScreen extends Screen {

    /** Эталонные (максимальные) размеры; при нехватке места панели сжимаются вместе. */
    private static final int BASE_MAIN_WIDTH = 260;
    private static final int BASE_SIDE_WIDTH = CharacterPanel.WIDTH;
    private static final int MIN_MAIN_WIDTH = 196;
    private static final int MIN_SIDE_WIDTH = 100;

    private static final int PANEL_HEIGHT = 232;
    /** Ниже этой высоты содержимое панели начинает налезать друг на друга. */
    private static final int MIN_PANEL_HEIGHT = 208;
    private static final int GAP = 4;
    private static final int MARGIN = 4;

    /** Нижний ряд кнопок: общая высота и отступ от низа панели — один на все три панели. */
    private static final int BUTTON_H = 18;
    private static final int BUTTON_BOTTOM_MARGIN = 8;
    /** Боковой отступ в основной панели (полоска опыта и кнопки «Назад»/«Руководство» используют его одинаково). */
    private static final int PAD = 10;

    private static final int XP_BAR_H = 8;
    /** На сколько частей разбита полоска опыта. */
    private static final int XP_SEGMENTS = 10;

    /** Ключи (игрок@мир/класс, см. GuideProgress.key), для которых за эту игровую сессию уже автоматически
     * открывали руководство (защита от зацикливания, если файл guide_seen.json не удалось записать). */
    private static final Set<String> AUTO_GUIDE_SHOWN = new HashSet<>();

    private final Screen parent;

    private int left;
    private int top;
    private int panelWidth = BASE_MAIN_WIDTH;
    private int sideWidth = BASE_SIDE_WIDTH;
    private int panelHeight = PANEL_HEIGHT;
    /** Левая панель (атрибуты) скрывается, если экран слишком узкий; дерево навыков справа — всегда. */
    private boolean showCharacterPanel = true;

    private int carouselIndex = 0;
    private boolean carouselInitialized = false;

    private int xpBarX, xpBarY, xpBarW, xpBarH, xpBarCurrent, xpBarNeeded;

    private ProfessionType lastKnownProfession = ProfessionType.NONE;

    private CharacterPanel characterPanel;
    private WeaponPanel weaponPanel;

    public ClassesScreen(Screen parent) {
        super(Component.translatable("screen.arz_classes.classes"));
        this.parent = parent;
    }

    private static List<ProfessionType> professionValues() {
        List<ProfessionType> list = new ArrayList<>();
        for (ProfessionType t : ProfessionType.values()) {
            if (t != ProfessionType.NONE) {
                list.add(t);
            }
        }
        return list;
    }

    /** Верх нижнего ряда кнопок — одинаковый для «Прокачки», «Руководства», «Назад» и «Рецептов». */
    private int buttonRowY() {
        return top + panelHeight - BUTTON_BOTTOM_MARGIN - BUTTON_H;
    }

    /** Считает ширины панелей под текущий размер окна/масштаб интерфейса: все панели сжимаются вместе. */
    private void computeLayout() {
        int avail = this.width - 2 * MARGIN;
        int minThree = 2 * MIN_SIDE_WIDTH + MIN_MAIN_WIDTH + 2 * GAP;
        this.showCharacterPanel = avail >= minThree;

        int panels = showCharacterPanel ? 3 : 2;
        int gaps = (panels - 1) * GAP;
        int idealContent = BASE_MAIN_WIDTH + (panels - 1) * BASE_SIDE_WIDTH;
        int content = Math.min(idealContent, avail - gaps);
        double k = (double) content / idealContent;

        int side = Math.max(MIN_SIDE_WIDTH, (int) Math.round(BASE_SIDE_WIDTH * k));
        int main = content - (panels - 1) * side;
        if (main < MIN_MAIN_WIDTH) {
            main = MIN_MAIN_WIDTH;
            side = Math.max(60, (content - main) / (panels - 1));
        }
        this.sideWidth = side;
        this.panelWidth = main;

        int group = main + (panels - 1) * side + gaps;
        int groupLeft = Math.max(MARGIN, (this.width - group) / 2);
        this.left = groupLeft + (showCharacterPanel ? side + GAP : 0);

        this.panelHeight = Math.max(MIN_PANEL_HEIGHT, Math.min(PANEL_HEIGHT, this.height - 2 * MARGIN));
        this.top = Math.max(MARGIN, (this.height - panelHeight) / 2);
    }

    @Override
    protected void init() {
        computeLayout();

        // ---- панели ----
        if (showCharacterPanel) {
            this.characterPanel = new CharacterPanel(left - sideWidth - GAP, top, sideWidth, panelHeight);
        } else {
            this.characterPanel = null;
        }

        int weaponPanelLeft = left + panelWidth + GAP;
        if (this.weaponPanel == null) {
            this.weaponPanel = new WeaponPanel(weaponPanelLeft, top, sideWidth, panelHeight);
        } else {
            this.weaponPanel.setBounds(weaponPanelLeft, top, sideWidth, panelHeight);
        }

        ProfessionData data = ClientProfessionData.get();
        ProfessionType current = data.currentProfession();
        this.lastKnownProfession = current;
        List<ProfessionType> values = professionValues();

        if (!carouselInitialized && current != ProfessionType.NONE) {
            int idx = values.indexOf(current);
            if (idx >= 0) {
                carouselIndex = idx;
            }
            carouselInitialized = true;
        }

        int carouselY = top + 40;

        addRenderableWidget(new StyledButton(left + 16, carouselY + 16, 20, 20,
                Component.literal("<"), () -> shiftCarousel(-1)));
        addRenderableWidget(new StyledButton(left + panelWidth - 36, carouselY + 16, 20, 20,
                Component.literal(">"), () -> shiftCarousel(1)));

        ProfessionType shown = values.get(carouselIndex);

        StyledButton selectButton = new StyledButton(left + panelWidth / 2 - 45, carouselY + 92, 90, BUTTON_H,
                shown == current
                        ? Component.translatable("screen.arz_classes.current")
                        : Component.translatable("screen.arz_classes.select"),
                () -> selectProfession(shown));
        selectButton.active = shown != current;
        addRenderableWidget(selectButton);

        int rowY = buttonRowY();

        // «Руководство»: слева в нижнем ряду, напротив «Назад»; показывает руководство класса из карусели
        addRenderableWidget(new StyledButton(left + PAD, rowY, 84, BUTTON_H,
                Component.translatable("screen.arz_classes.guide"),
                () -> Minecraft.getInstance().gui.setScreen(new GuideScreen(this, shown, false))));

        // «Назад»: справа, с тем же отступом PAD, что и у полоски опыта
        addRenderableWidget(new StyledButton(left + panelWidth - PAD - 52, rowY, 52, BUTTON_H,
                Component.translatable("screen.arz_classes.back"), this::onClose));

        // «Рецепты»: внизу панели дерева навыков
        addRenderableWidget(new StyledButton(weaponPanel.x() + 8, rowY, weaponPanel.width() - 16, BUTTON_H,
                Component.translatable("screen.arz_classes.recipe_book"),
                () -> Minecraft.getInstance().gui.setScreen(new RecipeBookScreen(this))));

        // «Прокачка»: внизу панели атрибутов, на той же высоте (пока заглушка)
        if (characterPanel != null) {
            StyledButton upgradesButton = new StyledButton(characterPanel.left() + 8, rowY,
                    characterPanel.width() - 16, BUTTON_H,
                    Component.translatable("screen.arz_classes.upgrades"),
                    () -> { /* TODO: открыть экран другой прокачки */ })
                    .withTooltip(Component.translatable("screen.arz_classes.upgrades.soon"));
            upgradesButton.active = false;
            addRenderableWidget(upgradesButton);
        }
    }

    @Override
    public void tick() {
        super.tick();
        ProfessionType current = ClientProfessionData.get().currentProfession();
        if (current != lastKnownProfession) {
            rebuildWidgets();
        }
        // Только первый раз с этим классом (у этого игрока в этом мире): открываем обязательное руководство.
        // При повторной смене на тот же класс не открываем. Вернуться к нему можно кнопкой «Руководство».
        if (current != ProfessionType.NONE && !GuideProgress.isSeen(current)) {
            String key = GuideProgress.key(current);
            if (key != null && AUTO_GUIDE_SHOWN.add(key)) {
                Minecraft.getInstance().gui.setScreen(new GuideScreen(this, current, true));
            }
        }
    }

    private void shiftCarousel(int delta) {
        List<ProfessionType> values = professionValues();
        carouselIndex = Math.floorMod(carouselIndex + delta, values.size());
        this.rebuildWidgets();
    }

    private void selectProfession(ProfessionType type) {
        ProfessionData data = ClientProfessionData.get();
        ProfessionType current = data.currentProfession();

        if (current == ProfessionType.NONE || current == type) {
            ProfessionClientNetworking.requestSelectProfession(type);
            return;
        }

        Minecraft.getInstance().gui.setScreen(new StyledConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        ProfessionClientNetworking.requestSelectProfession(type);
                    }
                    Minecraft.getInstance().gui.setScreen(this);
                },
                Component.translatable("screen.arz_classes.confirm_switch_title"),
                Component.translatable("screen.arz_classes.confirm_switch_message",
                        current.displayName(), type.displayName())));
    }

    // ---------------- ввод: всё, что касается дерева, уходит в WeaponPanel ----------------

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (weaponPanel != null) {
            if (weaponPanel.isModal()) {
                // открыт диалог подтверждения подкласса — кликабелен только он
                return weaponPanel.mouseClicked(event.x(), event.y(), event.button());
            }
            if (super.mouseClicked(event, doubleClick)) {
                return true;
            }
            return weaponPanel.mouseClicked(event.x(), event.y(), event.button());
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (weaponPanel != null && weaponPanel.mouseDragged(dragY)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (weaponPanel != null) {
            weaponPanel.mouseReleased();
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (weaponPanel != null && weaponPanel.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ---------------- отрисовка ----------------

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xC0101010);
        if (characterPanel != null) {
            characterPanel.extractBackground(graphics);
        }
        if (weaponPanel != null) {
            weaponPanel.extractBackground(graphics);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        ProfessionData data = ClientProfessionData.get();
        ProfessionType current = data.currentProfession();
        List<ProfessionType> values = professionValues();
        ProfessionType shown = values.get(carouselIndex);
        int shownLevel = (shown == current) ? data.level() : 0;
        int carouselY = top + 40;

        int classColor = ProfessionColors.of(shown);

        graphics.pose().pushMatrix();
        graphics.pose().translate(left + panelWidth / 2f, top + 22f);
        graphics.pose().scale(1.6f, 1.6f);
        graphics.centeredText(this.font, shown.displayName(), 0, -4, classColor);
        graphics.pose().popMatrix();

        // статистика: сколько игроков взяли этот класс, в цвет класса
        int percent = ClientClassStats.percent(shown);
        graphics.centeredText(this.font,
                Component.translatable("screen.arz_classes.class_stats", ClientClassStats.count(shown), percent),
                left + panelWidth / 2, top + 29, classColor);

        ItemStack icon = ProfessionIcons.of(shown);
        graphics.pose().pushMatrix();
        graphics.pose().translate(left + panelWidth / 2f, carouselY + 26f);
        graphics.pose().scale(2.2f, 2.2f);
        graphics.item(icon, -8, -8);
        graphics.pose().popMatrix();

        graphics.centeredText(this.font,
                Component.translatable("screen.arz_classes.level_label", shownLevel),
                left + panelWidth / 2, carouselY + 52, 0xFFAAAAAA);

        Component desc = Component.translatable("profession.arz_classes." + shown.getSerializedName() + ".desc");
        int descY = carouselY + 66;
        for (var line : this.font.split(desc, panelWidth - 32)) {
            graphics.centeredText(this.font, line, left + panelWidth / 2, descY, 0xFFCCCCCC);
            descY += this.font.lineHeight;
        }

        if (characterPanel != null) {
            characterPanel.extractRenderState(graphics, mouseX, mouseY, this.font, data);
        }

        int rowY = buttonRowY();
        int barY = rowY - XP_BAR_H - 6; // полоска опыта — строка прямо над рядом кнопок

        if (current != ProfessionType.NONE) {
            int level = data.level();
            int xp = data.xp();
            boolean maxed = data.isMaxLevel();
            int needed = current.xpToNextLevel(level);

            float progress = maxed ? 1f : (float) xp / needed;
            drawExperienceBar(graphics, left + PAD, left + panelWidth - PAD, barY, progress, level,
                    ProfessionColors.of(current));
            xpBarCurrent = xp;
            xpBarNeeded = needed;

            if (xpBarW > 0 && mouseX >= xpBarX && mouseX <= xpBarX + xpBarW
                    && mouseY >= xpBarY && mouseY <= xpBarY + xpBarH) {
                graphics.setTooltipForNextFrame(this.font,
                        Component.translatable("screen.arz_classes.xp_tooltip", xpBarCurrent, xpBarNeeded),
                        mouseX, mouseY);
            }

            // подпись подкласса — строкой над полоской опыта (в нижнем ряду теперь кнопки)
            int labelY = barY - 12;
            if (data.needsSubclassSelection()) {
                graphics.text(this.font, Component.translatable("screen.arz_classes.subclass_required"),
                        left + PAD, labelY, 0xFFFF5555);
            } else if (data.currentSubclass() != null) {
                Component subLabel = Component.translatable("screen.arz_classes.profession_button",
                        data.currentSubclass().displayName(), data.level());
                graphics.text(this.font, subLabel, left + PAD, labelY, 0xFFCCCCCC);
            }
        } else {
            graphics.text(this.font, Component.translatable("screen.arz_classes.no_profession"),
                    left + PAD, barY, 0xFFAAAAAA);
            xpBarW = 0;
        }

        // дерево навыков рисуем последним — диалог подтверждения и тултипы ложатся поверх всего
        if (weaponPanel != null) {
            weaponPanel.render(graphics, this.font, mouseX, mouseY);
        }
    }

    /** Полоска опыта на всю строку от x0 до x1 (с уровнем слева), в стиле плашек дерева навыков. */
    private void drawExperienceBar(GuiGraphicsExtractor graphics, int x0, int x1, int y,
                                   float progress, int level, int color) {
        progress = Math.max(0f, Math.min(1f, progress));
        String levelText = String.valueOf(level);
        int barX = x0 + this.font.width(levelText) + 6;
        int barW = x1 - barX;

        graphics.text(this.font, levelText, x0, y, color);

        // тёмный фон как у плашки очков, заполнение цветом класса, рамка как у закрытых узлов
        graphics.fill(barX, y, barX + barW, y + XP_BAR_H, 0xE0201818);
        int filled = (int) (barW * progress);
        graphics.fill(barX, y, barX + filled, y + XP_BAR_H, color);
        graphics.outline(barX - 1, y - 1, barW + 2, XP_BAR_H + 2, 0xFF555555);

        // деления: все одинаковые, тонкие и тёмные
        for (int i = 1; i < XP_SEGMENTS; i++) {
            int tx = barX + Math.round(barW * (float) i / XP_SEGMENTS);
            graphics.fill(tx, y, tx + 1, y + XP_BAR_H, 0x90000000);
        }

        this.xpBarX = barX;
        this.xpBarY = y;
        this.xpBarW = barW;
        this.xpBarH = XP_BAR_H;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().gui.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}