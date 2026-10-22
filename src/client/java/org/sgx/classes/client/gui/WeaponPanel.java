package org.sgx.classes.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.sgx.classes.client.ClientProfessionData;
import org.sgx.classes.client.network.ProfessionClientNetworking;
import org.sgx.classes.client.network.SkillUnlockClientNetworking;
import org.sgx.classes.profession.ProfessionColors;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;
import org.sgx.classes.profession.SubclassType;
import org.sgx.classes.profession.skill.SkillDefinition;
import org.sgx.classes.profession.skill.SkillTrees;

import java.util.ArrayList;
import java.util.List;

/** Правая панель ClassesScreen: дерево навыков. */
public class WeaponPanel {

    private static final int NODE_SIZE = 20;
    private static final int NODE_SPACING_Y = 40;
    private static final int MAX_FORK_OFFSET_X = 36;
    /** Шапка: заголовок + плашка очков + строка подкласса. */
    private static final int VIEWPORT_TOP_PAD = 44;
    /** Низ: место под кнопку «Книга рецептов» (её добавляет ClassesScreen). */
    private static final int VIEWPORT_BOTTOM_PAD = 32;
    /** Уровень, на котором выбирается подкласс (стоит 1 очко навыка). */
    private static final int FORK_LEVEL = ProfessionData.SUBCLASS_BRANCH_LEVEL;

    private int x, y, width, height;
    private int viewportTop, viewportBottom;

    private final List<SkillDefinition> nodes = new ArrayList<>();
    private final List<SubclassType> pendingChoices = new ArrayList<>();
    private int mainChainSize;
    private int chosenSide;
    /** Выбранный подкласс: его иконка остаётся на месте развилки, ветка идёт от неё вверх. */
    private SubclassType chosenSubclass;

    private int panY = 0;
    private boolean dragging = false;

    private ProfessionType lastProfession = null;
    private SubclassType lastSubclass = null;
    private boolean builtOnce = false;

    private SubclassType pendingConfirmSubclass;

    public WeaponPanel(int x, int y, int width, int height) {
        setBounds(x, y, width, height);
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.viewportTop = y + VIEWPORT_TOP_PAD;
        this.viewportBottom = y + height - VIEWPORT_BOTTOM_PAD;
        panY = Math.max(0, Math.min(maxPanY(), panY));
    }

    public int x() { return x; }
    public int y() { return y; }
    public int width() { return width; }
    public int height() { return height; }

    /** true, пока открыт диалог подтверждения — экран не должен пускать клики к кнопкам. */
    public boolean isModal() {
        return pendingConfirmSubclass != null;
    }

    private int forkOffset() {
        return Math.min(MAX_FORK_OFFSET_X, Math.max(12, (width - NODE_SIZE - 8) / 2));
    }

    // ---------------- данные ----------------

    private void rebuildNodes(ProfessionData data) {
        nodes.clear();
        pendingChoices.clear();
        chosenSide = 0;
        chosenSubclass = null;
        mainChainSize = 0;
        ProfessionType current = data.currentProfession();
        if (current != ProfessionType.NONE) {
            List<SkillDefinition> main = SkillTrees.mainChain(current);
            nodes.addAll(main);
            mainChainSize = main.size();
            SubclassType chosen = data.currentSubclass();
            if (chosen != null) {
                nodes.addAll(SkillTrees.branchChain(chosen));
                chosenSide = subclassSide(current, chosen);
                chosenSubclass = chosen;
            } else {
                for (SubclassType sub : SubclassType.values()) {
                    if (sub.parent() == current) {
                        pendingChoices.add(sub);
                    }
                }
            }
        }
        lastProfession = current;
        lastSubclass = data.currentSubclass();
        builtOnce = true;
    }

    private void refreshIfNeeded(ProfessionData data) {
        if (!builtOnce || data.currentProfession() != lastProfession || data.currentSubclass() != lastSubclass) {
            rebuildNodes(data);
            int anchor = Math.max(0, data.unlockedSkillCount() - 1);
            int middleY = viewportTop + (viewportBottom - viewportTop) / 2 - NODE_SIZE / 2;
            panY = middleY - baseYAtBottom(rowOf(anchor));
            panY = Math.max(0, Math.min(maxPanY(), panY));
            pendingConfirmSubclass = null;
        }
    }

