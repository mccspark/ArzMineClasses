package org.sgx.classes.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.block.CarpenterRecipe;
import org.sgx.classes.block.CarpenterWorkbenchMenu;

/**
 * Копия FactoryScreen (money) под верстак плотника: тот же интерфейс и взаимодействие.
 * Отличия только в данных: рецепты CarpenterRecipe, проверка крафта через menu.canCraft().
 * Выпадающий список рецептов прокручивается колесом мыши (и кликом по полоске справа).
 * Shift+клик по результату — крафт на сколько хватит ресурсов (до стака).
 */
public class CarpenterWorkbenchScreen extends AbstractContainerScreen<CarpenterWorkbenchMenu> {

    // Должны совпадать с размером PNG и с раскладкой слотов в CarpenterWorkbenchMenu (окно 203).
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 203;
    private static final Identifier TEXTURE = Arz_classes.id("textures/gui/carpenter_workbench.png");
    private static final Identifier SELECTION_BAR = Arz_classes.id("textures/gui/selection_bar.png");

    private static final int BAR_X = 7;
    private static final int BAR_Y = 30;
    private static final int BAR_WIDTH = 162;
    private static final int DROPDOWN_X = BAR_X + 1;
    private static final int DROPDOWN_WIDTH = BAR_WIDTH - 2;
    private static final int BAR_HEIGHT = 18;

    /** Сколько строк списка видно одновременно (остальные — прокруткой). */
    private static final int VISIBLE_ROWS = 7;
    private static final int SCROLLBAR_W = 4;

    private static final int INGREDIENTS_Y = 16;

    private static final int OUTPUT_X = 99;
    private static final int OUTPUT_Y = 74;
    private static final int SLOT_SIZE = 16;
    /** Иконка результата рисуется на 1 пиксель выше и левее области клика. */
    private static final int OUTPUT_DRAW_OFFSET = -1;

    private boolean dropdownOpen = false;
    private int scroll = 0;

    public CarpenterWorkbenchScreen(CarpenterWorkbenchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        this.titleLabelY -= 1;
    }

    private static int total() {
        return CarpenterRecipe.Registry.ALL.size();
    }

    private static int visibleRows() {
        return Math.min(total(), VISIBLE_ROWS);
    }

    private static int maxScroll() {
        return Math.max(0, total() - VISIBLE_ROWS);
    }

