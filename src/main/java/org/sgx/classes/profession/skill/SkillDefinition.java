package org.sgx.classes.profession.skill;

import java.util.List;

public record SkillDefinition(
        String id,
        int requiredLevel,
        String nameKey,
        String descKey,
        List<SkillEffect> effects
) {}