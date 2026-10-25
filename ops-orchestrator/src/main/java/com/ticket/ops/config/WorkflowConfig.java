package com.ticket.ops.config;

import com.ticket.ops.analyzer.fallback.AnalysisFallbackHandler;
import com.ticket.ops.analyzer.skill.AnalysisSkillRegistry;
import com.ticket.ops.gate.ActionExecutor;
import com.ticket.ops.gate.ExecutionGate;
import com.ticket.ops.knowledge.governance.KnowledgeGovernanceService;
import com.ticket.ops.workflow.DeterministicVerifier;
import com.ticket.ops.workflow.IncidentService;
import com.ticket.ops.workflow.IncidentWorkflow;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 编排与事件存储装配。
 */
@Configuration
public class WorkflowConfig {

    @Bean
    public DeterministicVerifier deterministicVerifier() {
        return new DeterministicVerifier();
    }

    @Bean
    public IncidentWorkflow incidentWorkflow(
            AnalysisSkillRegistry skillRegistry,
            AnalysisFallbackHandler fallbackHandler,
            ExecutionGate executionGate,
            ActionExecutor actionExecutor,
            DeterministicVerifier verifier,
            KnowledgeGovernanceService governanceService) {
        return new IncidentWorkflow(skillRegistry, fallbackHandler, executionGate,
                actionExecutor, verifier, governanceService);
    }

    @Bean
    public IncidentService incidentService(IncidentWorkflow workflow) {
        return new IncidentService(workflow);
    }
}
