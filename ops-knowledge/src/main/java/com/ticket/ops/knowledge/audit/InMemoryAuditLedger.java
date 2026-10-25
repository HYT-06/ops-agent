package com.ticket.ops.knowledge.audit;

import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.RiskLevel;
import com.ticket.ops.common.model.SuggestedAction;
import com.ticket.ops.knowledge.knowledge.KnowledgeEntry;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * P0 内存审计账本。后续替换为 MySQL append-only 表，接口保持不变。
 */
public class InMemoryAuditLedger implements AuditLedger {

    private final AtomicLong idSequence = new AtomicLong();
    private final Map<String, CopyOnWriteArrayList<AuditEvent>> eventsByRequest = new ConcurrentHashMap<>();
    private final Set<String> idempotencyKeys = ConcurrentHashMap.newKeySet();

    @Override
    public void recordGateChecked(String requestId, AnalysisReport report, RiskLevel level) {
        String payload = "scene=" + report.scene()
                + ", confidence=" + report.confidence()
                + ", riskLevel=" + level
                + ", hasAction=" + (report.suggestedAction() != null);
        append(requestId, "GATE", "EXECUTION_GATE", null, payload);
    }

    @Override
    public void recordExecution(String requestId, SuggestedAction action, String result) {
        if (!action.idempotencyKey().isBlank()) {
            idempotencyKeys.add(action.idempotencyKey());
        }
        append(requestId, "EXECUTION", "EXECUTOR", action.idempotencyKey(),
                "tool=" + action.toolName() + ", result=" + result);
    }

    @Override
    public void recordExecutionFailure(String requestId, SuggestedAction action, Throwable error) {
        append(requestId, "EXECUTION_FAILED", "EXECUTOR", action.idempotencyKey(),
                "tool=" + action.toolName() + ", error=" + (error == null ? "unknown" : error.getMessage()));
    }

    @Override
    public void recordKnowledgeWrite(KnowledgeEntry entry, String actor) {
        append(knowledgeRequestId(entry), "KNOWLEDGE_WRITE", actor, null,
                "knowledgeId=" + entry.id() + ", scene=" + entry.scene());
    }

    @Override
    public void recordKnowledgePromotion(KnowledgeEntry entry, String actor) {
        append(knowledgeRequestId(entry), "KNOWLEDGE_PROMOTION", actor, null,
                "knowledgeId=" + entry.id() + ", layer=" + entry.layer());
    }

    private String knowledgeRequestId(KnowledgeEntry entry) {
        return entry.sourceIncidentIds().isEmpty() ? "knowledge" : entry.sourceIncidentIds().get(0);
    }

    @Override
    public boolean existsIdempotencyKey(String idempotencyKey) {
        return idempotencyKey != null && !idempotencyKey.isBlank()
                && idempotencyKeys.contains(idempotencyKey);
    }

    @Override
    public List<AuditEvent> findByRequestId(String requestId) {
        return Optional.ofNullable(eventsByRequest.get(requestId))
                .map(List::copyOf)
                .orElseGet(List::of);
    }

    private void append(String requestId, String eventType, String actor,
                        String idempotencyKey, String payload) {
        String key = requestId == null || requestId.isBlank() ? "unknown" : requestId;
        AuditEvent event = new AuditEvent(
                idSequence.incrementAndGet(),
                key,
                eventType,
                actor,
                idempotencyKey,
                payload,
                Instant.now());
        eventsByRequest.computeIfAbsent(key, ignored -> new CopyOnWriteArrayList<>()).add(event);
    }
}
