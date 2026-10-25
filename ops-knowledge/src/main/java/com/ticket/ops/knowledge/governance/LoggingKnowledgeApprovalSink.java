package com.ticket.ops.knowledge.governance;

import com.ticket.ops.knowledge.knowledge.KnowledgeEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * P0 审批出口：写日志，后续替换为 Webhook。
 */
public class LoggingKnowledgeApprovalSink implements KnowledgeApprovalSink {

    private static final Logger log = LoggerFactory.getLogger(LoggingKnowledgeApprovalSink.class);

    @Override
    public void sendKnowledgeApproval(KnowledgeEntry entry) {
        log.info("知识晋升审批请求: knowledgeId={}, scene={}, tool={}, content={}",
                entry.id(), entry.scene(), entry.suggestedToolName(), entry.content());
    }
}
