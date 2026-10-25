package com.ticket.ops.knowledge.governance;

import com.ticket.ops.common.model.RiskLevel;
import com.ticket.ops.knowledge.knowledge.KnowledgeEntry;

/**
 * 默认风险策略：涉及 DLQ 重放、SQL DDL、JVM 调参的知识一律高风险。
 */
public class DefaultKnowledgeRiskPolicy implements KnowledgeRiskPolicy {

    @Override
    public RiskLevel classify(KnowledgeEntry entry) {
        String tool = entry.suggestedToolName();
        if (tool == null || tool.isBlank()) {
            return RiskLevel.HIGH;
        }
        if (tool.startsWith("dlq.replay")
                || tool.startsWith("sql.")
                || tool.startsWith("jvm.")) {
            return RiskLevel.HIGH;
        }
        return RiskLevel.LOW;
    }
}
