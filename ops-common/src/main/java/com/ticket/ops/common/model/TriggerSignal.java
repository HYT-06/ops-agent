package com.ticket.ops.common.model;

import com.ticket.ops.common.util.Maps;
import com.ticket.ops.common.util.Text;

import java.util.Map;

/**
 * 触发信号摘要，用于在 AnalysisRequest 中保留原始告警上下文。
 */
public record TriggerSignal(String ruleId, String summary, Map<String, Object> metrics) {

    public TriggerSignal {
        ruleId = Text.defaultIfBlank(ruleId, "manual");
        summary = summary == null ? "" : summary;
        metrics = Maps.immutable(metrics);
    }

    public static TriggerSignal manual(String summary) {
        return new TriggerSignal("manual", summary, Map.of());
    }
}
