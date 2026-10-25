package com.ticket.ops.analyzer.skill;

import com.ticket.ops.analyzer.model.AnalysisContext;
import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.SceneType;

/**
 * 场景分析技能。
 * <p>
 * 注意：分析 Agent 的依赖中不得出现写 Registry，写权限只能由门禁持有。
 */
public interface AnalysisSkill {

    SceneType supports();

    AnalysisReport analyze(AnalysisContext context);
}
