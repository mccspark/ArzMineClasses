package org.sgx.classes.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.sgx.classes.profession.ProfessionType;

/**
 * Книги со статистикой. У каждого класса своя книга; все они носятся в одном слоте Trinkets
 * ({@link ClassBookItem#TRINKET_SLOT}) и открываются одной клавишей.
 */
public enum BookKind {
    HARVEST("harvest_book", ProfessionType.FARMER, false),
    BLACKSMITH("blacksmith_book", ProfessionType.BLACKSMITH, true),
    MYSTIC("mystic_book", ProfessionType.MYSTIC, true),
    MINER("miner_book", ProfessionType.MINER, true),
    LUMBERJACK("lumberjack_book", ProfessionType.LUMBERJACK, true);

    private final String id;
    private final ProfessionType profession;
    private final boolean requiresClass;

    BookKind(String id, ProfessionType profession, boolean requiresClass) {
        this.id = id;
        this.profession = profession;
        this.requiresClass = requiresClass;
    }

    /** Путь предмета и часть ключей перевода. */
    public String id() {
        return id;
    }

    public ProfessionType profession() {
        return profession;
    }

    /**
     * true — книга считает только пока у игрока активен её класс.
     * Книга урожая считает всегда (так было с самого начала: убийства животных не зависят от класса).
     */
    public boolean requiresClass() {
        return requiresClass;
    }

    public Item item() {
        return switch (this) {
            case HARVEST -> ModItems.HARVEST_BOOK;
            case BLACKSMITH -> ModItems.BLACKSMITH_BOOK;
            case MYSTIC -> ModItems.MYSTIC_BOOK;
            case MINER -> ModItems.MINER_BOOK;
            case LUMBERJACK -> ModItems.LUMBERJACK_BOOK;
        };
    }

    /** Вид книги по предмету или null, если это не книга со статистикой. */
    public static BookKind of(ItemStack stack) {
        return stack.getItem() instanceof ClassBookItem book ? book.kind() : null;
    }

    /** Книга класса: для Фермера — книга урожая; null для NONE. */
    public static BookKind forProfession(ProfessionType type) {
        for (BookKind kind : values()) {
            if (kind.profession == type) {
                return kind;
            }
        }
        return null;
    }
}
