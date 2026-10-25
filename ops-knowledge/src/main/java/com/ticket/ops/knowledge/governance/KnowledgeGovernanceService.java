package com.ticket.ops.knowledge.governance;

import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.IncidentContext;
import com.ticket.ops.common.model.RiskLevel;
import com.ticket.ops.common.model.RootCauseCandidate;
import com.ticket.ops.common.model.VerificationResult;
import com.ticket.ops.knowledge.audit.AuditLedger;
import com.ticket.ops.knowledge.knowledge.KnowledgeEntry;
import com.ticket.ops.knowledge.knowledge.KnowledgeLayer;
import com.ticket.ops.knowledge.repository.KnowledgeRepository;

import java.util.List;

/**
 * 知识写入治理：
 * 分析结论先入候选池；验证通过后低风险且证据充分才自动晋升，高风险走人工门禁。
 */
public class KnowledgeGovernanceService {

    private final KnowledgeRepository repository;
    private final KnowledgeConflictDetector conflictDetector;
    private final KnowledgeRiskPolicy riskPolicy;
    private final KnowledgeApprovalSink approvalSink;
    private final AuditLedger auditLedger;
    private final int autoPromoteThreshold;
    private final double autoPromoteConfidence;

    public KnowledgeGovernanceService(KnowledgeRepository repository,
                                      KnowledgeConflictDetector conflictDetector,
                                      KnowledgeRiskPolicy riskPolicy,
                                      KnowledgeApprovalSink approvalSink,
                                      AuditLedger auditLedger,
                                      int autoPromoteThreshold,
                                      double autoPromoteConfidence) {
        this.repository = repository;
        this.conflictDetector = conflictDetector;
        this.riskPolicy = riskPolicy;
        this.approvalSink = approvalSink;
        this.auditLedger = auditLedger;
        this.autoPromoteThreshold = Math.max(1, autoPromoteThreshold);
        this.autoPromoteConfidence = Math.max(0.0, Math.min(1.0, autoPromoteConfidence));
    }

    /**
     * 分析结论写入候选池。冲突检测不可跳过。
     */
    public KnowledgeEntry submitCandidate(AnalysisReport report, IncidentContext context) {
        if (report.rootCauses().isEmpty()) {
            return null;
        }
        RootCauseCandidate top = report.rootCauses().get(0);
        String toolName = report.suggestedAction() == null ? "" : report.suggestedAction().toolName();
        String boundary = "resource=" + context.resourceId() + ", scene=" + context.scene();
        KnowledgeEntry candidate = KnowledgeEntry.candidate(
                0, report.scene(), top.cause(), top.cause(), boundary,
                toolName, top.confidence(), context.requestId());

        ConflictCheckResult conflict = conflictDetector.check(candidate);
        KnowledgeEntry saved = repository.save(candidate);
        auditLedger.recordKnowledgeWrite(saved,
                conflict.conflict() ? "ANALYSIS_AGENT_CONFLICT" : "ANALYSIS_AGENT");
        return saved;
    }

    /**
     * 验证通过后触发晋升流程。
     */
    public void onVerified(IncidentContext context, VerificationResult verification) {
        if (verification == null || !verification.passed()) {
            return;
        }
        List<KnowledgeEntry> candidates = repository.findCandidatesByIncident(context.requestId());
        for (KnowledgeEntry candidate : candidates) {
            KnowledgeEntry updated = repository.incrementVerified(candidate.id()).orElse(candidate);
            RiskLevel risk = riskPolicy.classify(updated);
            if (risk == RiskLevel.HIGH) {
                approvalSink.sendKnowledgeApproval(updated);
                continue;
            }
            if (conflictDetector.check(updated).conflict()) {
                approvalSink.sendKnowledgeApproval(updated);
                continue;
            }
            boolean enoughEvidence = updated.verifiedCount() >= autoPromoteThreshold
                    && updated.confidence() >= autoPromoteConfidence
                    && updated.applicableBoundary() != null
                    && !updated.applicableBoundary().isBlank();
            if (enoughEvidence) {
                repository.promote(updated.id(), KnowledgeLayer.VERIFIED)
                        .ifPresent(promoted -> auditLedger.recordKnowledgePromotion(promoted, "AUTO"));
            }
        }
    }
}