    private int subclassSide(ProfessionType current, SubclassType chosen) {
        int idx = 0;
        for (SubclassType sub : SubclassType.values()) {
            if (sub.parent() == current) {
                if (sub == chosen) {
                    return idx == 0 ? -1 : 1;
                }
                idx++;
            }
        }
        return 0;
    }

    /** Можно ли прямо сейчас выбрать подкласс: открыта блокировка, 10 уровень, вся главная цепочка куплена, есть очко. */
    private boolean canChooseSubclassNow(ProfessionData data) {
        return !pendingChoices.isEmpty()
                && data.subclassUnlocked()
                && data.level() >= FORK_LEVEL
                && data.unlockedSkillCount() >= nodes.size()
                && data.availableSkillPoints() > 0;
    }

    // ---------------- геометрия ----------------

    private int baseYAtBottom(int row) {
        return viewportBottom - NODE_SIZE - row * NODE_SPACING_Y;
    }

    /** Строка (ряд) узла на экране: после выбора подкласса один ряд между главной цепочкой и веткой занимает иконка подкласса. */
    private int rowOf(int index) {
        return (chosenSubclass != null && index >= mainChainSize) ? index + 1 : index;
    }

    private int nodeX(int index) {
        int base = x + width / 2 - NODE_SIZE / 2;
        if (index >= mainChainSize) {
            base += chosenSide * forkOffset();
        }
        return base;
    }

    private int nodeY(int index) {
        return baseYAtBottom(rowOf(index)) + panY;
    }

    private int forkNodeX(int choiceIndex) {
        int offset = (choiceIndex == 0) ? -forkOffset() : forkOffset();
        return x + width / 2 + offset - NODE_SIZE / 2;
    }

    /** Позиция иконки выбранного подкласса — ровно там, где она была на развилке. */
    private int markerX() {
        return x + width / 2 - NODE_SIZE / 2 + chosenSide * forkOffset();
    }

    private int markerY() {
        return baseYAtBottom(mainChainSize) + panY;
    }

    private int maxPanY() {
        boolean extraRow = chosenSubclass != null || !pendingChoices.isEmpty();
        int topRow = nodes.isEmpty() ? 0 : rowOf(nodes.size() - 1);
        if (extraRow && chosenSubclass == null) {
            topRow = nodes.size(); // ряд развилки над последним узлом
        }
        if (nodes.isEmpty() && chosenSubclass != null) {
            topRow = 0;
        }
        return Math.max(0, viewportTop - baseYAtBottom(topRow));
    }

    private boolean inViewport(double mx, double my) {
        return mx >= x && mx <= x + width && my >= viewportTop && my <= viewportBottom;
    }

    // ---------------- ввод ----------------

    /** @return true, если панель обработала клик. */
    public boolean mouseClicked(double mx, double my, int button) {
        if (pendingConfirmSubclass != null) {
            if (button == 0) {
                handleDialogClick(mx, my);
            }
            return true; // диалог модальный
        }
        if (button != 0 || mx < x || mx > x + width || my < y || my > y + height) {
            return false;
        }
        if (tryUnlockClick(mx, my)) {
            return true;
        }
        if (tryChooseSubclassClick(mx, my)) {
            return true;
        }
        if (inViewport(mx, my)) {
            dragging = true;
            return true;
        }
        return false;
    }

    public boolean mouseDragged(double dragY) {
        if (dragging && pendingConfirmSubclass == null) {
            panY += (int) dragY;
            panY = Math.max(0, Math.min(maxPanY(), panY));
            return true;
        }
        return false;
    }

    public boolean mouseReleased() {
        boolean was = dragging;
        dragging = false;
        return was;
    }

    public boolean mouseScrolled(double mx, double my, double scrollY) {
        if (pendingConfirmSubclass != null) {
            return true;
        }
        if (inViewport(mx, my)) {
            panY += (int) (scrollY * NODE_SPACING_Y / 2);
            panY = Math.max(0, Math.min(maxPanY(), panY));
            return true;
        }
        return false;
    }

