package com.ticket.ops.knowledge.audit;

import java.time.Instant;

/**
 * 审计事件。账本 append-only：只追加，不修改、不删除。
 */
public record AuditEvent(
        long id,
        String requestId,
        String eventType,
        String actor,
        String idempotencyKey,
        String payload,
        Instant createdAt) {
}
