package org.sgx.classes.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.profession.ProfessionData;
import org.sgx.classes.profession.ProfessionFeedback;
import org.sgx.classes.profession.ProfessionType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Опыт Повара за еду из котелка Farmer's Delight.
 *
 * Опыт даётся ТОЛЬКО когда игрок сам забрал порцию из котелка:
 *  1) через GUI котелка (слот результата, индекс 8): клик, шифт-клик, двойной клик, число/Q и т.д. —
 *     считается, сколько предметов реально ушло из слота (см. CookingPotMenuClickMixin);
 *  2) ПКМ миск/бутылкой по котелку (порция берётся из слота-«витрины», индекс 6).
 * Воронки и прочая автоматика опыта не дают — игрок в этих сценариях не участвует.
 *
 * Защита от абуза:
 *  - только профессия Фермер (подкласс не обязателен), не в творческом/наблюдателе;
 *  - опыт считается по факту ушедших предметов, а не по кликам;
 *  - ПКМ-путь засчитывается только если одновременно убыл контейнер в руке И порция в котелке.
 */
public final class CookingPotXp {

    // ===== НАСТРОЙКИ =====
    /** Фиксированный опыт за одну порцию. */
    public static final int XP_PER_SERVING = 40;
    /** Индекс слота результата в меню котелка FD (0-5 ингредиенты, 6 витрина, 7 контейнер, 8 результат). */
    public static final int OUTPUT_SLOT = 8;

    private static final Identifier POT_BLOCK = Identifier.fromNamespaceAndPath("farmersdelight", "cooking_pot");
    private static final Identifier POT_MENU = Identifier.fromNamespaceAndPath("farmersdelight", "cooking_pot");

    private static final Map<Class<?>, Boolean> POT_MENU_CLASSES = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Method> MEAL_GETTERS = new ConcurrentHashMap<>();
    private static boolean mealGetterWarned = false;

    private record Pending(ServerPlayer player, Level level, InteractionHand hand, BlockPos pos,
                           Item heldItem, int heldBefore, Item mealItem, int mealBefore) {
    }

    private static final List<Pending> PENDING = new ArrayList<>();

    private CookingPotXp() {
    }

    public static void init() {
        // ПКМ по котелку: запоминаем состояние ДО (событие срабатывает перед взаимодействием), проверяем в конце тика.
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide() || !(player instanceof ServerPlayer sp) || !isEligible(sp)) {
                return InteractionResult.PASS;
            }
            ItemStack held = sp.getItemInHand(hand);
            if (held.isEmpty()) {
                return InteractionResult.PASS;
            }
            BlockPos pos = hitResult.getBlockPos();
            if (!POT_BLOCK.equals(BuiltInRegistries.BLOCK.getKey(world.getBlockState(pos).getBlock()))) {
                return InteractionResult.PASS;
            }
            BlockEntity be = world.getBlockEntity(pos);
            ItemStack meal = readMeal(be);
            if (meal == null || meal.isEmpty() || PENDING.size() > 256) {
                return InteractionResult.PASS;
            }
            PENDING.add(new Pending(sp, world, hand, pos, held.getItem(), held.getCount(),
                    meal.getItem(), meal.getCount()));
            return InteractionResult.PASS; // сами ничего не отменяем
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (PENDING.isEmpty()) {
                return;
            }
            List<Pending> work = new ArrayList<>(PENDING);
            PENDING.clear();
            for (Pending p : work) {
                resolveRightClick(p);
            }
        });
    }

    private static void resolveRightClick(Pending p) {
        if (p.player().isRemoved()) {
            return;
        }
        // Контейнер в руке должен убыть (порция реально выдана игроку).
        ItemStack heldNow = p.player().getItemInHand(p.hand());
        int heldAfter = heldNow.is(p.heldItem()) ? heldNow.getCount() : 0;
        int consumed = p.heldBefore() - heldAfter;
        if (consumed <= 0) {
            return;
        }
        // …и порция должна убыть из котелка (иначе это просто совпадение: например, котелок сам переложил еду в выход).
        ItemStack mealNow = readMeal(p.level().getBlockEntity(p.pos()));
        int mealAfter = (mealNow != null && !mealNow.isEmpty() && mealNow.is(p.mealItem())) ? mealNow.getCount() : 0;
        int removed = p.mealBefore() - mealAfter;
        if (removed <= 0) {
            return;
        }
        awardServings(p.player(), p.mealItem(), Math.min(consumed, removed));
    }

    // ===== ВЫЗЫВАЕТСЯ ИЗ CookingPotMenuClickMixin =====

    public static boolean isPotMenu(AbstractContainerMenu menu) {
        Class<?> cls = menu.getClass();
        Boolean cached = POT_MENU_CLASSES.get(cls);
        if (cached != null) {
            return cached;
        }
        boolean result;
        try {
            // getType() у меню без типа (например, инвентарь игрока) бросает исключение — поэтому результат кэшируется по классу.
            Identifier id = BuiltInRegistries.MENU.getKey(menu.getType());
            result = POT_MENU.equals(id);
        } catch (Exception e) {
            result = false;
        }
        if (!result && cls.getName().toLowerCase(Locale.ROOT).contains("cookingpot")) {
            result = true; // запасной вариант: id меню другой, но класс — котелок
        }
        POT_MENU_CLASSES.put(cls, result);
        return result;
    }

    public static boolean isEligible(ServerPlayer player) {
        if  (player.isSpectator()) {
            return false;
        }
        ProfessionData data = ModAttachments.get(player);
        // Опыт получает любой Фермер (подкласс не нужен). Чтобы вернуть «только Повар» — добавь: && data.currentSubclass() == SubclassType.COOK
        return data.currentProfession() == ProfessionType.FARMER;
    }

    /** Начислить фиксированный опыт ({@link #XP_PER_SERVING} за порцию), забранный игроком из котелка. */
    public static void awardServings(ServerPlayer player, Item dish, int servings) {
        if (servings <= 0) {
            return;
        }
        if (!isEligible(player)) {
            return;
        }
        int xp = XP_PER_SERVING * servings;
        ProfessionFeedback.grantXp(player, ProfessionType.FARMER, xp);
    }

    /**
     * Порция, лежащая в слоте-«витрине» котелка (CookingPotBlockEntity#getMeal). Через рефлексию — чтобы не
     * тянуть Farmer's Delight как compile-зависимость (так же, как остальной мод работает с FD по id).
     * Возвращает null, если метод не найден.
     */
    private static ItemStack readMeal(BlockEntity be) {
        if (be == null) {
            return null;
        }
        Class<?> cls = be.getClass();
        Method getter = MEAL_GETTERS.get(cls);
        if (getter == null) {
            try {
                getter = cls.getMethod("getMeal");
                MEAL_GETTERS.put(cls, getter);
            } catch (ReflectiveOperationException e) {
                if (!mealGetterWarned) {
                    mealGetterWarned = true;
                    Arz_classes.LOGGER.warn("[arz_classes] CookingPotXp: у {} нет метода getMeal() — опыт за ПКМ-выдачу из котелка отключён", cls.getName());
                }
                return null;
            }
        }
        try {
            Object result = getter.invoke(be);
            // Копируем сразу: FD отдаёт живой стек, а он потом уменьшится на наших глазах.
            return result instanceof ItemStack stack ? stack.copy() : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}