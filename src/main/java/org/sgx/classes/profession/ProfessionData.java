package org.sgx.classes.profession;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.sgx.classes.profession.skill.SkillDefinition;
import org.sgx.classes.profession.skill.SkillEffect;
import org.sgx.classes.profession.skill.SkillTrees;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Прогресс игрока. Смена класса ПОЛНОСТЬЮ сбрасывает прогресс прежнего — level/xp,
 * а теперь ещё и unlockedSkillCount, не карта по всем ProfessionType.
 *
 * Очки навыков: каждый уровень даёт 1 очко. Выбор подкласса (10 уровень) тоже стоит 1 очко.
 * Итого: 9 узлов главной цепочки + выбор подкласса + 10 узлов ветки (11..20) = 20 очков.
 * База подкласса (SkillTrees.subclassBase) действует сразу после выбора подкласса.
 */
public final class ProfessionData {

    public static final int SUBCLASS_BRANCH_LEVEL = 10;

    /**
     * Уровень, на котором опыт перестаёт засчитываться, пока игроку не открыт выбор подкласса
     * (и подкласс не выбран). Чтобы поставить гейт на 10 уровень — убрать "- 1".
     */
    public static final int XP_GATE_LEVEL = SUBCLASS_BRANCH_LEVEL - 1;

