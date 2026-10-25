package com.ticket.ops.gate.approval;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * P0 内存审批回调存储，后续替换为数据库 + Webhook 回调。
 */
public class ApprovalCallbackStore {

    private final Map<String, ApprovalCallback> callbacks = new ConcurrentHashMap<>();

    public ApprovalCallback save(ApprovalCallback callback) {
        callbacks.put(callback.callbackId(), callback);
        return callback;
    }

    public Optional<ApprovalCallback> find(String callbackId) {
        return Optional.ofNullable(callbacks.get(callbackId));
    }

    public List<ApprovalCallback> findExpired(Instant now) {
        return callbacks.values().stream()
                .filter(callback -> "PENDING".equals(callback.status()))
                .filter(callback -> callback.expiresAt().isBefore(now))
                .toList();
    }

    public void markTimeout(String callbackId) {
        callbacks.computeIfPresent(callbackId,
                (key, callback) -> callback.withStatus("TIMEOUT"));
    }

    public void markApproved(String callbackId) {
        callbacks.computeIfPresent(callbackId,
                (key, callback) -> callback.withStatus("APPROVED"));
    }

    public void markRejected(String callbackId) {
        callbacks.computeIfPresent(callbackId,
                (key, callback) -> callback.withStatus("REJECTED"));
    }

    public int size() {
        return callbacks.size();
    }
}
