package com.ticket.ops.config;

import com.ticket.ops.gate.ActionExecutor;
import com.ticket.ops.gate.ExecutionGate;
import com.ticket.ops.gate.ParameterBoundValidator;
import com.ticket.ops.gate.RiskClassifier;
import com.ticket.ops.gate.approval.ApprovalCallbackStore;
import com.ticket.ops.gate.approval.ApprovalNotifier;
import com.ticket.ops.gate.approval.ApprovalTimeoutScheduler;
import com.ticket.ops.gate.approval.LoggingApprovalNotifier;
import com.ticket.ops.gate.rollback.RollbackRegistry;
import com.ticket.ops.knowledge.audit.AuditLedger;
import com.ticket.ops.mcp.registry.ToolRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 门禁层装配。写 Registry 是唯一注入点。
 */
@Configuration
public class GateConfig {

    @Bean
    public ParameterBoundValidator parameterBoundValidator(OpsAgentProperties properties) {
        Map<String, List<ParameterBoundValidator.Bound>> bounds = new HashMap<>();
        properties.gate().bounds().forEach((toolName, parameterBounds) -> {
            List<ParameterBoundValidator.Bound> boundList = new ArrayList<>();
            parameterBounds.forEach((parameter, bound) ->
                    boundList.add(new ParameterBoundValidator.Bound(parameter, bound.min(), bound.max())));
            bounds.put(toolName, boundList);
        });
        if (bounds.isEmpty()) {
            bounds.putAll(ParameterBoundValidator.defaultBounds());
        }
        return new ParameterBoundValidator(bounds);
    }

    @Bean
    public RiskClassifier riskClassifier() {
        return new RiskClassifier();
    }

    @Bean
    public ApprovalCallbackStore approvalCallbackStore() {
        return new ApprovalCallbackStore();
    }

    @Bean
    public ApprovalNotifier approvalNotifier() {
        return new LoggingApprovalNotifier();
    }

    @Bean
    public RollbackRegistry rollbackRegistry() {
        return new RollbackRegistry();
    }

    @Bean
    public ExecutionGate executionGate(
            @Qualifier("writeRegistry") ToolRegistry writeRegistry,
            ParameterBoundValidator boundValidator,
            RiskClassifier riskClassifier,
            ApprovalNotifier approvalNotifier,
            ApprovalCallbackStore callbackStore,
            AuditLedger auditLedger,
            OpsAgentProperties properties) {
        return new ExecutionGate(writeRegistry, boundValidator, riskClassifier,
                approvalNotifier, callbackStore, auditLedger,
                properties.gate().approvalTimeoutMinutes());
    }

    @Bean
    public ActionExecutor actionExecutor(
            @Qualifier("writeRegistry") ToolRegistry writeRegistry,
            AuditLedger auditLedger,
            RollbackRegistry rollbackRegistry) {
        return new ActionExecutor(writeRegistry, auditLedger, rollbackRegistry);
    }

    @Bean
    public ApprovalTimeoutScheduler approvalTimeoutScheduler(ApprovalCallbackStore callbackStore) {
        return new ApprovalTimeoutScheduler(callbackStore);
    }
}
