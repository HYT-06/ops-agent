package com.ticket.ops.knowledge.governance;

import com.ticket.ops.common.model.RiskLevel;
import com.ticket.ops.knowledge.knowledge.KnowledgeEntry;

/**
 * 知识条目的风险策略，与执行门禁的风险分类相互独立。
 */
public interface KnowledgeRiskPolicy {

    RiskLevel classify(KnowledgeEntry entry);
}
