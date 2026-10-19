package org.sgx.classes.profession.skill;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.sgx.classes.Arz_classes;
import org.sgx.classes.profession.ProfessionData;

import java.util.List;

/**
 * Пересчитывает и переприменяет пассивные бонусы дерева навыков (SkillEffect.AttributeBonus)
 * на реальные атрибуты игрока. Вызывать при: входе игрока, смене класса, выборе подкласса,
 * level-up. Идемпотентно — можно звать сколько угодно раз подряд подряд.
 *
 * ВНИМАНИЕ (версионный риск, как и с ItemAttributeModifiers в WeaponSharpening): сигнатуры
 * AttributeInstance.removeModifier(Identifier)/addOrUpdateTransientModifier(...) не проверены
 * компиляцией под ваш срез — если не найдутся, пришлите ошибку.
 */
public final class SkillAttributeSync {

    private SkillAttributeSync() {
    }

    public static void reapply(ServerPlayer player, ProfessionData data) {
        AttributeMap attrs = player.getAttributes();

        // 1. Снимаем модификаторы ВСЕХ известных узлов дерева (не только разблокированных
        // сейчас) — целенаправленно по id, без предикат-версии removeModifiers, которой
        // может не быть в этой версии API.
        for (List<SkillDefinition> chain : SkillTrees.allChains()) {
            for (SkillDefinition skill : chain) {
                for (SkillEffect effect : skill.effects()) {
                    if (effect instanceof SkillEffect.AttributeBonus bonus && attrs.hasAttribute(bonus.attribute())) {
                        AttributeInstance instance = attrs.getInstance(bonus.attribute());
                        if (instance != null) {
                            instance.removeModifier(modifierId(skill));
                        }
                    }
                }
            }
        }

        // 2. Накладываем модификаторы для всех сейчас разблокированных узлов.
        for (SkillDefinition skill : data.unlockedSkills()) {
            for (SkillEffect effect : skill.effects()) {
                if (effect instanceof SkillEffect.AttributeBonus bonus && attrs.hasAttribute(bonus.attribute())) {
                    AttributeInstance instance = attrs.getInstance(bonus.attribute());
                    if (instance != null) {
                        instance.addOrUpdateTransientModifier(
                                new AttributeModifier(modifierId(skill), bonus.amount(), bonus.operation()));
                    }
                }
            }
        }
    }

    private static Identifier modifierId(SkillDefinition skill) {
        return Identifier.fromNamespaceAndPath(Arz_classes.MOD_ID, "skill_" + skill.id());
    }
}