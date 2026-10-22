package org.sgx.classes.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionType;

import java.util.List;

/**
 * Боковая панель: ник + класс, модель, атрибуты. Иконки здоровья и удачи — ванильные спрайты
 * (сердце из HUD и клевер эффекта «Удача»), остальные пока временные из ванильных предметов.
 * Ширина задаётся при создании (WIDTH — эталонная), чтобы панель сжималась вместе с экраном.
 */
public final class CharacterPanel {

    /** Эталонная (максимальная) ширина панели. */
    public static final int WIDTH = 132;

    private static final int NICK_Y_OFFSET = 6;
    private static final int CLASS_Y_OFFSET = 16;
    private static final int MODEL_TOP_OFFSET = 30;
    private static final int MODEL_MAX_HEIGHT = 92;
    private static final int MODEL_MIN_HEIGHT = 48;
    private static final int MODEL_MARGIN_X = 15;
    /** Масштаб модели при максимальной высоте области (MODEL_MAX_HEIGHT); на меньших экранах уменьшается пропорционально. */
    private static final float MODEL_BASE_SCALE = 28f;
    /** Отступ между моделью и заголовком «Атрибуты». */
    private static final int MODEL_ATTR_GAP = 12;
    private static final int ATTR_HEADER_GAP = 10;
    private static final int ATTR_ROWS = 5;
    private static final int ROW_HEIGHT = 12;
    private static final int ROW_MARGIN_X = 8;
    private static final int BOTTOM_RESERVED = 26;

    /** Всё, что занимает место под моделью: заголовок, строки, нижний ряд кнопок и небольшой запас. */
    private static final int BELOW_MODEL_RESERVED =
            MODEL_TOP_OFFSET + MODEL_ATTR_GAP + ATTR_HEADER_GAP + ATTR_ROWS * ROW_HEIGHT + BOTTOM_RESERVED + 2;

    private static final float ICON_SCALE = 0.6f;
    private static final int ICON_SIZE = 16; // до масштаба; после ICON_SCALE фактически ~10px
    private static final int ICON_TEXT_GAP = 3;

    private static final Identifier HEART_SPRITE = Identifier.withDefaultNamespace("hud/heart/full");
    private static final Identifier LUCK_SPRITE = Identifier.withDefaultNamespace("mob_effect/luck");

    private final int left;
    private final int top;
    private final int width;
    private final int height;

    public CharacterPanel(int left, int top, int width, int height) {
        this.left = left;
        this.top = top;
        this.width = width;
        this.height = height;
    }

    private static double mainHandAttackDamage(Player player) {
        double base = player.getAttributeValue(Attributes.ATTACK_DAMAGE); // база игрока (обычно 1.0 + эффекты типа Strength)
        ItemStack mainHand = player.getMainHandItem();
        var modifiers = mainHand.get(net.minecraft.core.component.DataComponents.ATTRIBUTE_MODIFIERS);
        if (modifiers != null) {
            for (var entry : modifiers.modifiers()) {
                if (entry.attribute().is(Attributes.ATTACK_DAMAGE)
                        && entry.slot().test(net.minecraft.world.entity.EquipmentSlot.MAINHAND)) {
                    double amount = entry.modifier().amount();
                    base += switch (entry.modifier().operation()) {
                        case ADD_VALUE -> amount;
                        case ADD_MULTIPLIED_BASE, ADD_MULTIPLIED_TOTAL -> base * amount;
                    };
                }
            }
        }
        return base;
    }

    public int left() {
        return left;
    }

    public int width() {
        return width;
    }

    /** Верх кнопки внизу панели (под будущую другую прокачку). */
    public int skillsButtonTop() {
        return top + height - BOTTOM_RESERVED + 4;
    }

    public int skillsButtonHeight() {
        return BOTTOM_RESERVED - 8;
    }

    public void extractBackground(GuiGraphicsExtractor graphics) {
        BookStyle.panel(graphics, left, top, width, height);
    }

