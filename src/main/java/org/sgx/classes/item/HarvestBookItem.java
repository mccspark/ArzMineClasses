package org.sgx.classes.item;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.sgx.classes.network.BindHarvestBookPayload;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Книга урожая: крафтит только Фермер. Первое открытие (ПКМ) привязывает её к нику игрока;
 * дальше урожай считается только пока книга у владельца — в слоте Trinkets ({@link #TRINKET_SLOT})
 * или в инвентаре. Открытие экрана — на клиенте через clientOpener (его ставит клиентский
 * инициализатор) либо хоткеем.
 */
public final class HarvestBookItem extends Item {

    /** Слот Trinkets, в который надевается книга (data/trinkets/slots/chest/harvest_book.json). */
    public static final String TRINKET_SLOT = "chest/harvest_book";

    /** Ставится клиентом: открыть экран книги для книги в указанной руке. */
    public static Consumer<InteractionHand> clientOpener;

    public HarvestBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            if (clientOpener != null) {
                clientOpener.accept(hand);
            }
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            ItemStack book = player.getItemInHand(hand);
            if (!bind(sp, book)) {
                importVanillaStats(sp, book);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Привязывает непривязанную книгу к игроку. false — книга уже привязана (к кому угодно). */
    public static boolean bind(ServerPlayer player, ItemStack book) {
        if (book.has(ModDataComponents.HARVEST_OWNER_ID)) {
            return false;
        }
        String name = player.getName().getString();
        book.set(ModDataComponents.HARVEST_OWNER_ID, player.getUUID().toString());
        book.set(ModDataComponents.HARVEST_OWNER_NAME, name);
        player.sendSystemMessage(Component.translatable("message.arz_classes.harvest_book_bound", name), true);
        importVanillaStats(player, book);
        return true;
    }

    public static Map<String, Integer> stats(ItemStack book) {
        return book.getOrDefault(ModDataComponents.HARVEST_STATS, Map.of());
    }

    /** Все книги игрока: сначала слот Trinkets, потом инвентарь. Работает и на клиенте. */
    public static List<ItemStack> books(Player player) {
        List<ItemStack> result = new ArrayList<>();
        for (TrinketSlotAccess access : TrinketsApi.getAttachment(player)
                .equipped(s -> s.getItem() instanceof HarvestBookItem, false)) {
            result.add(access.get());
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof HarvestBookItem) {
                result.add(s);
            }
        }
        return result;
    }

    /** Какую книгу показать по хоткею: привязанную к этому игроку, иначе любую; пусто — книги нет. */
    public static ItemStack findForDisplay(Player player) {
        String uuid = player.getUUID().toString();
        ItemStack fallback = ItemStack.EMPTY;
        for (ItemStack s : books(player)) {
            if (uuid.equals(s.get(ModDataComponents.HARVEST_OWNER_ID))) {
                return s;
            }
            if (fallback.isEmpty()) {
                fallback = s;
            }
        }
        return fallback;
    }

    /**
     * Культуры-блоки, которые НЕ считаются «по блоку»: у кустов перцев Rustic Delight цвет урожая
     * случайный, поэтому перцы считаются поштучно по выпавшим предметам ({@link #COUNTED_DROPS}).
     */
    private static final Set<String> UNCOUNTED_CROP_BLOCKS = Set.of(
            "rusticdelight:bell_peppers",
            "rusticdelight:pale_bell_peppers",
            "rusticdelight:dark_bell_peppers");

    /** Предметы, которые считаются поштучно по дропу с культуры (ключ в книге — "item:&lt;id&gt;"). */
    public static final Set<String> COUNTED_DROPS = Set.of(
            "rusticdelight:bell_pepper_green",
            "rusticdelight:bell_pepper_yellow",
            "rusticdelight:bell_pepper_red",
            "rusticdelight:bell_pepper_orange",
            "rusticdelight:bell_pepper_white",
            "rusticdelight:bell_pepper_pink",
            "rusticdelight:bell_pepper_blue",
            "rusticdelight:bell_pepper_purple",
            "rusticdelight:bell_pepper_black");

    /** +1 к счётчику в книге этого игрока (если она у него в слоте Trinkets/инвентаре и привязана к нему). */
    public static void record(ServerPlayer player, String key) {
        record(player, key, 1);
    }

    public static void record(ServerPlayer player, String key, int amount) {
        if (amount <= 0) {
            return;
        }
        String uuid = player.getUUID().toString();
        for (ItemStack s : books(player)) {
            if (uuid.equals(s.get(ModDataComponents.HARVEST_OWNER_ID))) {
                importVanillaStats(player, s);
                Map<String, Integer> map = new HashMap<>(stats(s));
                map.merge(key, amount, Integer::sum);
                s.set(ModDataComponents.HARVEST_STATS, map);
                return;
            }
        }
    }

    public static void recordCrop(ServerPlayer player, Block crop) {
        String id = BuiltInRegistries.BLOCK.getKey(crop).toString();
        if (UNCOUNTED_CROP_BLOCKS.contains(id)) {
            return;
        }
        record(player, "crop:" + id);
    }

    /**
     * Считает поштучно отслеживаемые дропы (перцы), только что выпавшие у блока {@code pos}.
     * Вызывать СРАЗУ после сбора культуры: выбираются ItemEntity, созданные в этом же тике (tickCount == 0).
     */
    public static void recordNearbyDrops(ServerPlayer player, ServerLevel level, BlockPos pos) {
        boolean hasBook = false;
        String uuid = player.getUUID().toString();
        for (ItemStack s : books(player)) {
            if (uuid.equals(s.get(ModDataComponents.HARVEST_OWNER_ID))) {
                hasBook = true;
                break;
            }
        }
        if (!hasBook) {
            return;
        }
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(1.0),
                entity -> entity.tickCount == 0)) {
            ItemStack stack = e.getItem();
            String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
            if (COUNTED_DROPS.contains(id)) {
                record(player, "item:" + id, stack.getCount());
            }
        }
    }

    // ───────────── история из ванильной статистики ─────────────

    /** Метка в счётчиках книги: история уже подтянута из статистики игрока (делается один раз на книгу). */
    public static final String META_IMPORTED = "meta:imported";

    /** Культуры, историю которых берём из ванильного «Добыто» (блок; ключ в книге — "crop:<id блока>"). */
    private static final String[] MINED_CROPS = {
            "minecraft:wheat", "minecraft:carrots", "minecraft:potatoes", "minecraft:beetroots",
            "minecraft:brown_mushroom", "minecraft:red_mushroom",
            "farmersdelight:cabbages", "farmersdelight:onions", "farmersdelight:rice_panicles",
            "rusticdelight:cotton", "rusticdelight:coffee"};

    /** Золотые культуры мода: историю берём из «Подобрано» (ключ — "gold:<id>"). Золотую морковь не трогаем: она крафтится. */
    private static final String[] GOLDEN_ITEMS = {
            "arz_classes:golden_wheat", "arz_classes:golden_potato", "arz_classes:golden_beetroot",
            "arz_classes:golden_cabbage", "arz_classes:golden_onion", "arz_classes:golden_tomato",
            "arz_classes:golden_rice"};

    private static Identifier idOf(String s) {
        int c = s.indexOf(':');
        return Identifier.fromNamespaceAndPath(s.substring(0, c), s.substring(c + 1));
    }

    private static void putMax(Map<String, Integer> map, String key, int value) {
        if (value > 0) {
            map.merge(key, value, Math::max);
        }
    }

    /**
     * Подтягивает в книгу то, что игрок уже сделал до неё, из ванильной статистики: добытые культуры и грибы,
     * подобранные перцы/помидоры/золотые культуры, убитых мирных животных. Один раз на книгу (метка
     * {@link #META_IMPORTED}); берётся максимум из «уже в книге» и «в статистике», чтобы ничего не задвоилось.
     * Разведение по видам в ванильной статистике не хранится (там только общее число), поэтому его не подтянуть.
     */
    public static void importVanillaStats(ServerPlayer player, ItemStack book) {
        if (!player.getUUID().toString().equals(book.get(ModDataComponents.HARVEST_OWNER_ID))) {
            return;
        }
        Map<String, Integer> current = stats(book);
        if (current.containsKey(META_IMPORTED)) {
            return;
        }
        Map<String, Integer> map = new HashMap<>(current);
        var counter = player.getStats();

        for (String id : MINED_CROPS) {
            BuiltInRegistries.BLOCK.get(idOf(id)).ifPresent(ref ->
                    putMax(map, "crop:" + id, counter.getValue(Stats.BLOCK_MINED.get(ref.value()))));
        }
        // помидоры собираются ПКМ и блок не ломают — считаем по подобранным помидорам
        BuiltInRegistries.ITEM.get(idOf("farmersdelight:tomato")).ifPresent(ref ->
                putMax(map, "crop:farmersdelight:tomatoes", counter.getValue(Stats.ITEM_PICKED_UP.get(ref.value()))));
        for (String id : COUNTED_DROPS) {
            BuiltInRegistries.ITEM.get(idOf(id)).ifPresent(ref ->
                    putMax(map, "item:" + id, counter.getValue(Stats.ITEM_PICKED_UP.get(ref.value()))));
        }
        for (String id : GOLDEN_ITEMS) {
            BuiltInRegistries.ITEM.get(idOf(id)).ifPresent(ref ->
                    putMax(map, "gold:" + id, counter.getValue(Stats.ITEM_PICKED_UP.get(ref.value()))));
        }
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type.getCategory() == MobCategory.CREATURE || type.getCategory() == MobCategory.AXOLOTLS) {
                putMax(map, "kill:" + BuiltInRegistries.ENTITY_TYPE.getKey(type),
                        counter.getValue(Stats.ENTITY_KILLED.get(type)));
            }
        }

        map.put(META_IMPORTED, 1);
        book.set(ModDataComponents.HARVEST_STATS, map);
    }

    /** Убитое мирное животное: "kill:&lt;id типа&gt;". */
    public static void recordKill(ServerPlayer player, EntityType<?> type) {
        record(player, "kill:" + BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    /** Разведённое животное: "bred:&lt;id типа&gt;". */
    public static void recordBred(ServerPlayer player, EntityType<?> type) {
        record(player, "bred:" + BuiltInRegistries.ENTITY_TYPE.getKey(type));
    }

    public static void recordGolden(ServerPlayer player, Item golden) {
        record(player, "gold:" + BuiltInRegistries.ITEM.getKey(golden));
    }

    /** Сеть: привязка книги по запросу клиента (хоткей). Вызывать из onInitialize. */
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(BindHarvestBookPayload.TYPE, BindHarvestBookPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(BindHarvestBookPayload.TYPE, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayer player = context.player();
                    String uuid = player.getUUID().toString();
                    for (ItemStack s : books(player)) {
                        if (bind(player, s)) {
                            break; // привязали непривязанную (заодно подтянулась история)
                        }
                        if (uuid.equals(s.get(ModDataComponents.HARVEST_OWNER_ID))) {
                            importVanillaStats(player, s); // своя книга, привязанная раньше — догоняем историю
                            break;
                        }
                    }
                }));
    }
}
