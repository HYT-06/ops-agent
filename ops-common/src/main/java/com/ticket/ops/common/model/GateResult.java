package com.ticket.ops.common.model;

/**
 * 门禁判定结果，携带判定原因、风险等级与待执行动作。
 */
public record GateResult(
        GateDecision decision,
        String reason,
        RiskLevel riskLevel,
        SuggestedAction action) {

    public static GateResult noActionNeeded(String reason) {
        return new GateResult(GateDecision.NO_ACTION, reason, null, null);
    }

    public static GateResult reject(String reason) {
        return new GateResult(GateDecision.REJECTED, reason, RiskLevel.HIGH, null);
    }

    public static GateResult autoApprove(SuggestedAction action) {
        return new GateResult(GateDecision.AUTO_APPROVED, "低风险，自动放行", RiskLevel.LOW, action);
    }

    public static GateResult requireApproval(SuggestedAction action) {
        return new GateResult(GateDecision.REQUIRES_APPROVAL, "高风险，等待人工审批", RiskLevel.HIGH, action);
    }

    public boolean rejected() {
        return decision == GateDecision.REJECTED;
    }

    public boolean requiresApproval() {
        return decision == GateDecision.REQUIRES_APPROVAL;
    }

    public boolean autoApproved() {
        return decision == GateDecision.AUTO_APPROVED;
    }
}
