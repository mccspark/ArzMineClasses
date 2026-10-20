package org.sgx.classes.item;

import com.mojang.serialization.Codec;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.sgx.classes.Arz_classes;

import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModDataComponents {

    public static final DataComponentType<Integer> SHARPEN_LEVEL = register(
            "sharpen_level",
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());
    public static final DataComponentType<Boolean> PERMANENTLY_BROKEN = register(
            "permanently_broken",
            DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .build());

    /**
     * Список вставленных рун (по Identifier предмета-руны). Лимит по тиру — см. RuneSlots.
     * ВНИМАНИЕ (версионный риск): Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()) —
     * не проверял компиляцией под вашу 26.2, если не найдётся — пришлите ошибку.
     */
    public static final DataComponentType<List<Identifier>> RUNE_SOCKETS = register(
            "rune_sockets",
            DataComponentType.<List<Identifier>>builder()
                    .persistent(Identifier.CODEC.listOf())
                    .networkSynchronized(Identifier.STREAM_CODEC.apply(ByteBufCodecs.list()))
                    .build());

    /**
     * Редкость предмета (см. ItemRarity) — проставляется только при крафте у кузнеца
     * с открытым нужным milestone-узлом. Отсутствие компонента = COMMON (у обычных
     * игроков компонент никогда не появляется). Сетевой кодек написан вручную —
     * пишем/читаем id строкой напрямую, без обращения к несуществующему Codec.byName.
     */
    public static final DataComponentType<ItemRarity> ITEM_RARITY = register(
            "item_rarity",
            DataComponentType.<ItemRarity>builder()
                    .persistent(ItemRarity.CODEC)
                    .networkSynchronized(StreamCodec.of(
                            (buf, rarity) -> buf.writeUtf(rarity.getSerializedName()),
                            buf -> ItemRarity.byId(buf.readUtf())))
                    .build());
    public static final DataComponentType<Integer> MEAL_STARS = register(
            "meal_stars",
            DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** Книга урожая: UUID и ник первого открывшего + счётчики ("crop:<блок>" / "gold:<предмет>"). */
    public static final DataComponentType<String> HARVEST_OWNER_ID = register(
            "harvest_owner_id",
            DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());
    public static final DataComponentType<String> HARVEST_OWNER_NAME = register(
            "harvest_owner_name",
            DataComponentType.<String>builder()
                    .persistent(Codec.STRING)
                    .networkSynchronized(ByteBufCodecs.STRING_UTF8)
                    .build());
    public static final DataComponentType<Map<String, Integer>> HARVEST_STATS = register(
            "harvest_stats",
            DataComponentType.<Map<String, Integer>>builder()
                    .persistent(Codec.unboundedMap(Codec.STRING, Codec.INT))
                    .networkSynchronized(ByteBufCodecs.<ByteBuf, String, Integer, Map<String, Integer>>map(
                            size -> new HashMap<>(), ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT))
                    .build());

    private ModDataComponents() {
    }

    private static <T> DataComponentType<T> register(String path, DataComponentType<T> type) {
        ResourceKey<DataComponentType<?>> key = ResourceKey.create(
                Registries.DATA_COMPONENT_TYPE,
                Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, path));
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, key, type);
    }

    public static void init() {
        // Пустой метод-загрузчик: сам факт обращения к классу инициализирует static-поля выше.
    }
}