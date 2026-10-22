package org.sgx.classes.item;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.sgx.classes.attachment.ModAttachments;
import org.sgx.classes.network.BindHarvestBookPayload;
import org.sgx.classes.profession.ProfessionType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Книга класса со статистикой (общая основа для книги урожая, кузнеца, мистика, шахтёра и лесоруба).
 * <p>
 * Первое открытие (ПКМ или хоткей) привязывает книгу к игроку. Считает книга, лежащая у владельца в слоте
 * Trinkets ({@link #TRINKET_SLOT}) или в инвентаре. Счётчики хранятся прямо на предмете
 * ({@link ModDataComponents#HARVEST_STATS}): ключи вида "префикс:id" → число.
 */
public class ClassBookItem extends Item {

    /**
     * Слот Trinkets для всех книг. Идентификатор остался от книги урожая, чтобы не потерять уже надетые книги
     * (меняется только название слота в переводах).
     */
    public static final String TRINKET_SLOT = "chest/harvest_book";

    /** Метка в счётчиках: история из ванильной статистики уже подтянута (один раз на книгу). */
    public static final String META_IMPORTED = "meta:imported";

    /** Ставится клиентом: открыть экран книги данного вида для книги в указанной руке. */
    public static BiConsumer<BookKind, InteractionHand> clientOpener;

    private final BookKind kind;

    public ClassBookItem(Properties properties, BookKind kind) {
        super(properties);
        this.kind = kind;
    }

    public BookKind kind() {
        return kind;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            if (clientOpener != null) {
                clientOpener.accept(kind, hand);
            }
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            ItemStack book = player.getItemInHand(hand);
            if (!bind(sp, book)) {
                BookHistory.importOnce(sp, book);
            }
        }
        return InteractionResult.SUCCESS;
    }

    // ───────────── привязка ─────────────

    /** Привязывает непривязанную книгу к игроку (и подтягивает историю). false — книга уже привязана. */
    public static boolean bind(ServerPlayer player, ItemStack book) {
        if (book.has(ModDataComponents.HARVEST_OWNER_ID)) {
            return false;
        }
        String name = player.getName().getString();
        book.set(ModDataComponents.HARVEST_OWNER_ID, player.getUUID().toString());
        book.set(ModDataComponents.HARVEST_OWNER_NAME, name);
        player.sendSystemMessage(Component.translatable("message.arz_classes.harvest_book_bound", name), true);
        BookHistory.importOnce(player, book);
        return true;
    }

    public static Map<String, Integer> stats(ItemStack book) {
        return book.getOrDefault(ModDataComponents.HARVEST_STATS, Map.of());
    }

    // ───────────── поиск книг ─────────────

    /** Все книги игрока (любого вида): сначала слот Trinkets, потом инвентарь. Работает и на клиенте. */
    public static List<ItemStack> allBooks(Player player) {
        List<ItemStack> result = new ArrayList<>();
        for (TrinketSlotAccess access : TrinketsApi.getAttachment(player)
                .equipped(s -> s.getItem() instanceof ClassBookItem, false)) {
            result.add(access.get());
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof ClassBookItem) {
                result.add(s);
            }
        }
        return result;
    }

    /** Книги одного вида. */
    public static List<ItemStack> books(Player player, BookKind kind) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack s : allBooks(player)) {
            if (BookKind.of(s) == kind) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * Какую книгу открыть по хоткею: сначала книга текущего класса (привязанная к игроку), потом любая
     * привязанная к игроку, потом любая. Пусто — книг нет вовсе.
     */
    public static ItemStack findForDisplay(Player player, ProfessionType currentClass) {
        String uuid = player.getUUID().toString();
        BookKind preferred = BookKind.forProfession(currentClass);
        ItemStack best = ItemStack.EMPTY;
        int bestScore = -1;
        for (ItemStack s : allBooks(player)) {
            int score = 0;
            if (uuid.equals(s.get(ModDataComponents.HARVEST_OWNER_ID))) {
                score += 2;
            }
            if (preferred != null && BookKind.of(s) == preferred) {
                score += 4;
            }
            if (score > bestScore) {
                bestScore = score;
                best = s;
            }
        }
        return best;
    }

    // ───────────── запись счётчиков (сервер) ─────────────

    /** Первая книга этого вида, привязанная к игроку. */
    private static ItemStack ownBook(ServerPlayer player, BookKind kind) {
        String uuid = player.getUUID().toString();
        for (ItemStack s : books(player, kind)) {
            if (uuid.equals(s.get(ModDataComponents.HARVEST_OWNER_ID))) {
                return s;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean allowed(ServerPlayer player, BookKind kind) {
        return !kind.requiresClass() || ModAttachments.get(player).currentProfession() == kind.profession();
    }

    /** Есть ли у игрока своя книга этого вида (и активен ли её класс). Дешёвая проверка перед тяжёлой работой. */
    public static boolean isTracking(ServerPlayer player, BookKind kind) {
        return allowed(player, kind) && !ownBook(player, kind).isEmpty();
    }

    /** +amount к счётчику в книге игрока. Если книги нет (или не тот класс) — ничего не происходит. */
    public static void record(ServerPlayer player, BookKind kind, String key, int amount) {
        if (amount <= 0 || !allowed(player, kind)) {
            return;
        }
        ItemStack book = ownBook(player, kind);
        if (book.isEmpty()) {
            return;
        }
        BookHistory.importOnce(player, book);
        Map<String, Integer> map = new HashMap<>(stats(book));
        map.merge(key, amount, Integer::sum);
        book.set(ModDataComponents.HARVEST_STATS, map);
    }

    public static void record(ServerPlayer player, BookKind kind, String key) {
        record(player, kind, key, 1);
    }

    /** Рекорд: записывает значение, только если оно больше уже сохранённого. */
    public static void recordMax(ServerPlayer player, BookKind kind, String key, int value) {
        if (value <= 0 || !allowed(player, kind)) {
            return;
        }
        ItemStack book = ownBook(player, kind);
        if (book.isEmpty() || stats(book).getOrDefault(key, 0) >= value) {
            return;
        }
        BookHistory.importOnce(player, book);
        Map<String, Integer> map = new HashMap<>(stats(book));
        map.merge(key, value, Math::max);
        book.set(ModDataComponents.HARVEST_STATS, map);
    }

    // ───────────── сеть ─────────────

    /** Сеть: привязка/догон истории по запросу клиента (хоткей). Вызывать из onInitialize. */
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(BindHarvestBookPayload.TYPE, BindHarvestBookPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BindHarvestBookPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    String uuid = player.getUUID().toString();
                    for (ItemStack s : allBooks(player)) {
                        if (bind(player, s)) {
                            continue; // привязали непривязанную (история подтянулась в bind)
                        }
                        if (uuid.equals(s.get(ModDataComponents.HARVEST_OWNER_ID))) {
                            BookHistory.importOnce(player, s); // своя, привязана раньше — догоняем историю
                        }
                    }
                }));
    }
}