    private boolean tryUnlockClick(double mx, double my) {
        ProfessionData data = ClientProfessionData.get();
        int unlockedCount = data.unlockedSkillCount();
        if (unlockedCount >= nodes.size() || data.availableSkillPoints() <= 0) {
            return false;
        }
        SkillDefinition next = nodes.get(unlockedCount);
        if (data.level() < next.requiredLevel()) {
            return false;
        }
        int nx = nodeX(unlockedCount);
        int ny = nodeY(unlockedCount);
        if (mx >= nx && mx <= nx + NODE_SIZE && my >= ny && my <= ny + NODE_SIZE && inViewport(mx, my)) {
            SkillUnlockClientNetworking.requestUnlockNext();
            return true;
        }
        return false;
    }

    private boolean tryChooseSubclassClick(double mx, double my) {
        ProfessionData data = ClientProfessionData.get();
        if (!canChooseSubclassNow(data)) {
            return false;
        }
        int ny = nodeY(nodes.size());
        if (ny < viewportTop || ny > viewportBottom) {
            return false;
        }
        for (int i = 0; i < pendingChoices.size(); i++) {
            int nx = forkNodeX(i);
            if (mx >= nx && mx <= nx + NODE_SIZE && my >= ny && my <= ny + NODE_SIZE) {
                pendingConfirmSubclass = pendingChoices.get(i);
                return true;
            }
        }
        return false;
    }

    // ---------------- диалог подтверждения ----------------

    private int boxW() { return width - 12; }
    private int boxH() { return 110; }
    private int boxX() { return x + 6; }
    private int boxY() { return y + height / 2 - boxH() / 2; }
    private int dlgBtnW() { return (boxW() - 18) / 2; }
    private int dlgBtnY() { return boxY() + boxH() - 24; }
    private int confirmBtnX() { return boxX() + 6; }
    private int cancelBtnX() { return boxX() + 12 + dlgBtnW(); }

    private boolean over(double mx, double my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
    }

    private void handleDialogClick(double mx, double my) {
        if (over(mx, my, confirmBtnX(), dlgBtnY(), dlgBtnW(), 18)) {
            ProfessionClientNetworking.requestSelectSubclass(pendingConfirmSubclass);
            pendingConfirmSubclass = null;
        } else if (over(mx, my, cancelBtnX(), dlgBtnY(), dlgBtnW(), 18)) {
            pendingConfirmSubclass = null;
        }
    }

    private void drawDialogButton(GuiGraphicsExtractor g, Font font, int bx, int by, int bw, Component label,
                                  int mx, int my) {
        boolean hover = over(mx, my, bx, by, bw, 18);
        g.fill(bx, by, bx + bw, by + 18, hover ? 0xFF3A3620 : 0xFF1A1A1A);
        g.outline(bx, by, bw, 18, hover ? 0xFFFFD46B : 0xFF777777);
        g.centeredText(font, label, bx + bw / 2, by + 5, 0xFFFFFFFF);
    }

    private void drawDialog(GuiGraphicsExtractor g, Font font, int mx, int my) {
        g.fill(x, y, x + width, y + height, 0xB0000000);
        int bx = boxX(), by = boxY(), bw = boxW(), bh = boxH();
        g.fill(bx, by, bx + bw, by + bh, 0xF0201818);
        g.outline(bx, by, bw, bh, 0xFFFFD46B);

        g.centeredText(font, Component.translatable("screen.arz_classes.confirm_subclass_title"),
                bx + bw / 2, by + 6, 0xFFFFD46B);
        g.textWithWordWrap(font,
                Component.translatable("screen.arz_classes.confirm_subclass_body", pendingConfirmSubclass.displayName()),
                bx + 6, by + 20, bw - 12, 0xFFFFFFFF);
        g.textWithWordWrap(font, Component.translatable("screen.arz_classes.confirm_subclass_warning"),
                bx + 6, by + 44, bw - 12, 0xFFFF5555);

        drawDialogButton(g, font, confirmBtnX(), dlgBtnY(), dlgBtnW(),
                Component.translatable("screen.arz_classes.confirm"), mx, my);
        drawDialogButton(g, font, cancelBtnX(), dlgBtnY(), dlgBtnW(),
                Component.translatable("screen.arz_classes.cancel"), mx, my);
    }

