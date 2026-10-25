package com.ticket.ops.knowledge.audit;

import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.RiskLevel;
import com.ticket.ops.common.model.SuggestedAction;
import com.ticket.ops.knowledge.knowledge.KnowledgeEntry;

import java.util.List;

/**
 * 审计账本接口。所有写操作只追加，禁止 UPDATE / DELETE。
 */
public interface AuditLedger {

    void recordGateChecked(String requestId, AnalysisReport report, RiskLevel level);

    void recordExecution(String requestId, SuggestedAction action, String result);

    void recordExecutionFailure(String requestId, SuggestedAction action, Throwable error);

    void recordKnowledgeWrite(KnowledgeEntry entry, String actor);

    void recordKnowledgePromotion(KnowledgeEntry entry, String actor);

    boolean existsIdempotencyKey(String idempotencyKey);

    List<AuditEvent> findByRequestId(String requestId);
}
