package com.ticket.ops.knowledge.governance;

import com.ticket.ops.knowledge.knowledge.KnowledgeEntry;

/**
 * 高风险知识晋升审批出口，P2 接入企业微信 / 钉钉 Webhook。
 */
public interface KnowledgeApprovalSink {

    void sendKnowledgeApproval(KnowledgeEntry entry);
}
