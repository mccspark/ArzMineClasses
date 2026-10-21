package org.sgx.classes.block;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.item.ModBlocks;

import java.util.Set;

public final class ModBlockEntities {

    private static final ResourceKey<BlockEntityType<?>> RARITY_UPGRADE_TABLE_KEY =
            ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, Arz_classes.id("rarity_upgrade_table"));

    public static final BlockEntityType<RarityUpgradeTableBlockEntity> RARITY_UPGRADE_TABLE =
            new BlockEntityType<>(RarityUpgradeTableBlockEntity::new, Set.of(ModBlocks.RARITY_UPGRADE_TABLE));

    private static final ResourceKey<BlockEntityType<?>> CARPENTER_WORKBENCH_KEY =
            ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, Arz_classes.id("carpenter_workbench"));

    public static final BlockEntityType<CarpenterWorkbenchEntity> CARPENTER_WORKBENCH =
            new BlockEntityType<>(CarpenterWorkbenchEntity::new, Set.of(ModBlocks.CARPENTER_WORKBENCH));

    private static final ResourceKey<BlockEntityType<?>> WEAPON_RACK_KEY =
            ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, Arz_classes.id("weapon_rack"));

    public static final BlockEntityType<WeaponRackBlockEntity> WEAPON_RACK =
            new BlockEntityType<>(WeaponRackBlockEntity::new, Set.copyOf(ModWeaponRacks.blocks()));

    private ModBlockEntities() {
    }

    public static void init() {
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, RARITY_UPGRADE_TABLE_KEY, RARITY_UPGRADE_TABLE);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, CARPENTER_WORKBENCH_KEY, CARPENTER_WORKBENCH);
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, WEAPON_RACK_KEY, WEAPON_RACK);
    }
}