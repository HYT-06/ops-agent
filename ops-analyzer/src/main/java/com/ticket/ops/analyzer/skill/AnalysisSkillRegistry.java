package com.ticket.ops.analyzer.skill;

import com.ticket.ops.common.model.SceneType;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 场景到分析技能的注册表，每个场景必须且只能有一个技能。
 */
public class AnalysisSkillRegistry {

    private final Map<SceneType, AnalysisSkill> skills = new EnumMap<>(SceneType.class);

    public AnalysisSkillRegistry(List<AnalysisSkill> skillList) {
        if (skillList == null) {
            return;
        }
        for (AnalysisSkill skill : skillList) {
            AnalysisSkill previous = skills.putIfAbsent(skill.supports(), skill);
            if (previous != null) {
                throw new IllegalArgumentException("场景 " + skill.supports()
                        + " 注册了多个分析技能: " + previous.getClass().getSimpleName()
                        + ", " + skill.getClass().getSimpleName());
            }
        }
    }

    public AnalysisSkill get(SceneType scene) {
        AnalysisSkill skill = skills.get(scene);
        if (skill == null) {
            throw new IllegalArgumentException("场景未注册分析技能: " + scene);
        }
        return skill;
    }
}
