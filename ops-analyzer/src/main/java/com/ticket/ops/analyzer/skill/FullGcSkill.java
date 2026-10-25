package com.ticket.ops.analyzer.skill;

import com.ticket.ops.analyzer.model.AnalysisContext;
import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.SceneType;

/**
 * FullGC 分析技能占位。P1 接入只读 MCP 证据与 Qwen-Max 推理。
 */
public class FullGcSkill implements AnalysisSkill {

    @Override
    public SceneType supports() {
        return SceneType.FULLGC;
    }

    @Override
    public AnalysisReport analyze(AnalysisContext context) {
        return AnalysisReport.placeholder(context.request(),
                "P0 占位：FullGC 分析技能尚未接入 LLM 与 GC/JVM 证据链");
    }
}
