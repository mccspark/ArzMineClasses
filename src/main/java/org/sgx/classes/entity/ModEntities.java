package org.sgx.classes.entity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import org.sgx.classes.Arz_classes;

public final class ModEntities {

    private static final ResourceKey<EntityType<?>> TRAINING_DUMMY_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Arz_classes.id("training_dummy"));
    private static final ResourceKey<Item> TRAINING_DUMMY_ITEM_KEY =
            ResourceKey.create(Registries.ITEM, Arz_classes.id("training_dummy"));

    /** Манекен для тренировок: стойка для брони (проходимая, но ловит удары) с моделью-пугалом. */
    public static final EntityType<TrainingDummy> TRAINING_DUMMY =
            EntityType.Builder.<TrainingDummy>of(TrainingDummy::new, MobCategory.MISC)
                    .sized(0.5f, 1.975f)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build(TRAINING_DUMMY_KEY);

    public static final Item TRAINING_DUMMY_ITEM = new TrainingDummyItem(
            new Item.Properties().setId(TRAINING_DUMMY_ITEM_KEY));

    private ModEntities() {
    }

    public static void init() {
        Registry.register(BuiltInRegistries.ENTITY_TYPE, TRAINING_DUMMY_KEY, TRAINING_DUMMY);
        FabricDefaultAttributeRegistry.register(TRAINING_DUMMY, LivingEntity.createLivingAttributes());
        Registry.register(BuiltInRegistries.ITEM, TRAINING_DUMMY_ITEM_KEY, TRAINING_DUMMY_ITEM);
    }
}
