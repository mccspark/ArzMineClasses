package org.sgx.classes.item;

import net.minecraft.world.inventory.ContainerData;

/** Константы и общие интерфейсы для слота firewood в обычной печи. */
public final class FurnaceFirewood {

    /** Индекс дополнительного слота в контейнере печи (0 - сырьё, 1 - топливо, 2 - результат). */
    public static final int SLOT = 3;
    /** Координаты слота в интерфейсе печи (справа от слота результата, на уровне слота топлива). */
    public static final int SLOT_X = 148;
    public static final int SLOT_Y = 53;
    /** Сколько переплавленных предметов покрывает одно бревно firewood. */
    public static final int SMELTS_PER_LOG = 4;

    private FurnaceFirewood() {
    }

    /** Реализуется миксином на AbstractFurnaceBlockEntity (состояние хранится в самой печи). */
    public interface State {
        int arzClasses$getCharges();

        void arzClasses$setCharges(int charges);

        int arzClasses$getResultBefore();

        void arzClasses$setResultBefore(int count);

        ContainerData arzClasses$data();
    }

    /** Реализуется миксином на FurnaceMenu: даёт клиенту доступ к синхронизируемому счётчику использований. */
    public interface MenuAccess {
        ContainerData arzClasses$firewoodData();
    }
}