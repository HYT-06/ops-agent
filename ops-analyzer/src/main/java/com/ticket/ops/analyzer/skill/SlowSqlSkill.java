package com.ticket.ops.analyzer.skill;

import com.ticket.ops.analyzer.model.AnalysisContext;
import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.SceneType;

/**
 * 慢 SQL 分析技能占位。P1 接入慢日志、EXPLAIN 与统计信息。
 */
public class SlowSqlSkill implements AnalysisSkill {

    @Override
    public SceneType supports() {
        return SceneType.SLOW_SQL;
    }

    @Override
    public AnalysisReport analyze(AnalysisContext context) {
        return AnalysisReport.placeholder(context.request(),
                "P0 占位：慢 SQL 分析技能尚未接入 LLM 与慢日志/执行计划证据链");
    }
}
