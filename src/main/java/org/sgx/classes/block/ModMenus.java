package org.sgx.classes.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.item.BonePouchMenu;
import org.sgx.classes.item.FoodPouchMenu;

public final class ModMenus {

    private static final ResourceKey<MenuType<?>> RARITY_UPGRADE_TABLE_KEY =
            ResourceKey.create(Registries.MENU, Arz_classes.id("rarity_upgrade_table"));

    public static final MenuType<RarityUpgradeTableMenu> RARITY_UPGRADE_TABLE =
            new MenuType<>(RarityUpgradeTableMenu::new, FeatureFlags.VANILLA_SET);

    private static final ResourceKey<MenuType<?>> BONE_POUCH_KEY =
            ResourceKey.create(Registries.MENU, Arz_classes.id("bone_pouch"));

    public static final MenuType<BonePouchMenu> BONE_POUCH =
            new MenuType<>(BonePouchMenu::new, FeatureFlags.VANILLA_SET);

    private static final ResourceKey<MenuType<?>> FOOD_POUCH_KEY =
            ResourceKey.create(Registries.MENU, Arz_classes.id("food_pouch"));

    public static final MenuType<FoodPouchMenu> FOOD_POUCH =
            new MenuType<>(FoodPouchMenu::new, FeatureFlags.VANILLA_SET);

    private static final ResourceKey<MenuType<?>> CARPENTER_WORKBENCH_KEY =
            ResourceKey.create(Registries.MENU, Arz_classes.id("carpenter_workbench"));

    public static final MenuType<CarpenterWorkbenchMenu> CARPENTER_WORKBENCH =
            new MenuType<>(CarpenterWorkbenchMenu::new, FeatureFlags.VANILLA_SET);

    private ModMenus() {
    }

    public static void init() {
        Registry.register(BuiltInRegistries.MENU, RARITY_UPGRADE_TABLE_KEY, RARITY_UPGRADE_TABLE);
        Registry.register(BuiltInRegistries.MENU, BONE_POUCH_KEY, BONE_POUCH);
        Registry.register(BuiltInRegistries.MENU, FOOD_POUCH_KEY, FOOD_POUCH);
        Registry.register(BuiltInRegistries.MENU, CARPENTER_WORKBENCH_KEY, CARPENTER_WORKBENCH);
    }

}