    // ---------------- отрисовка ----------------

    /** Фон панели — вызывать из ClassesScreen.extractBackground. */
    public void extractBackground(GuiGraphicsExtractor g) {
        BookStyle.panel(g, x, y, width, height);
    }

    public void render(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY) {
        ProfessionData data = ClientProfessionData.get();
        ProfessionType current = data.currentProfession();
        refreshIfNeeded(data);

        g.text(font, Component.translatable("screen.arz_classes.skill_tree"), x + 8, y + 6, BookStyle.body(0xFFFFFFFF));

        if (current == ProfessionType.NONE) {
            g.textWithWordWrap(font, Component.translatable("screen.arz_classes.no_profession"),
                    x + 8, y + 24, width - 16, 0xFFAAAAAA);
            return;
        }

        // плашка очков навыков
        int badgeX = x + 6, badgeY = y + 18, badgeW = width - 12, badgeH = 14;
        g.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, BookStyle.well(0xE0201818));
        g.outline(badgeX, badgeY, badgeW, badgeH,
                BookStyle.enabled() ? BookStyle.theme().gold : 0xFFFFD46B);
        g.centeredText(font, Component.translatable("screen.arz_classes.skill_points", data.availableSkillPoints()),
                badgeX + badgeW / 2, badgeY + 3, BookStyle.title(0xFFFFD46B));

        // выбранный подкласс
        if (data.currentSubclass() != null) {
            SubclassType sub = data.currentSubclass();
            int subY = badgeY + badgeH + 4;
            g.blitSprite(RenderPipelines.GUI_TEXTURED, SkillIcons.forSubclassChoice(sub), badgeX, subY, 10, 10);
            g.text(font, sub.displayName(), badgeX + 13, subY + 1, ProfessionColors.of(current));
        }

        int unlockedCount = data.unlockedSkillCount();
        boolean forkVisible = !pendingChoices.isEmpty();
        int unlockedColor = ProfessionColors.of(current);

        boolean modal = pendingConfirmSubclass != null;
        try {
            g.enableScissor(x, viewportTop, x + width, viewportBottom);
            renderNodesAndConnectors(g, font, modal ? -1000 : mouseX, modal ? -1000 : mouseY,
                    data, unlockedCount, nodes.size() - 1, unlockedColor, forkVisible);
        } catch (Throwable t) {
            g.text(font, Component.literal("§c" + t.getClass().getSimpleName()), x + 8, viewportTop + 10, 0xFFFF5555);
            t.printStackTrace();
        } finally {
            g.disableScissor();
        }