    public static final Codec<ProfessionData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ProfessionType.CODEC.fieldOf("current_profession").forGetter(d -> d.currentProfession),
            Codec.INT.fieldOf("level").forGetter(d -> d.level),
            Codec.INT.fieldOf("xp").forGetter(d -> d.xp),
            SubclassType.CODEC.optionalFieldOf("current_subclass").forGetter(d -> Optional.ofNullable(d.currentSubclass)),
            Codec.INT.optionalFieldOf("unlocked_skill_count", 0).forGetter(d -> d.unlockedSkillCount),
            Codec.DOUBLE.optionalFieldOf("local_xp_multiplier", 1.0).forGetter(d -> d.localXpMultiplier),
            Codec.BOOL.optionalFieldOf("subclass_unlocked", false).forGetter(d -> d.subclassUnlocked)
    ).apply(instance, ProfessionData::new));

    private ProfessionType currentProfession;
    private int level;
    private int xp;
    private SubclassType currentSubclass;
    private int unlockedSkillCount;
    private double localXpMultiplier;
    /** Открыт ли игроку выбор подкласса (включается админской командой /profession unlock). */
    private boolean subclassUnlocked;

    public ProfessionData() {
        this(ProfessionType.NONE, 0, 0, Optional.empty(), 0, 1.0, false);
    }

    private ProfessionData(ProfessionType currentProfession, int level, int xp,
                           Optional<SubclassType> currentSubclass, int unlockedSkillCount,
                           double localXpMultiplier, boolean subclassUnlocked) {
        this.currentProfession = currentProfession;
        this.level = level;
        this.xp = xp;
        this.currentSubclass = currentSubclass.orElse(null);
        this.unlockedSkillCount = unlockedSkillCount;
        this.localXpMultiplier = localXpMultiplier;
        this.subclassUnlocked = subclassUnlocked;
    }

    public ProfessionType currentProfession() {
        return currentProfession;
    }

    /** Смена класса — ПОЛНОСТЬЮ сбрасывает уровень/опыт/подкласс/купленные навыки прежнего.
     * Флаг subclassUnlocked НЕ сбрасывается: это награда за ивент, а не прогресс класса. */
    public void setCurrentProfession(ProfessionType type) {
        if (type == currentProfession) {
            return;
        }
        this.currentProfession = type;
        this.level = 0;
        this.xp = 0;
        this.currentSubclass = null;
        this.unlockedSkillCount = 0;
    }

    public int level() {
        return level;
    }

    public int xp() {
        return xp;
    }

    public double localXpMultiplier() {
        return localXpMultiplier;
    }

    public void setLocalXpMultiplier(double multiplier) {
        this.localXpMultiplier = multiplier;
    }

    /**
     * true, если опыт сейчас не засчитывается: игрок дошёл до XP_GATE_LEVEL, подкласс не выбран
     * и выбор подкласса ему ещё не открыт.
     */
    public boolean xpBlockedBySubclassGate() {
        return currentProfession != ProfessionType.NONE
                && level >= XP_GATE_LEVEL
                && currentSubclass == null
                && !subclassUnlocked;
    }

    /** Добавляет опыт активному классу, возвращает true при level-up. */
    public boolean addXp(int amount) {
        if (currentProfession == ProfessionType.NONE || level >= ProfessionType.MAX_LEVEL) {
            return false;
        }
        if (xpBlockedBySubclassGate()) {
            return false; // опыт получен, но не засчитывается, пока не открыт выбор подкласса
        }
        xp += amount;
        boolean leveledUp = false;
        int needed = currentProfession.xpToNextLevel(level);
        while (xp >= needed && level < ProfessionType.MAX_LEVEL) {
            xp -= needed;
            level++;
            leveledUp = true;
            needed = currentProfession.xpToNextLevel(level);
        }
        return leveledUp;
    }

    /** Отнимает опыт (для админ-команд), при нехватке опускает уровень. Ниже 0 уровня/0 опыта не уходит.
     * Возвращает true, если уровень упал. */
    public boolean removeXp(int amount) {
        if (currentProfession == ProfessionType.NONE || amount <= 0) {
            return false;
        }
        boolean levelDropped = false;
        int remaining = amount;
        while (remaining > 0) {
            if (xp >= remaining) {
                xp -= remaining;
                remaining = 0;
            } else {
                remaining -= xp;
                xp = 0;
                if (level <= 0) {
                    break;
                }
                level--;
                levelDropped = true;
                xp = currentProfession.xpToNextLevel(level); // полная полоска предыдущего уровня
            }
        }
        if (levelDropped) {
            clampSkillsToLevel();
        }
        return levelDropped;
    }

    /** Напрямую поднимает уровень текущего класса, минуя опыт — для отладочных предметов и админ-команд. */
    public boolean addLevel(int amount) {
        if (currentProfession == ProfessionType.NONE || level >= ProfessionType.MAX_LEVEL) {
            return false;
        }
        level = Math.min(ProfessionType.MAX_LEVEL, level + amount);
        if (level >= ProfessionType.MAX_LEVEL) {
            xp = 0;
        }
        return true;
    }

    /** Напрямую снижает уровень (опыт внутри уровня обнуляется), лишние купленные навыки снимаются. */
    public boolean removeLevel(int amount) {
        if (currentProfession == ProfessionType.NONE || level <= 0 || amount <= 0) {
            return false;
        }
        level = Math.max(0, level - amount);
        xp = 0;
        clampSkillsToLevel();
        return true;
    }

    /**
     * Каждый уровень даёт одно очко, выбор подкласса тоже стоит очко, поэтому
     * (купленные навыки + выбор подкласса) не могут превышать уровень.
     * Если уровень упал ниже 10 — подкласс сбрасывается (его выбор невозможен без 10 уровня).
     */
    private void clampSkillsToLevel() {
        if (level < SUBCLASS_BRANCH_LEVEL) {
            currentSubclass = null;
        }
        int cap = level - (currentSubclass != null ? 1 : 0);
        if (unlockedSkillCount > cap) {
            unlockedSkillCount = Math.max(0, cap);
        }
    }

    public boolean isMaxLevel() {
        return level >= ProfessionType.MAX_LEVEL;
    }

    // --- Подкласс: выбор на SUBCLASS_BRANCH_LEVEL (10), стоит 1 очко навыков, необратим ---

    public SubclassType currentSubclass() {
        return currentSubclass;
    }

    public boolean subclassUnlocked() {
        return subclassUnlocked;
    }

    public void setSubclassUnlocked(boolean unlocked) {
        this.subclassUnlocked = unlocked;
    }

    public boolean needsSubclassSelection() {
        return currentProfession != ProfessionType.NONE
                && level >= SUBCLASS_BRANCH_LEVEL
                && currentSubclass == null;
    }

    /** Обычный выбор игроком: требует, чтобы админ/ивент открыл выбор подкласса. */
    public boolean chooseSubclass(SubclassType subclass) {
        return chooseSubclass(subclass, false);
    }

    /**
     * bypassLock = true — для админской команды: игнорирует флаг блокировки и проверку очков.
     * Для обычного игрока: нужен 10 уровень, открытая блокировка, все узлы главной цепочки
     * куплены и свободное очко навыка (выбор подкласса его тратит).
     */
    public boolean chooseSubclass(SubclassType subclass, boolean bypassLock) {
        if (subclass.parent() != currentProfession || level < SUBCLASS_BRANCH_LEVEL || currentSubclass != null) {
            return false;
        }
        if (!bypassLock) {
            if (!subclassUnlocked) {
                return false;
            }
            if (unlockedSkillCount < SkillTrees.mainChain(currentProfession).size()) {
                return false;
            }
            if (availableSkillPoints() <= 0) {
                return false;
            }
        }
        this.currentSubclass = subclass;
        return true;
    }

    // --- Дерево навыков ---

    private List<SkillDefinition> fullChain() {
        List<SkillDefinition> combined = new ArrayList<>();
        if (currentProfession == ProfessionType.NONE) {
            return combined;
        }
        combined.addAll(SkillTrees.mainChain(currentProfession));
        if (currentSubclass != null) {
            combined.addAll(SkillTrees.branchChain(currentSubclass));
        }
        return combined;
    }

    public int unlockedSkillCount() {
        return unlockedSkillCount;
    }

    /** Уровень минус купленные узлы минус 1 очко за выбранный подкласс. */
    public int availableSkillPoints() {
        return Math.max(0, level - unlockedSkillCount - (currentSubclass != null ? 1 : 0));
    }

    public boolean unlockNextSkill() {
        List<SkillDefinition> chain = fullChain();
        if (unlockedSkillCount >= chain.size() || availableSkillPoints() <= 0) {
            return false;
        }
        SkillDefinition next = chain.get(unlockedSkillCount);
        if (level < next.requiredLevel()) {
            return false;
        }
        unlockedSkillCount++;
        return true;
    }

    public List<SkillDefinition> unlockedSkills() {
        List<SkillDefinition> chain = fullChain();
        int count = Math.min(unlockedSkillCount, chain.size());
        return new ArrayList<>(chain.subList(0, count));
    }

    /** Все действующие эффекты: купленные узлы + база выбранного подкласса. */
    public List<SkillEffect> activeEffects() {
        List<SkillEffect> all = new ArrayList<>();
        for (SkillDefinition s : unlockedSkills()) {
            all.addAll(s.effects());
        }
        if (currentSubclass != null) {
            all.addAll(SkillTrees.subclassBase(currentSubclass));
        }
        return all;
    }

    public double customScalarTotal(String key) {
        double sum = 0;
        for (SkillEffect e : activeEffects()) {
            if (e instanceof SkillEffect.CustomScalar cs && cs.key().equals(key)) sum += cs.value();
        }
        return sum;
    }

    public boolean hasAbility(String abilityId) {
        for (SkillEffect e : activeEffects()) {
            if (e instanceof SkillEffect.Ability a && a.abilityId().equals(abilityId)) return true;
        }
        return false;
    }
}