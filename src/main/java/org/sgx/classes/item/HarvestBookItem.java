package org.sgx.classes.item;

import eu.pb4.trinkets.api.TrinketSlotAccess;
import eu.pb4.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
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
            bind(sp, player.getItemInHand(hand));
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

    /** +1 к счётчику в книге этого игрока (если она у него в слоте Trinkets/инвентаре и привязана к нему). */
    public static void record(ServerPlayer player, String key) {
        String uuid = player.getUUID().toString();
        for (ItemStack s : books(player)) {
            if (uuid.equals(s.get(ModDataComponents.HARVEST_OWNER_ID))) {
                Map<String, Integer> map = new HashMap<>(stats(s));
                map.merge(key, 1, Integer::sum);
                s.set(ModDataComponents.HARVEST_STATS, map);
                return;
            }
        }
    }

    public static void recordCrop(ServerPlayer player, Block crop) {
        record(player, "crop:" + BuiltInRegistries.BLOCK.getKey(crop));
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
                    for (ItemStack s : books(player)) {
                        if (bind(player, s)) {
                            break;
                        }
                    }
                }));
    }
}
