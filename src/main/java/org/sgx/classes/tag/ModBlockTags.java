package org.sgx.classes.tag;

import org.sgx.classes.Arz_classes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * Свои теги мода. Наполняются через датапак (см. data/arz_classes/tags/block/),
 * куда можно дописать блоки из ЛЮБОГО мода — включая опциональную ссылку на
 * общий конвенционный тег #c:crops, который используют многие модовые фермы
 * (в частности [Let's Do] Farm & Charm, судя по их changelog про cross-mod tags).
 */
public final class ModBlockTags {

    /** Блоки, чей сбор считается "сбором урожая" для класса Фермер. */
    public static final TagKey<Block> FARMER_CROPS = create("farmer_crops");

    private ModBlockTags() {
    }

    private static TagKey<Block> create(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, path));
    }
}