    /** Подрезает строку по ширине, добавляя многоточие. */
    private static String fit(Font font, String text, int maxWidth) {
        if (maxWidth <= 0) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        int ellipsis = font.width("…");
        return font.plainSubstrByWidth(text, Math.max(0, maxWidth - ellipsis)) + "…";
    }

    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, Font font, ProfessionData data) {
        Player player = Minecraft.getInstance().player;

        if (player != null) {
            graphics.centeredText(font, player.getName(), left + width / 2, top + NICK_Y_OFFSET, 0xFFFFFFFF);
        }

        ProfessionType current = data.currentProfession();
        if (current != ProfessionType.NONE) {
            Component classLine = data.currentSubclass() != null
                    ? Component.translatable("screen.arz_classes.profession_button",
                    data.currentSubclass().displayName(), data.level())
                    : Component.translatable("screen.arz_classes.profession_button",
                    current.displayName(), data.level());
            graphics.centeredText(font, classLine, left + width / 2, top + CLASS_Y_OFFSET, 0xFFAAAAAA);
        }

        int modelHeight = Math.max(MODEL_MIN_HEIGHT, Math.min(MODEL_MAX_HEIGHT, height - BELOW_MODEL_RESERVED));

        if (player == null) {
            graphics.centeredText(font, Component.literal("—"), left + width / 2, top + MODEL_TOP_OFFSET + 20, 0xFF666666);
            return;
        }

        int modelTop = top + MODEL_TOP_OFFSET;
        int modelBottom = modelTop + modelHeight;
        int modelMargin = Math.min(MODEL_MARGIN_X, width / 6);
        int modelScale = Math.max(16, Math.round(MODEL_BASE_SCALE * modelHeight / MODEL_MAX_HEIGHT));
        InventoryScreen.extractEntityInInventoryFollowsMouse(
                graphics, left + modelMargin, modelTop, left + width - modelMargin, modelBottom, modelScale, 0.0625F,
                (float) mouseX, (float) mouseY, player);

        int attrHeaderY = modelBottom + MODEL_ATTR_GAP;
        graphics.centeredText(font, Component.translatable("screen.arz_classes.attributes"),
                left + width / 2, attrHeaderY, 0xFFAAAAAA);

        List<AttributeRow> rows = List.of(
                AttributeRow.ofSprite(HEART_SPRITE, "screen.arz_classes.attr.health",
                        format(player.getHealth()) + " / " + format(player.getAttributeValue(Attributes.MAX_HEALTH))),
                AttributeRow.ofItem(new ItemStack(Items.IRON_SWORD), "screen.arz_classes.attr.attack_damage",
                        format(mainHandAttackDamage(player))),
                AttributeRow.ofItem(new ItemStack(Items.FEATHER), "screen.arz_classes.attr.attack_speed",
                        format(player.getAttributeValue(Attributes.ATTACK_SPEED))),
                AttributeRow.ofItem(new ItemStack(Items.IRON_CHESTPLATE), "screen.arz_classes.attr.armor",
                        format(player.getAttributeValue(Attributes.ARMOR))),
                AttributeRow.ofSprite(LUCK_SPRITE, "screen.arz_classes.attr.luck",
                        format(player.getAttributeValue(Attributes.LUCK)))
        );

        int iconVisualSize = Math.round(ICON_SIZE * ICON_SCALE);
        int labelX = left + ROW_MARGIN_X + iconVisualSize + ICON_TEXT_GAP;

        int rowY = attrHeaderY + ATTR_HEADER_GAP;
        for (AttributeRow row : rows) {
            if (row.sprite() != null) {
                graphics.blitSprite(RenderPipelines.GUI_TEXTURED, row.sprite(),
                        left + ROW_MARGIN_X, rowY - 1, iconVisualSize, iconVisualSize);
            } else {
                graphics.pose().pushMatrix();
                graphics.pose().translate(left + ROW_MARGIN_X, rowY - 2);
                graphics.pose().scale(ICON_SCALE, ICON_SCALE);
                graphics.item(row.icon(), 0, 0);
                graphics.pose().popMatrix();
            }

            int valueWidth = font.width(row.value());
            int maxLabel = (left + width - ROW_MARGIN_X - valueWidth - 4) - labelX;
            String label = fit(font, Component.translatable(row.labelKey()).getString(), maxLabel);
            graphics.text(font, label, labelX, rowY, 0xFFCCCCCC);
            graphics.text(font, row.value(), left + width - ROW_MARGIN_X - valueWidth, rowY, 0xFFFFFFFF);
            rowY += ROW_HEIGHT;
        }
    }

    private static String format(double value) {
        return value == Math.floor(value) ? String.valueOf((int) value) : String.format("%.1f", value);
    }

    /** Строка атрибута: иконка — либо предмет (icon), либо спрайт из GUI-атласа (sprite). */
    private record AttributeRow(ItemStack icon, Identifier sprite, String labelKey, String value) {
        static AttributeRow ofItem(ItemStack icon, String labelKey, String value) {
            return new AttributeRow(icon, null, labelKey, value);
        }

        static AttributeRow ofSprite(Identifier sprite, String labelKey, String value) {
            return new AttributeRow(null, sprite, labelKey, value);
        }
    }
}