package org.sgx.classes.profession.skill;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;

public sealed interface SkillEffect {


    record AttributeBonus(Holder<Attribute> attribute, double amount,
                          AttributeModifier.Operation operation) implements SkillEffect {}

    record CustomScalar(String key, double value) implements SkillEffect {}

    record Ability(String abilityId) implements SkillEffect {}
}