package com.ticket.ops.gate;

import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.GateResult;
import com.ticket.ops.common.model.IncidentContext;
import com.ticket.ops.common.model.RiskLevel;
import com.ticket.ops.common.model.SuggestedAction;
import com.ticket.ops.gate.approval.ApprovalCallback;
import com.ticket.ops.gate.approval.ApprovalCallbackStore;
import com.ticket.ops.gate.approval.ApprovalNotifier;
import com.ticket.ops.gate.model.BoundCheckResult;
import com.ticket.ops.knowledge.audit.AuditLedger;
import com.ticket.ops.mcp.registry.ToolRegistry;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * 执行门禁（确定性代码，不经过 LLM）。
 * <p>
 * 硬约束：只接受结构化 AnalysisReport；三件套不完整、工具未授权、参数越界一律拒绝。
 */
public class ExecutionGate {

    private final ToolRegistry writeRegistry;
    private final ParameterBoundValidator boundValidator;
    private final RiskClassifier riskClassifier;
    private final ApprovalNotifier approvalNotifier;
    private final ApprovalCallbackStore callbackStore;
    private final AuditLedger auditLedger;
    private final int approvalTimeoutMinutes;

    public ExecutionGate(ToolRegistry writeRegistry,
                         ParameterBoundValidator boundValidator,
                         RiskClassifier riskClassifier,
                         ApprovalNotifier approvalNotifier,
                         ApprovalCallbackStore callbackStore,
                         AuditLedger auditLedger,
                         int approvalTimeoutMinutes) {
        this.writeRegistry = Objects.requireNonNull(writeRegistry, "writeRegistry 不能为空");
        this.boundValidator = boundValidator;
        this.riskClassifier = riskClassifier;
        this.approvalNotifier = approvalNotifier;
        this.callbackStore = callbackStore;
        this.auditLedger = auditLedger;
        this.approvalTimeoutMinutes = Math.max(1, approvalTimeoutMinutes);
    }

    public GateResult check(AnalysisReport report, IncidentContext context) {
        if (report == null) {
            return GateResult.reject("分析报告缺失");
        }
        SuggestedAction action = report.suggestedAction();
        if (action == null) {
            return GateResult.noActionNeeded("仅诊断，无执行建议");
        }
        if (!action.isComplete()) {
            return GateResult.reject("三件套不完整：需要 toolName / params / rollbackPoint / idempotencyKey");
        }
        if (!writeRegistry.contains(action.toolName())) {
            return GateResult.reject("工具未授权：" + action.toolName());
        }

        BoundCheckResult boundCheck = boundValidator.validate(action.toolName(), action.params());
        if (!boundCheck.passed()) {
            return GateResult.reject("参数越界：" + boundCheck.reason());
        }

        RiskLevel riskLevel = riskClassifier.classify(action, context);
        auditLedger.recordGateChecked(context.requestId(), report, riskLevel);

        if (riskLevel == RiskLevel.LOW) {
            return GateResult.autoApprove(action);
        }

        String callbackId = UUID.randomUUID().toString();
        callbackStore.save(ApprovalCallback.pending(callbackId, context.requestId(), action,
                Instant.now().plus(approvalTimeoutMinutes, ChronoUnit.MINUTES)));
        approvalNotifier.notify(action, report, context, callbackId);
        return GateResult.requireApproval(action);
    }
}