        if (modal) {
            drawDialog(g, font, mouseX, mouseY);
        }
    }

    private void renderNodesAndConnectors(GuiGraphicsExtractor g, Font font, int mouseX, int mouseY,
                                          ProfessionData data, int unlockedCount, int maxVisibleIndex,
                                          int unlockedColor, boolean forkVisible) {
        boolean hasMarker = chosenSubclass != null;

        for (int i = 0; i < maxVisibleIndex; i++) {
            if (hasMarker && i + 1 == mainChainSize) {
                continue; // это стык главной цепочки и ветки — рисуем через иконку подкласса ниже
            }
            int x1 = nodeX(i) + NODE_SIZE / 2;
            int y1 = nodeY(i) + NODE_SIZE / 2;
            int x2 = nodeX(i + 1) + NODE_SIZE / 2;
            int y2 = nodeY(i + 1) + NODE_SIZE / 2;
            drawConnector(g, x1, y1, x2, y2, (i + 1 < unlockedCount) ? unlockedColor : 0xFF3A3A3A);
        }

        if (hasMarker && mainChainSize > 0) {
            // главная цепочка -> иконка подкласса (диагональный уход в сторону)
            int mainLastX = nodeX(mainChainSize - 1) + NODE_SIZE / 2;
            int mainLastY = nodeY(mainChainSize - 1) + NODE_SIZE / 2;
            int mx = markerX() + NODE_SIZE / 2;
            int my = markerY() + NODE_SIZE / 2;
            drawConnector(g, mainLastX, mainLastY, mx, my, unlockedColor);
            // иконка подкласса -> первый навык ветки
            if (nodes.size() > mainChainSize) {
                int bx = nodeX(mainChainSize) + NODE_SIZE / 2;
                int by = nodeY(mainChainSize) + NODE_SIZE / 2;
                drawConnector(g, mx, my, bx, by, (mainChainSize < unlockedCount) ? unlockedColor : 0xFF3A3A3A);
            }
        }

        if (forkVisible && !nodes.isEmpty()) {
            int lastX = nodeX(nodes.size() - 1) + NODE_SIZE / 2;
            int lastY = nodeY(nodes.size() - 1) + NODE_SIZE / 2;
            int forkY = nodeY(nodes.size()) + NODE_SIZE / 2;
            g.fill(lastX - 1, forkY, lastX + 1, lastY, 0xFF3A3A3A);
            g.fill(x + width / 2 - forkOffset(), forkY - 1, x + width / 2 + forkOffset(), forkY + 1, 0xFF3A3A3A);
        }

        for (int i = 0; i <= maxVisibleIndex; i++) {
            SkillDefinition skill = nodes.get(i);
            int nx = nodeX(i);
            int ny = nodeY(i);
            if (ny + NODE_SIZE < viewportTop || ny > viewportBottom) {
                continue;
            }

            boolean unlocked = i < unlockedCount;
            boolean purchasable = !unlocked && i == unlockedCount
                    && data.level() >= skill.requiredLevel() && data.availableSkillPoints() > 0;

            int borderColor = unlocked ? unlockedColor : (purchasable ? 0xFFFFD46B : 0xFF555555);
            int bgColor = unlocked ? 0xFF2B2B2B : (purchasable ? 0xFF3A3620 : 0xFF1A1A1A);

            g.fill(nx, ny, nx + NODE_SIZE, ny + NODE_SIZE, bgColor);
            g.outline(nx, ny, NODE_SIZE, NODE_SIZE, borderColor);
            g.blitSprite(RenderPipelines.GUI_TEXTURED, SkillIcons.of(skill), nx + 2, ny + 2,
                    NODE_SIZE - 4, NODE_SIZE - 4);

            Identifier overlay = SkillIcons.overlayOf(skill);
            if (overlay != null) {
                int size = 8;
                int ox = nx + NODE_SIZE - 2 - size;
                int oy = ny + NODE_SIZE - 2 - size;
                g.fill(ox - 1, oy - 1, ox + size + 1, oy + size + 1, bgColor);
                g.blitSprite(RenderPipelines.GUI_TEXTURED, overlay, ox, oy, size, size);
            }

            if (mouseX >= nx && mouseX <= nx + NODE_SIZE && mouseY >= ny && mouseY <= ny + NODE_SIZE
                    && mouseY >= viewportTop && mouseY <= viewportBottom) {
                List<FormattedCharSequence> tooltip = new ArrayList<>(List.of(
                        Component.translatable(skill.nameKey())
                                .withStyle(s -> s.withBold(true).withColor(unlockedColor & 0xFFFFFF))
                                .getVisualOrderText(),
                        Component.translatable(skill.descKey()).getVisualOrderText(),
                        Component.translatable("screen.arz_classes.level_label", skill.requiredLevel())
                                .withStyle(ChatFormatting.GRAY).getVisualOrderText()
                ));
                if (purchasable) {
                    tooltip.add(Component.translatable("screen.arz_classes.click_to_unlock").getVisualOrderText());
                }
                g.setTooltipForNextFrame(font, tooltip, mouseX, mouseY);
            }
        }

        // выбранный подкласс остаётся на месте развилки как узел, от которого идёт ветка
        if (hasMarker) {
            int nx = markerX();
            int ny = markerY();
            if (!(ny + NODE_SIZE < viewportTop || ny > viewportBottom)) {
                g.fill(nx, ny, nx + NODE_SIZE, ny + NODE_SIZE, 0xFF2B2B2B);
                g.outline(nx, ny, NODE_SIZE, NODE_SIZE, unlockedColor);
                g.blitSprite(RenderPipelines.GUI_TEXTURED, SkillIcons.forSubclassChoice(chosenSubclass),
                        nx + 2, ny + 2, NODE_SIZE - 4, NODE_SIZE - 4);

                if (mouseX >= nx && mouseX <= nx + NODE_SIZE && mouseY >= ny && mouseY <= ny + NODE_SIZE
                        && mouseY >= viewportTop && mouseY <= viewportBottom) {
                    List<FormattedCharSequence> tip = new ArrayList<>();
                    tip.add(chosenSubclass.displayName().copy()
                            .withStyle(s -> s.withBold(true).withColor(unlockedColor & 0xFFFFFF))
                            .getVisualOrderText());
                    tip.add(Component.translatable("screen.arz_classes.subclass_preview."
                            + chosenSubclass.name().toLowerCase()).getVisualOrderText());
                    tip.add(Component.translatable("screen.arz_classes.level_label", FORK_LEVEL)
                            .withStyle(ChatFormatting.GRAY).getVisualOrderText());
                    g.setTooltipForNextFrame(font, tip, mouseX, mouseY);
                }
            }
        }

        if (forkVisible) {
            boolean lockedByEvent = !data.subclassUnlocked();
            boolean clickable = canChooseSubclassNow(data);
            int ny = nodeY(nodes.size());
            for (int i = 0; i < pendingChoices.size(); i++) {
                int nx = forkNodeX(i);
                if (ny + NODE_SIZE < viewportTop || ny > viewportBottom) {
                    continue;
                }

                g.fill(nx, ny, nx + NODE_SIZE, ny + NODE_SIZE, clickable ? 0xFF3A3620 : 0xFF1A1A1A);
                g.outline(nx, ny, NODE_SIZE, NODE_SIZE, clickable ? 0xFFFFD46B : 0xFF555555);
                g.blitSprite(RenderPipelines.GUI_TEXTURED, SkillIcons.forSubclassChoice(pendingChoices.get(i)),
                        nx + 2, ny + 2, NODE_SIZE - 4, NODE_SIZE - 4);
                if (lockedByEvent) {
                    g.fill(nx + 2, ny + 2, nx + NODE_SIZE - 2, ny + NODE_SIZE - 2, 0xB0101010);
                }

                Component name = pendingChoices.get(i).displayName();

                if (mouseX >= nx && mouseX <= nx + NODE_SIZE && mouseY >= ny && mouseY <= ny + NODE_SIZE
                        && mouseY >= viewportTop && mouseY <= viewportBottom) {
                    List<FormattedCharSequence> tip = new ArrayList<>();
                    tip.add(name.copy()
                            .withStyle(s -> s.withBold(true).withColor(unlockedColor & 0xFFFFFF))
                            .getVisualOrderText());
                    if (lockedByEvent) {
                        tip.add(Component.translatable("screen.arz_classes.subclass_locked")
                                .withStyle(ChatFormatting.RED).getVisualOrderText());
                    } else {
                        tip.add(Component.translatable("screen.arz_classes.subclass_preview."
                                + pendingChoices.get(i).name().toLowerCase()).getVisualOrderText());
                        tip.add(Component.translatable("screen.arz_classes.level_label", FORK_LEVEL)
                                .withStyle(ChatFormatting.GRAY).getVisualOrderText());
                        if (clickable) {
                            tip.add(Component.translatable("screen.arz_classes.click_to_unlock").getVisualOrderText());
                        }
                    }
                    g.setTooltipForNextFrame(font, tip, mouseX, mouseY);
                }
            }
        }
    }

    private void drawConnector(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int color) {
        if (x1 == x2) {
            g.fill(x1 - 1, Math.min(y1, y2), x1 + 1, Math.max(y1, y2), color);
            return;
        }
        int midY = (y1 + y2) / 2;
        g.fill(x1 - 1, Math.min(y1, midY), x1 + 1, Math.max(y1, midY), color);
        g.fill(Math.min(x1, x2), midY - 1, Math.max(x1, x2), midY + 1, color);
        g.fill(x2 - 1, Math.min(midY, y2), x2 + 1, Math.max(midY, y2), color);
    }
}