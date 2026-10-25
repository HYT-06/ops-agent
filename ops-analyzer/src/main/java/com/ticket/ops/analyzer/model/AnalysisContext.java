package com.ticket.ops.analyzer.model;

import com.ticket.ops.common.model.AnalysisRequest;
import com.ticket.ops.common.model.EvidenceRef;
import com.ticket.ops.mcp.model.ToolSummary;

import java.util.List;

/**
 * 分析技能输入：事件 + 已收集证据 + 当前场景可见只读工具。
 */
public record AnalysisContext(
        AnalysisRequest request,
        List<EvidenceRef> evidenceRefs,
        List<ToolSummary> visibleTools) {

    public AnalysisContext {
        evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        visibleTools = visibleTools == null ? List.of() : List.copyOf(visibleTools);
    }

    public static AnalysisContext of(AnalysisRequest request) {
        return new AnalysisContext(request, List.of(), List.of());
    }
}
