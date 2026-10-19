package org.sgx.classes.item;

import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.Arz_classes;

/** Слот под firewood в интерфейсе печи. Вынесен в отдельный класс: в миксинах нельзя объявлять внутренние классы. */
public class FirewoodSlot extends Slot {

    /** Спрайт-тень пустого слота: textures/gui/sprites/container/slot/firewood.png */
    private static final Identifier EMPTY_ICON = Arz_classes.id("container/slot/firewood");

    public FirewoodSlot(Container container, int index) {
        super(container, index, FurnaceFirewood.SLOT_X, FurnaceFirewood.SLOT_Y);
    }

    @Override
    public Identifier getNoItemIcon() {
        return EMPTY_ICON;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.is(ModItems.FIREWOOD);
    }
}