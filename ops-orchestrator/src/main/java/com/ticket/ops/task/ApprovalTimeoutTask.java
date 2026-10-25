package com.ticket.ops.task;

import com.ticket.ops.gate.approval.ApprovalCallback;
import com.ticket.ops.gate.approval.ApprovalTimeoutScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * 定时检查审批超时，超时默认拒绝并等待 P2 接入升级通知。
 */
@Component
public class ApprovalTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(ApprovalTimeoutTask.class);

    private final ApprovalTimeoutScheduler scheduler;

    public ApprovalTimeoutTask(ApprovalTimeoutScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Scheduled(fixedDelayString = "${ops.agent.gate.approval-check-interval-ms:30000}")
    public void checkTimeouts() {
        List<ApprovalCallback> expired = scheduler.checkTimeouts(Instant.now());
        if (!expired.isEmpty()) {
            log.warn("审批超时自动拒绝 {} 条，等待人工升级", expired.size());
        }
    }
}
