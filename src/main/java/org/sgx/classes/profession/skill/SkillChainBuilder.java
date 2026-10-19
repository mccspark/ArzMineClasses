package org.sgx.classes.profession.skill;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.ArrayList;
import java.util.List;

/** Собирает линейную цепочку узлов уровень-за-уровнем, без дырок и дублей. */
public final class SkillChainBuilder {

    private final List<SkillDefinition> nodes = new ArrayList<>();
    private int nextLevel;

    public SkillChainBuilder(int startLevel) {
        this.nextLevel = startLevel;
    }

    public SkillChainBuilder scalar(String idPrefix, int count, String key, double perNode, String nameKey) {
        for (int i = 0; i < count; i++) {
            nodes.add(new SkillDefinition(idPrefix + "_" + nextLevel, nextLevel, nameKey,
                    nameKey + ".desc", List.of(new SkillEffect.CustomScalar(key, perNode))));
            nextLevel++;
        }
        return this;
    }

    public SkillChainBuilder attribute(String idPrefix, int count, Holder<Attribute> attr, double perNode,
                                       AttributeModifier.Operation op, String nameKey) {
        for (int i = 0; i < count; i++) {
            nodes.add(new SkillDefinition(idPrefix + "_" + nextLevel, nextLevel, nameKey,
                    nameKey + ".desc", List.of(new SkillEffect.AttributeBonus(attr, perNode, op))));
            nextLevel++;
        }
        return this;
    }

    public SkillChainBuilder milestone(String id, String abilityId, String nameKey) {
        nodes.add(new SkillDefinition(id, nextLevel, nameKey, nameKey + ".desc",
                List.of(new SkillEffect.Ability(abilityId))));
        nextLevel++;
        return this;
    }

    /** Узел с ДВУМЯ ability-эффектами разом (например "Эпическое + Щит с шипами" одним кликом). */
    public SkillChainBuilder dual(String id, String abilityIdA, String abilityIdB, String nameKey) {
        nodes.add(new SkillDefinition(id, nextLevel, nameKey, nameKey + ".desc",
                List.of(new SkillEffect.Ability(abilityIdA), new SkillEffect.Ability(abilityIdB))));
        nextLevel++;
        return this;
    }

    /** Узел с числовым бонусом + milestone-способностью разом (например "+3% заточки + Мифическое"). */
    public SkillChainBuilder scalarAndMilestone(String id, String key, double value, String abilityId, String nameKey) {
        nodes.add(new SkillDefinition(id, nextLevel, nameKey, nameKey + ".desc",
                List.of(new SkillEffect.CustomScalar(key, value), new SkillEffect.Ability(abilityId))));
        nextLevel++;
        return this;
    }

    /** Чередует scalar-узлы двух треков 1-через-1 (когда у класса два continuous-бонуса). */
    public SkillChainBuilder interleavedScalar(String idPrefix, int count, String keyA, double perA, String nameKeyA,
                                               String keyB, double perB, String nameKeyB) {
        for (int i = 0; i < count; i++) {
            boolean useA = i % 2 == 0;
            nodes.add(new SkillDefinition(idPrefix + "_" + nextLevel, nextLevel,
                    useA ? nameKeyA : nameKeyB, (useA ? nameKeyA : nameKeyB) + ".desc",
                    List.of(new SkillEffect.CustomScalar(useA ? keyA : keyB, useA ? perA : perB))));
            nextLevel++;
        }
        return this;
    }

    public List<SkillDefinition> build(int expectedEndExclusive) {
        if (nextLevel != expectedEndExclusive) {
            throw new IllegalStateException("Цепочка обрывается на уровне " + nextLevel
                    + ", ожидался конец " + expectedEndExclusive + " — проверьте сумму count'ов.");
        }
        return List.copyOf(nodes);
    }
}