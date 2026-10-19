package org.sgx.classes.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.block.RarityUpgradeTableBlockEntity;
import org.sgx.classes.block.RarityUpgradeTableMenu;
import org.sgx.classes.item.ItemRarity;
import org.sgx.classes.network.StartRarityUpgradePayload;
import org.sgx.classes.profession.RarityUpgradeHandler;

public final class RarityUpgradeTableScreen extends AbstractContainerScreen<RarityUpgradeTableMenu> {

    private static final Identifier TEXTURE = Arz_classes.id("textures/gui/container/rarity_upgrade_table.png");
    private static final Identifier BUTTON_TEXTURE = Arz_classes.id("textures/gui/container/upgrade_button.png");
    private static final Identifier SHADOW_LEFT = Arz_classes.id("textures/gui/container/upgrade_table_shadow_left.png");
    private static final Identifier SHADOW_RIGHT = Arz_classes.id("textures/gui/container/upgrade_table_shadow_right.png");

    private static final int BUTTON_SIZE = 16;
    private static final int BUTTON_CENTER_X = 88;
    private static final int BUTTON_CENTER_Y = 43;
    /** Радиус круглой кнопки (и зоны наведения). Если круг в текстуре меньше 16 px — уменьши. */
    private static final double BUTTON_RADIUS = 8.0;
    /** Цвет затемнения кнопки при наведении (ARGB). */
    private static final int HOVER_DARKEN = 0x80000000;

    public RarityUpgradeTableScreen(RarityUpgradeTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 192);
    }

    private boolean isOverButton(double mouseX, double mouseY) {
        double dx = mouseX - (leftPos + BUTTON_CENTER_X);
        double dy = mouseY - (topPos + BUTTON_CENTER_Y);
        return dx * dx + dy * dy <= BUTTON_RADIUS * BUTTON_RADIUS;
    }

    /** Заливает круг построчно — затемняется только сама кнопка, а не весь квадрат вокруг неё. */
    private void fillButtonCircle(GuiGraphicsExtractor graphics, int color) {
        int cx = leftPos + BUTTON_CENTER_X;
        int cy = topPos + BUTTON_CENTER_Y;
        int rows = (int) Math.ceil(BUTTON_RADIUS * 2);
        for (int row = 0; row < rows; row++) {
            double dy = row + 0.5 - BUTTON_RADIUS;
            double half = Math.sqrt(Math.max(0.0, BUTTON_RADIUS * BUTTON_RADIUS - dy * dy));
            int x0 = (int) Math.round(cx - half);
            int x1 = (int) Math.round(cx + half);
            int y = (int) Math.round(cy - BUTTON_RADIUS) + row;
            if (x1 > x0) {
                graphics.fill(x0, y, x1, y + 1, color);
            }
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blit(TEXTURE, leftPos, topPos, leftPos + imageWidth, topPos + imageHeight,
                0.0f, 1.0f, 0.0f, 1.0f);

        ItemStack itemStack = menu.container().getItem(RarityUpgradeTableBlockEntity.SLOT_ITEM);
        ItemStack shardStack = menu.container().getItem(RarityUpgradeTableBlockEntity.SLOT_SHARD);

        if (itemStack.isEmpty()) {
            graphics.blit(SHADOW_LEFT, leftPos + 62, topPos + 22, leftPos + 78, topPos + 38,
                    0.0f, 1.0f, 0.0f, 1.0f);
        }
        if (shardStack.isEmpty()) {
            graphics.blit(SHADOW_RIGHT, leftPos + 98, topPos + 22, leftPos + 114, topPos + 38,
                    0.0f, 1.0f, 0.0f, 1.0f);
        }

        int bx = leftPos + BUTTON_CENTER_X - BUTTON_SIZE / 2;
        int by = topPos + BUTTON_CENTER_Y - BUTTON_SIZE / 2;
        graphics.blit(BUTTON_TEXTURE, bx, by, bx + BUTTON_SIZE, by + BUTTON_SIZE,
                0.0f, 1.0f, 0.0f, 1.0f);

        if (isOverButton(mouseX, mouseY)) {
            fillButtonCircle(graphics, HOVER_DARKEN);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && isOverButton(event.x(), event.y())) {
            ItemStack itemStack = menu.container().getItem(RarityUpgradeTableBlockEntity.SLOT_ITEM);
            ItemStack shardStack = menu.container().getItem(RarityUpgradeTableBlockEntity.SLOT_SHARD);
            if (RarityUpgradeHandler.check(itemStack, shardStack).canAttempt()) {
                ClientPlayNetworking.send(new StartRarityUpgradePayload());
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {

        ItemStack itemStack = menu.container().getItem(RarityUpgradeTableBlockEntity.SLOT_ITEM);
        ItemStack shardStack = menu.container().getItem(RarityUpgradeTableBlockEntity.SLOT_SHARD);
        RarityUpgradeHandler.Result check = RarityUpgradeHandler.check(itemStack, shardStack);

        if (check.canAttempt()) {
            ItemRarity target = check.target();
            Component preview = Component.translatable("screen.arz_classes.rarity_upgrade_preview",
                    Component.translatable(target.translationKey()),
                    String.format("%.0f", check.chance() * 100));
            graphics.text(this.font, preview, 8, 78, target.argb());
        }
    }
}