    private void setScroll(int value) {
        this.scroll = Math.max(0, Math.min(value, maxScroll()));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTicks) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTicks);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                this.leftPos, this.topPos, 0, 0,
                this.imageWidth, this.imageHeight, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        renderSelectionBar(guiGraphics, mouseX, mouseY);
        renderIngredients(guiGraphics, mouseX, mouseY);
        renderOutput(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
        guiGraphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY + 1, 0xFF404040, false);
    }

    @Override
    protected void extractSlots(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractSlots(guiGraphics, mouseX, mouseY);
        if (this.dropdownOpen) {
            renderDropdown(guiGraphics, mouseX, mouseY);
        }
    }

    private void renderSelectionBar(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SELECTION_BAR,
                this.leftPos + BAR_X, this.topPos + BAR_Y, 0, 0,
                BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, BAR_HEIGHT);

        CarpenterRecipe recipe = this.menu.getSelectedRecipe();
        if (recipe == null) {
            String hint = Component.translatable("gui.arz_classes.carpenter.select_recipe").getString();
            guiGraphics.text(this.font, hint,
                    this.leftPos + BAR_X + (BAR_WIDTH - this.font.width(hint)) / 2,
                    this.topPos + BAR_Y + 5, 0xFFFFFFFF);
            return;
        }

        guiGraphics.fakeItem(recipe.result(), this.leftPos + BAR_X + 1, this.topPos + BAR_Y + 1);
        guiGraphics.text(this.font, recipe.result().getHoverName().getString(),
                this.leftPos + BAR_X + 21, this.topPos + BAR_Y + 5, 0xFFFFFFFF);
        if (isHovering(BAR_X + 1, BAR_Y + 1, SLOT_SIZE, SLOT_SIZE, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(this.font, recipe.result(), mouseX, mouseY);
        }
    }

    private void renderIngredients(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        CarpenterRecipe recipe = this.menu.getSelectedRecipe();
        if (recipe == null) {
            return;
        }

        int x = this.leftPos + 8;
        for (CarpenterRecipe.Input ingredient : recipe.inputs()) {
            ItemStack icon = ingredient.display();
            guiGraphics.pose().pushMatrix();
            guiGraphics.pose().translate(x, this.topPos + INGREDIENTS_Y);
            guiGraphics.pose().scale(0.5f);
            guiGraphics.fakeItem(icon, 0, 0);
            guiGraphics.pose().popMatrix();

            String label = "x" + ingredient.count();
            guiGraphics.text(this.font, label, x + 10, this.topPos + INGREDIENTS_Y + 1, 0xFFFFFFFF);
            if (isHovering(x - this.leftPos, INGREDIENTS_Y, 8, 8, mouseX, mouseY)) {
                guiGraphics.setTooltipForNextFrame(this.font, icon, mouseX, mouseY);
            }
            x += 10 + this.font.width(label) + 6;
        }
    }

    private void renderOutput(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        CarpenterRecipe recipe = this.menu.getSelectedRecipe();
        if (recipe == null || !this.menu.canCraft(recipe)) {
            return;
        }

        ItemStack result = recipe.result();
        int drawX = this.leftPos + OUTPUT_X + OUTPUT_DRAW_OFFSET;
        int drawY = this.topPos + OUTPUT_Y + OUTPUT_DRAW_OFFSET;
        guiGraphics.fakeItem(result, drawX, drawY);
        guiGraphics.itemDecorations(this.font, result, drawX, drawY);
        if (isHovering(OUTPUT_X, OUTPUT_Y, SLOT_SIZE, SLOT_SIZE, mouseX, mouseY)) {
            guiGraphics.setTooltipForNextFrame(this.font, result, mouseX, mouseY);
        }
    }

    private void renderDropdown(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int x = DROPDOWN_X;
        int y = BAR_Y + BAR_HEIGHT;
        int rows = visibleRows();
        int height = rows * BAR_HEIGHT;
        boolean scrollable = total() > VISIBLE_ROWS;

        guiGraphics.fill(x - 1, y - 1, x + DROPDOWN_WIDTH + 1, y + height + 1, 0xFF000000);
        guiGraphics.fill(x, y, x + DROPDOWN_WIDTH, y + height, 0xE0202020);

        for (int row = 0; row < rows; row++) {
            int i = this.scroll + row;
            CarpenterRecipe recipe = CarpenterRecipe.Registry.ALL.get(i);
            int rowY = y + row * BAR_HEIGHT;
            if (mouseX - this.leftPos >= x && mouseX - this.leftPos < x + DROPDOWN_WIDTH - (scrollable ? SCROLLBAR_W + 2 : 0)
                    && mouseY - this.topPos >= rowY && mouseY - this.topPos < rowY + BAR_HEIGHT) {
                guiGraphics.fill(x, rowY, x + DROPDOWN_WIDTH - (scrollable ? SCROLLBAR_W + 2 : 0), rowY + BAR_HEIGHT, 0x40FFFFFF);
            }
            guiGraphics.fakeItem(recipe.result(), x, rowY + 1);
            guiGraphics.text(this.font, recipe.result().getHoverName().getString(),
                    x + 20, rowY + 5, 0xFFFFFFFF);
        }

        if (scrollable) {
            int barX = x + DROPDOWN_WIDTH - SCROLLBAR_W - 1;
            int thumbH = Math.max(10, height * rows / total());
            int thumbY = y + (height - thumbH) * this.scroll / maxScroll();
            guiGraphics.fill(barX, y, barX + SCROLLBAR_W, y + height, 0xFF101010);
            guiGraphics.fill(barX, thumbY, barX + SCROLLBAR_W, thumbY + thumbH, 0xFFAAAAAA);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.dropdownOpen && maxScroll() > 0) {
            setScroll(this.scroll - (int) Math.signum(scrollY));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        double localX = event.x() - this.leftPos;
        double localY = event.y() - this.topPos;

        if (this.dropdownOpen) {
            int listY = BAR_Y + BAR_HEIGHT;
            int listH = visibleRows() * BAR_HEIGHT;
            if (localX >= DROPDOWN_X && localX < DROPDOWN_X + DROPDOWN_WIDTH
                    && localY >= listY && localY < listY + listH) {
                // клик по полоске прокрутки — перемещаем список, не закрываем
                if (maxScroll() > 0 && localX >= DROPDOWN_X + DROPDOWN_WIDTH - SCROLLBAR_W - 3) {
                    double ratio = (localY - listY) / listH;
                    setScroll((int) Math.round(ratio * maxScroll()));
                    return true;
                }
                int recipeIndex = this.scroll + (int) ((localY - listY) / BAR_HEIGHT);
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1 + recipeIndex);
            }
            this.dropdownOpen = false;
            return true;
        }

        if (localX >= BAR_X && localX < BAR_X + BAR_WIDTH && localY >= BAR_Y && localY < BAR_Y + BAR_HEIGHT) {
            this.dropdownOpen = !this.dropdownOpen;
            if (this.dropdownOpen) {
                // открываем так, чтобы выбранный рецепт был виден
                int selected = CarpenterRecipe.Registry.ALL.indexOf(this.menu.getSelectedRecipe());
                setScroll(selected >= 0 ? selected - VISIBLE_ROWS / 2 : 0);
            }
            return true;
        }

        if (localX >= OUTPUT_X && localX < OUTPUT_X + SLOT_SIZE
                && localY >= OUTPUT_Y && localY < OUTPUT_Y + SLOT_SIZE) {
            CarpenterRecipe recipe = this.menu.getSelectedRecipe();
            if (recipe != null && this.menu.canCraft(recipe)) {
                int button = event.hasShiftDown()
                        ? CarpenterWorkbenchMenu.BUTTON_CRAFT_MAX
                        : CarpenterWorkbenchMenu.BUTTON_CRAFT;
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, button);
            }
            return true;
        }

        return super.mouseClicked(event, doubled);
    }

    @Override
    public void removed() {
        this.dropdownOpen = false;
        super.removed();
    }
}