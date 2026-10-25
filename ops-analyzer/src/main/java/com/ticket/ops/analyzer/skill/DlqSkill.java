package com.ticket.ops.analyzer.skill;

import com.ticket.ops.analyzer.model.AnalysisContext;
import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.SceneType;

/**
 * DLQ 分析技能占位。P3 接入死信聚类、订单状态与幂等记录。
 */
public class DlqSkill implements AnalysisSkill {

    @Override
    public SceneType supports() {
        return SceneType.DLQ;
    }

    @Override
    public AnalysisReport analyze(AnalysisContext context) {
        return AnalysisReport.placeholder(context.request(),
                "P0 占位：DLQ 分析技能尚未接入 LLM 与死信/订单/幂等证据链");
    }
}
