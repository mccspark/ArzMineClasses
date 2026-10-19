package org.sgx.classes.item;

import eu.pb4.trinkets.api.component.TrinketDataComponents;
import eu.pb4.trinkets.api.component.TrinketEquippable;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import org.sgx.classes.Arz_classes;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModItems {

    private static final ResourceKey<Item> BLACKSMITH_HAMMER_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("blacksmith_hammer"));
    private static final ResourceKey<Item> COPPER_HAMMER_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("copper_hammer"));
    private static final ResourceKey<Item> DIAMOND_SHARD_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("diamond_shard"));
    private static final ResourceKey<Item> KNOWLEDGE_SCROLL_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("knowledge_scroll"));
    private static final ResourceKey<Item> ADMIN_DAMAGE_STICK_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("admin_damage_stick"));
    private static final ResourceKey<Item> ADMIN_KNOCKBACK_STICK_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("admin_knockback_stick"));
    private static final ResourceKey<Item> GOLDEN_BEETROOT_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("golden_beetroot"));
    private static final ResourceKey<Item> GOLDEN_CABBAGE_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("golden_cabbage"));
    private static final ResourceKey<Item> GOLDEN_ONION_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("golden_onion"));
    private static final ResourceKey<Item> GOLDEN_POTATO_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("golden_potato"));
    private static final ResourceKey<Item> GOLDEN_RICE_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("golden_rice"));
    private static final ResourceKey<Item> GOLDEN_TOMATO_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("golden_tomato"));
    private static final ResourceKey<Item> GOLDEN_WHEAT_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("golden_wheat"));
    private static final ResourceKey<Item> FIREWOOD_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("firewood"));
    private static final ResourceKey<Item> MINER_HELMET_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("miner_helmet"));

    public static final Item KNOWLEDGE_SCROLL = new org.sgx.classes.item.KnowledgeScrollItem(
            new Item.Properties()
                    .stacksTo(16)
                    .setId(KNOWLEDGE_SCROLL_KEY));

    public static final Item BLACKSMITH_HAMMER = new Item(
            new Item.Properties()
                    .durability(150)
                    .setId(BLACKSMITH_HAMMER_KEY));

    /** Дешёвая/слабая альтернатива основному молоту — та же роль, всего 50 прочности. */
    public static final Item COPPER_HAMMER = new Item(
            new Item.Properties()
                    .durability(50)
                    .setId(COPPER_HAMMER_KEY));

    public static final Item DIAMOND_SHARD = new Item(
            new Item.Properties().setId(DIAMOND_SHARD_KEY));
    public static final  Item FIREWOOD = new Item(
            new Item.Properties().setId(FIREWOOD_KEY));

    public static final Item MINER_HELMET = new Item(
            new Item.Properties()
                    .durability(100)
                    .setId(MINER_HELMET_KEY)
                    .component(DataComponents.EQUIPPABLE,
                            Equippable.builder(EquipmentSlot.HEAD)
                                    .setEquipSound(SoundEvents.ARMOR_EQUIP_IRON)
                                    .setAsset(ResourceKey.create(EquipmentAssets.ROOT_ID, Arz_classes.id("miner_helmet")))
                                    .build()));



    public static final Item ADMIN_DAMAGE_STICK = new Item(
            new Item.Properties()
                    .stacksTo(1)
                    .setId(ADMIN_DAMAGE_STICK_KEY)
                    .component(DataComponents.ATTRIBUTE_MODIFIERS,
                            ItemAttributeModifiers.builder()
                                    .add(Attributes.ATTACK_DAMAGE,
                                            new AttributeModifier(Arz_classes.id("admin_damage_stick_bonus"),
                                                    1000.0, AttributeModifier.Operation.ADD_VALUE),
                                            EquipmentSlotGroup.MAINHAND)
                                    .build()));

    /** Отладочный предмет: +1000 к отдаче удара в основной руке. Только для админов/креатива. */
    public static final Item ADMIN_KNOCKBACK_STICK = new Item(
            new Item.Properties()
                    .stacksTo(1)
                    .setId(ADMIN_KNOCKBACK_STICK_KEY)
                    .component(DataComponents.ATTRIBUTE_MODIFIERS,
                            ItemAttributeModifiers.builder()
                                    .add(Attributes.ATTACK_KNOCKBACK,
                                            new AttributeModifier(Arz_classes.id("admin_knockback_stick_bonus"),
                                                    1000.0, AttributeModifier.Operation.ADD_VALUE),
                                            EquipmentSlotGroup.MAINHAND)
                                    .build()));
    private static final ResourceKey<Item> BONE_POUCH_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("bone_pouch"));

    public static final Item BONE_POUCH = new BonePouchItem(
            new Item.Properties()
                    .stacksTo(1)
                    .setId(BONE_POUCH_KEY));

    private static final ResourceKey<Item> RING_IRON_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("ring_iron"));
    private static final ResourceKey<Item> RING_GOLD_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("ring_gold"));
    private static final ResourceKey<Item> RING_NETHERITE_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("ring_netherite"));

    public static final Item RING_IRON = new Item(
            new Item.Properties()
                    .stacksTo(1)
                    .setId(RING_IRON_KEY)
                    .component(TrinketDataComponents.EQUIPMENT,
                            TrinketEquippable.DEFAULT.withSlots("chest/ring")));

    public static final Item RING_GOLD = new Item(
            new Item.Properties()
                    .stacksTo(1)
                    .setId(RING_GOLD_KEY)
                    .component(TrinketDataComponents.EQUIPMENT,
                            TrinketEquippable.DEFAULT.withSlots("chest/ring")));

    public static final Item RING_NETHERITE = new Item(
            new Item.Properties()
                    .stacksTo(1)
                    .setId(RING_NETHERITE_KEY)
                    .component(TrinketDataComponents.EQUIPMENT,
                            TrinketEquippable.DEFAULT.withSlots("chest/ring")));

    public static final Map<ItemRarity, Item> RARITY_LOOT_ITEMS = new LinkedHashMap<>();

    static {
        for (ItemRarity rarity : ItemRarity.values()) {
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                    Arz_classes.id("rarity_loot_" + rarity.getSerializedName()));
            RARITY_LOOT_ITEMS.put(rarity,
                    new RarityLootItem(new Item.Properties().setId(key), rarity));
        }
    }

    public static final Map<ItemRarity, Item> RARITY_SHARDS = new LinkedHashMap<>();

    static {
        for (ItemRarity rarity : ItemRarity.values()) {
            if (rarity == ItemRarity.COMMON) {
                continue;
            }
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                    Arz_classes.id("rarity_shard_" + rarity.getSerializedName()));
            RARITY_SHARDS.put(rarity, new Item(new Item.Properties().setId(key)));
        }
    }
    public static final Map<ItemRarity, Item> RARITY_FRAGMENTS = new LinkedHashMap<>();

    static {
        for (ItemRarity rarity : ItemRarity.values()) {
            if (rarity == ItemRarity.COMMON) {
                continue;
            }
            ResourceKey<Item> key = ResourceKey.create(Registries.ITEM,
                    Arz_classes.id("rarity_fragment_" + rarity.getSerializedName()));
            RARITY_FRAGMENTS.put(rarity, new Item(new Item.Properties().setId(key)));
        }
    }
    public static final FoodProperties GOLDEN_BEETROOT_PROPERTIES = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(1.5f)
            .alwaysEdible()
            .build();
    public static final Item GOLDEN_BEETROOT = new Item(new Item.Properties()
            .setId(GOLDEN_BEETROOT_KEY)
            .food(GOLDEN_BEETROOT_PROPERTIES)
            .stacksTo(64)
    );
    public static final FoodProperties GOLDEN_CABBAGE_PROPERTIES = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(1.5f)
            .alwaysEdible()
            .build();
    public static final Item GOLDEN_CABBAGE= new Item(new Item.Properties()
            .setId(GOLDEN_CABBAGE_KEY)
            .food(GOLDEN_CABBAGE_PROPERTIES)
            .stacksTo(64)
    );
    public static final FoodProperties GOLDEN_ONION_PROPERTIES = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(1.5f)
            .alwaysEdible()
            .build();
    public static final Item GOLDEN_ONION= new Item(new Item.Properties()
            .setId(GOLDEN_ONION_KEY)
            .food(GOLDEN_ONION_PROPERTIES)
            .stacksTo(64)
    );
    public static final FoodProperties GOLDEN_POTATO_PROPERTIES = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(1.5f)
            .alwaysEdible()
            .build();
    public static final Item GOLDEN_POTATO= new Item(new Item.Properties()
            .setId(GOLDEN_POTATO_KEY)
            .food(GOLDEN_POTATO_PROPERTIES)
            .stacksTo(64)
    );
    public static final FoodProperties GOLDEN_RICE_PROPERTIES = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(1.5f)
            .alwaysEdible()
            .build();
    public static final Item GOLDEN_RICE= new Item(new Item.Properties()
            .setId(GOLDEN_RICE_KEY)
            .food(GOLDEN_RICE_PROPERTIES)
            .stacksTo(64)
    );
    public static final FoodProperties GOLDEN_TOMATO_PROPERTIES = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(1.5f)
            .alwaysEdible()
            .build();
    public static final Item GOLDEN_TOMATO= new Item(new Item.Properties()
            .setId(GOLDEN_TOMATO_KEY)
            .food(GOLDEN_TOMATO_PROPERTIES)
            .stacksTo(64)
    );
    public static final Item GOLDEN_WHEAT= new Item(
            new Item.Properties()
                    .stacksTo(64)
                    .setId(GOLDEN_WHEAT_KEY));


    private ModItems() {
    }

    public static void init() {
        Registry.register(BuiltInRegistries.ITEM, KNOWLEDGE_SCROLL_KEY, KNOWLEDGE_SCROLL);
        Registry.register(BuiltInRegistries.ITEM, BLACKSMITH_HAMMER_KEY, BLACKSMITH_HAMMER);
        Registry.register(BuiltInRegistries.ITEM, COPPER_HAMMER_KEY, COPPER_HAMMER);
        Registry.register(BuiltInRegistries.ITEM, DIAMOND_SHARD_KEY, DIAMOND_SHARD);
        Registry.register(BuiltInRegistries.ITEM, FIREWOOD_KEY, FIREWOOD);
        Registry.register(BuiltInRegistries.ITEM, MINER_HELMET_KEY, MINER_HELMET);
        Registry.register(BuiltInRegistries.ITEM, ADMIN_DAMAGE_STICK_KEY, ADMIN_DAMAGE_STICK);
        Registry.register(BuiltInRegistries.ITEM, ADMIN_KNOCKBACK_STICK_KEY, ADMIN_KNOCKBACK_STICK);
        Registry.register(BuiltInRegistries.ITEM, RING_IRON_KEY, RING_IRON);
        Registry.register(BuiltInRegistries.ITEM, RING_GOLD_KEY, RING_GOLD);
        Registry.register(BuiltInRegistries.ITEM, RING_NETHERITE_KEY, RING_NETHERITE);
        Registry.register(BuiltInRegistries.ITEM, BONE_POUCH_KEY, BONE_POUCH);
        Registry.register(BuiltInRegistries.ITEM, GOLDEN_BEETROOT_KEY, GOLDEN_BEETROOT);
        Registry.register(BuiltInRegistries.ITEM, GOLDEN_CABBAGE_KEY, GOLDEN_CABBAGE);
        Registry.register(BuiltInRegistries.ITEM, GOLDEN_ONION_KEY, GOLDEN_ONION);
        Registry.register(BuiltInRegistries.ITEM, GOLDEN_POTATO_KEY, GOLDEN_POTATO);
        Registry.register(BuiltInRegistries.ITEM, GOLDEN_RICE_KEY, GOLDEN_RICE);
        Registry.register(BuiltInRegistries.ITEM, GOLDEN_TOMATO_KEY, GOLDEN_TOMATO);
        Registry.register(BuiltInRegistries.ITEM, GOLDEN_WHEAT_KEY, GOLDEN_WHEAT);

        for (var entry : RARITY_LOOT_ITEMS.entrySet()) {
            Registry.register(BuiltInRegistries.ITEM,
                    Arz_classes.id("rarity_loot_" + entry.getKey().getSerializedName()), entry.getValue());
        }
        for (var entry : RARITY_SHARDS.entrySet()) {
            Registry.register(BuiltInRegistries.ITEM,
                    Arz_classes.id("rarity_shard_" + entry.getKey().getSerializedName()), entry.getValue());
        }
        for (var entry : RARITY_FRAGMENTS.entrySet()) {
            Registry.register(BuiltInRegistries.ITEM,
                    Arz_classes.id("rarity_fragment_" + entry.getKey().getSerializedName()), entry.getValue());
        }
    }
}