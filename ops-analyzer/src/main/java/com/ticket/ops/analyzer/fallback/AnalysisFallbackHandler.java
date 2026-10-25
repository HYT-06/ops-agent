package com.ticket.ops.analyzer.fallback;

import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.AnalysisRequest;

/**
 * Agent 自身降级路径：LLM / MCP 异常时返回结构化失败报告，不拖垮运维流程。
 */
public class AnalysisFallbackHandler {

    public AnalysisReport fallback(AnalysisRequest request, Throwable cause) {
        String message = cause == null || cause.getMessage() == null
                ? "未知错误"
                : cause.getMessage();
        return AnalysisReport.failed(request.requestId(), request.scene(),
                "分析失败，建议人工排查：" + message);
    }
}
