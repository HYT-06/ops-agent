package com.ticket.ops.gate.approval;

import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.IncidentContext;
import com.ticket.ops.common.model.SuggestedAction;

/**
 * 高风险变更审批通知出口，P2 接入企业微信 / 钉钉 Webhook。
 */
public interface ApprovalNotifier {

    void notify(SuggestedAction action, AnalysisReport report, IncidentContext context, String callbackId);
}
