package com.ticket.ops.gate.approval;

import java.time.Instant;
import java.util.List;

/**
 * 审批超时检查。超时默认拒绝并升级人工，由编排层定时调用。
 */
public class ApprovalTimeoutScheduler {

    private final ApprovalCallbackStore callbackStore;

    public ApprovalTimeoutScheduler(ApprovalCallbackStore callbackStore) {
        this.callbackStore = callbackStore;
    }

    public List<ApprovalCallback> checkTimeouts(Instant now) {
        List<ApprovalCallback> expired = callbackStore.findExpired(now);
        expired.forEach(callback -> callbackStore.markTimeout(callback.callbackId()));
        return expired;
    }
}
