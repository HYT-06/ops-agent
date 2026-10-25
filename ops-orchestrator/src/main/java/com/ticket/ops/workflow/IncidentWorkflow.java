package com.ticket.ops.workflow;

import com.ticket.ops.analyzer.fallback.AnalysisFallbackHandler;
import com.ticket.ops.analyzer.model.AnalysisContext;
import com.ticket.ops.analyzer.skill.AnalysisSkillRegistry;
import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.AnalysisRequest;
import com.ticket.ops.common.model.GateResult;
import com.ticket.ops.common.model.IncidentContext;
import com.ticket.ops.common.model.IncidentState;
import com.ticket.ops.common.model.VerificationResult;
import com.ticket.ops.gate.ActionExecutor;
import com.ticket.ops.gate.ExecutionGate;
import com.ticket.ops.knowledge.governance.KnowledgeGovernanceService;
import com.ticket.ops.mcp.model.ExecutionResult;

/**
 * P0 顺序编排骨架。
 * <p>
 * P1 将替换为 Spring AI Alibaba Graph 状态机，节点与状态枚举保持一致。
 */
public class IncidentWorkflow {

    private final AnalysisSkillRegistry skillRegistry;
    private final AnalysisFallbackHandler fallbackHandler;
    private final ExecutionGate executionGate;
    private final ActionExecutor actionExecutor;
    private final DeterministicVerifier verifier;
    private final KnowledgeGovernanceService governanceService;

    public IncidentWorkflow(AnalysisSkillRegistry skillRegistry,
                            AnalysisFallbackHandler fallbackHandler,
                            ExecutionGate executionGate,
                            ActionExecutor actionExecutor,
                            DeterministicVerifier verifier,
                            KnowledgeGovernanceService governanceService) {
        this.skillRegistry = skillRegistry;
        this.fallbackHandler = fallbackHandler;
        this.executionGate = executionGate;
        this.actionExecutor = actionExecutor;
        this.verifier = verifier;
        this.governanceService = governanceService;
    }

    public IncidentRecord advance(IncidentRecord incident) {
        AnalysisRequest request = incident.request();
        IncidentContext context = IncidentContext.of(request);

        AnalysisReport report;
        try {
            report = skillRegistry.get(request.scene()).analyze(AnalysisContext.of(request));
        } catch (RuntimeException error) {
            report = fallbackHandler.fallback(request, error);
        }
        IncidentRecord analyzing = incident.withReport(report, IncidentState.ANALYZING);

        GateResult gateResult = executionGate.check(report, context);
        IncidentRecord gateChecked = analyzing.withGateResult(gateResult, IncidentState.GATE_CHECK);

        if (gateResult.decision() == com.ticket.ops.common.model.GateDecision.NO_ACTION) {
            return gateChecked.withState(IncidentState.ARCHIVED);
        }
        if (gateResult.rejected()) {
            return gateChecked.withState(IncidentState.FAILED);
        }
        if (gateResult.requiresApproval()) {
            return gateChecked.withState(IncidentState.APPROVING);
        }

        ExecutionResult execution;
        try {
            execution = actionExecutor.execute(request.requestId(), gateResult.action());
        } catch (RuntimeException error) {
            return gateChecked.withState(IncidentState.ROLLED_BACK);
        }
        IncidentRecord executing = gateChecked.withExecutionResult(execution, IncidentState.EXECUTING);

        VerificationResult verification = verifier.verify(executing);
        IncidentRecord verifying = executing.withVerification(verification, IncidentState.VERIFYING);
        if (verification.passed()) {
            governanceService.onVerified(context, verification);
            return verifying.withState(IncidentState.ARCHIVED);
        }
        return verifying.withState(IncidentState.ROLLED_BACK);
    }
}
