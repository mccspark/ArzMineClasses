package org.sgx.classes.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.client.ClientProfessionData;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;

import java.util.List;
import java.util.stream.Collectors;

public final class RecipeBookScreen extends Screen {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 240;
    private static final int LIST_WIDTH = 110;
    private static final int ROW_HEIGHT = 22;

    private final Screen parent;
    private int left, top;
    private int selected = 0;
    private List<ModRecipeBook.Entry> entries = List.of();

    public RecipeBookScreen(Screen parent) {
        super(Component.translatable("screen.arz_classes.recipe_book"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.left = (this.width - PANEL_WIDTH) / 2;
        this.top = (this.height - PANEL_HEIGHT) / 2;
        this.entries = visibleEntries();
        this.selected = 0;
        addRenderableWidget(new StyledButton(
                left + PANEL_WIDTH - 62, top + PANEL_HEIGHT - 26, 52, 18,
                Component.translatable("screen.arz_classes.back"),
                this::onClose));
    }

    /** profession == null -> виден всем; иначе виден только игроку этого класса.
     * Чужие рецепты сюда вообще не попадают (не путать с unlocked() — та лишь
     * блокирует крафт визуально, но запись всё равно в списке). */
    private static List<ModRecipeBook.Entry> visibleEntries() {
        ProfessionData data = ClientProfessionData.get();
        ProfessionType current = data.currentProfession();
        return ModRecipeBook.ENTRIES.stream()
                .filter(e -> e.profession() == null || e.profession() == current)
                .collect(Collectors.toList());
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        double mouseX = event.x();
        double mouseY = event.y();
        int listTop = top + 24;
        for (int i = 0; i < entries.size(); i++) {
            int rowY = listTop + i * ROW_HEIGHT;
            if (mouseX >= left + 8 && mouseX <= left + 8 + LIST_WIDTH
                    && mouseY >= rowY && mouseY <= rowY + ROW_HEIGHT) {
                selected = i;
                return true;
            }
        }
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xE0101010);
        graphics.outline(left, top, PANEL_WIDTH, PANEL_HEIGHT, 0xFFFFD46B);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractRenderState(graphics, mouseX, mouseY, a);

        graphics.centeredText(this.font, this.title, left + PANEL_WIDTH / 2, top + 6, 0xFFFFD46B);

        if (entries.isEmpty()) {
            graphics.centeredText(this.font,
                    Component.translatable("screen.arz_classes.recipe_book_empty"),
                    left + PANEL_WIDTH / 2, top + PANEL_HEIGHT / 2, 0xFF888888);
            return;
        }

        ProfessionData data = ClientProfessionData.get();
        int listTop = top + 24;

        for (int i = 0; i < entries.size(); i++) {
            ModRecipeBook.Entry entry = entries.get(i);
            boolean unlocked = entry.unlocked().test(data);
            int rowY = listTop + i * ROW_HEIGHT;
            int bg = i == selected ? 0xFF3A3620 : 0xFF201818;
            graphics.fill(left + 8, rowY, left + 8 + LIST_WIDTH, rowY + ROW_HEIGHT - 2, bg);
            graphics.outline(left + 8, rowY, LIST_WIDTH, ROW_HEIGHT - 2, i == selected ? 0xFFFFD46B : 0xFF3A3A3A);
            int textColor = unlocked ? 0xFFFFFFFF : 0xFF777777;
            graphics.text(this.font, entry.name(), left + 12, rowY + 6, textColor);
        }

        ModRecipeBook.Entry current = entries.get(Math.min(selected, entries.size() - 1));
        boolean unlocked = current.unlocked().test(data);
        int gridLeft = left + LIST_WIDTH + 30;
        int gridTop = top + 40;
        int cell = 18;

        ItemStack hovered = ItemStack.EMPTY;
        for (int i = 0; i < 9; i++) {
            int col = i % 3;
            int row = i / 3;
            int x = gridLeft + col * cell;
            int y = gridTop + row * cell;
            graphics.fill(x, y, x + 16, y + 16, 0xFF373737);
            graphics.outline(x, y, 16, 16, 0xFF000000);
            ItemStack stack = current.grid()[i];
            if (!stack.isEmpty()) {
                graphics.item(stack, x, y);
                if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                    hovered = stack;
                }
            }
        }

        int arrowX = gridLeft + cell * 3 + 6;
        int resultX = arrowX + 20;
        int resultY = gridTop + cell;
        graphics.text(this.font, "->", arrowX, resultY + 4, 0xFFFFFFFF);

        if (unlocked) {
            graphics.item(current.result(), resultX, resultY);
            if (!current.result().isEmpty()
                    && mouseX >= resultX && mouseX < resultX + 16 && mouseY >= resultY && mouseY < resultY + 16) {
                hovered = current.result();
            }
        } else {
            graphics.fill(resultX, resultY, resultX + 16, resultY + 16, 0x80000000);
            graphics.outline(resultX, resultY, 16, 16, 0xFFAA5555);
        }

        // подсказка: что за предмет под курсором (ингредиент или результат)
        if (!hovered.isEmpty()) {
            graphics.setTooltipForNextFrame(this.font, hovered, mouseX, mouseY);
        }

        if (!unlocked) {
            graphics.textWithWordWrap(this.font, current.lockedHint(),
                    gridLeft, gridTop + cell * 3 + 14, PANEL_WIDTH - LIST_WIDTH - 50, 0xFFFF9999);
        }
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