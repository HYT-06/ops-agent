package com.ticket.ops.gate.approval;

import com.ticket.ops.common.model.SuggestedAction;
import com.ticket.ops.common.util.Maps;

import java.time.Instant;
import java.util.Map;

/**
 * 审批回调记录。
 */
public record ApprovalCallback(
        String callbackId,
        String requestId,
        SuggestedAction action,
        Instant expiresAt,
        String status) {

    public ApprovalCallback {
        status = status == null ? "PENDING" : status;
    }

    public static ApprovalCallback pending(String callbackId, String requestId,
                                           SuggestedAction action, Instant expiresAt) {
        return new ApprovalCallback(callbackId, requestId, action, expiresAt, "PENDING");
    }

    public ApprovalCallback withStatus(String newStatus) {
        Map<String, Object> params = Maps.immutable(action.params());
        SuggestedAction copied = new SuggestedAction(action.toolName(), params,
                action.rollbackPoint(), action.idempotencyKey());
        return new ApprovalCallback(callbackId, requestId, copied, expiresAt, newStatus);
    }
}
