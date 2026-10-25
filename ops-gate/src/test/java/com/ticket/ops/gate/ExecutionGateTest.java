package com.ticket.ops.gate;

import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.GateResult;
import com.ticket.ops.common.model.IncidentContext;
import com.ticket.ops.common.model.RiskLevel;
import com.ticket.ops.common.model.RootCauseCandidate;
import com.ticket.ops.common.model.SceneType;
import com.ticket.ops.common.model.SuggestedAction;
import com.ticket.ops.gate.approval.ApprovalCallbackStore;
import com.ticket.ops.gate.approval.ApprovalNotifier;
import com.ticket.ops.gate.model.BoundCheckResult;
import com.ticket.ops.mcp.exception.ToolNotFoundException;
import com.ticket.ops.mcp.manifest.McpPluginManifest;
import com.ticket.ops.mcp.manifest.PluginType;
import com.ticket.ops.mcp.manifest.ToolDefinition;
import com.ticket.ops.mcp.model.ExecutionResult;
import com.ticket.ops.mcp.model.ToolDetail;
import com.ticket.ops.mcp.plugin.McpPlugin;
import com.ticket.ops.mcp.plugins.dlq.DlqReplayWritePlugin;
import com.ticket.ops.mcp.plugins.exec.ThreadPoolExecWritePlugin;
import com.ticket.ops.mcp.plugins.gc.GcJvmReadOnlyPlugin;
import com.ticket.ops.mcp.registry.ToolRegistry;
import com.ticket.ops.knowledge.audit.InMemoryAuditLedger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionGateTest {

    private ToolRegistry writeRegistry;
    private ToolRegistry readOnlyRegistry;
    private InMemoryAuditLedger auditLedger;
    private ApprovalCallbackStore callbackStore;
    private RecordingNotifier notifier;
    private ExecutionGate gate;

    @BeforeEach
    void setUp() {
        writeRegistry = new ToolRegistry(PluginType.WRITE);
        writeRegistry.register(new ThreadPoolExecWritePlugin());
        writeRegistry.register(new DlqReplayWritePlugin());

        readOnlyRegistry = new ToolRegistry(PluginType.READ_ONLY);
        readOnlyRegistry.register(new GcJvmReadOnlyPlugin());

        auditLedger = new InMemoryAuditLedger();
        callbackStore = new ApprovalCallbackStore();
        notifier = new RecordingNotifier();

        Map<String, List<ParameterBoundValidator.Bound>> bounds = Map.of(
                "exec.updateThreadPool", List.of(new ParameterBoundValidator.Bound("coreSize", 8, 64)),
                "dlq.replay", List.of(new ParameterBoundValidator.Bound("batchSize", 1, 100)));
        gate = new ExecutionGate(writeRegistry, new ParameterBoundValidator(bounds),
                new RiskClassifier(), notifier, callbackStore, auditLedger, 15);
    }

    @Test
    void rejectsOutOfBoundParameter() {
        GateResult result = gate.check(report(new SuggestedAction("exec.updateThreadPool",
                Map.of("coreSize", 999), "coreSize=8", "idem-1")), context());

        assertTrue(result.rejected());
        assertTrue(result.reason().contains("参数越界"));
    }

    @Test
    void rejectsUnauthorizedTool() {
        GateResult result = gate.check(report(new SuggestedAction("sql.addIndex",
                Map.of("table", "orders"), "DROP INDEX idx_1", "idem-2")), context());

        assertTrue(result.rejected());
        assertTrue(result.reason().contains("工具未授权"));
    }

    @Test
    void rejectsIncompleteThreePiece() {
        GateResult result = gate.check(report(new SuggestedAction("exec.updateThreadPool",
                Map.of("coreSize", 12), "coreSize=8", "")), context());

        assertTrue(result.rejected());
        assertTrue(result.reason().contains("三件套不完整"));
    }

    @Test
    void autoApprovesSmallThreadPoolExpansion() {
        GateResult result = gate.check(report(new SuggestedAction("exec.updateThreadPool",
                Map.of("currentCoreSize", 10, "coreSize", 12), "coreSize=10", "idem-3")), context());

        assertTrue(result.autoApproved());
        assertEquals(RiskLevel.LOW, result.riskLevel());
        assertEquals(1, auditLedger.findByRequestId("req-1").size());
    }

    @Test
    void dlqReplayAlwaysRequiresApproval() {
        GateResult result = gate.check(report(new SuggestedAction("dlq.replay",
                Map.of("messageId", "m1", "batchSize", 10), "none", "idem-4")), context());

        assertTrue(result.requiresApproval());
        assertEquals(RiskLevel.HIGH, result.riskLevel());
        assertEquals(1, callbackStore.size());
        assertEquals(1, notifier.actions.size());
    }

    @Test
    void analyzerCannotAccessWriteTool() {
        assertThrows(ToolNotFoundException.class, () -> readOnlyRegistry.execute(
                "exec.updateThreadPool", Map.of("coreSize", 12), "idem-5"));
    }

    @Test
    void writeToolRequiresIdempotencyKey() {
        assertThrows(IllegalArgumentException.class, () -> writeRegistry.execute(
                "exec.updateThreadPool", Map.of("coreSize", 12), ""));
    }

    @Test
    void validActionPassesBoundaryValidation() {
        BoundCheckResult result = new ParameterBoundValidator(Map.of(
                "exec.updateThreadPool", List.of(new ParameterBoundValidator.Bound("coreSize", 8, 64))))
                .validate("exec.updateThreadPool", Map.of("coreSize", 64));
        assertTrue(result.passed());
    }

    @Test
    void actionExecutorSnapshotsThenExecutes() {
        RecordingRollbackRegistry rollbackRegistry = new RecordingRollbackRegistry();
        ActionExecutor executor = new ActionExecutor(writeRegistry, auditLedger, rollbackRegistry);
        SuggestedAction action = new SuggestedAction("exec.updateThreadPool",
                Map.of("coreSize", 12), "coreSize=8", "idem-6");

        ExecutionResult result = executor.execute("req-2", action);

        assertTrue(result.success());
        assertTrue(rollbackRegistry.registered);
        assertFalse(rollbackRegistry.rolledBack);
        assertTrue(auditLedger.existsIdempotencyKey("idem-6"));
        assertTrue(rollbackRegistry.find("req-2").isPresent());
    }

    @Test
    void failedExecutionTriggersRollback() {
        ToolRegistry failingRegistry = new ToolRegistry(PluginType.WRITE);
        failingRegistry.register(new FailingWritePlugin());
        RecordingRollbackRegistry rollbackRegistry = new RecordingRollbackRegistry();
        ActionExecutor executor = new ActionExecutor(failingRegistry, auditLedger, rollbackRegistry);
        SuggestedAction action = new SuggestedAction("exec.fail",
                Map.of(), "none", "idem-7");

        assertThrows(IllegalStateException.class, () -> executor.execute("req-3", action));

        assertTrue(rollbackRegistry.registered);
        assertTrue(rollbackRegistry.rolledBack);
        assertTrue(rollbackRegistry.find("req-3").isEmpty());
        assertTrue(auditLedger.findByRequestId("req-3").stream()
                .anyMatch(event -> "EXECUTION_FAILED".equals(event.eventType())));
    }

    private AnalysisReport report(SuggestedAction action) {
        return new AnalysisReport("req-1", SceneType.FULLGC,
                List.of(new RootCauseCandidate("测试根因", 0.9, List.of())),
                action, 0.9, List.of(), List.of(), Instant.now());
    }

    private IncidentContext context() {
        return new IncidentContext("req-1", SceneType.FULLGC, "demo", java.util.Set.of());
    }

    static class RecordingNotifier implements ApprovalNotifier {

        final List<SuggestedAction> actions = new ArrayList<>();

        @Override
        public void notify(SuggestedAction action, AnalysisReport report,
                           IncidentContext context, String callbackId) {
            actions.add(action);
        }
    }

    static class RecordingRollbackRegistry extends com.ticket.ops.gate.rollback.RollbackRegistry {

        boolean registered;
        boolean rolledBack;

        @Override
        public void register(String requestId, String toolName, Map<String, Object> params,
                             Map<String, Object> before, String idempotencyKey) {
            registered = true;
            super.register(requestId, toolName, params, before, idempotencyKey);
        }

        @Override
        public java.util.Optional<com.ticket.ops.gate.rollback.RollbackEntry> triggerRollback(String requestId) {
            rolledBack = true;
            return super.triggerRollback(requestId);
        }
    }

    static class FailingWritePlugin implements McpPlugin {

        private final McpPluginManifest manifest = new McpPluginManifest(
                "failing-write-plugin", "exec", "1.0.0", PluginType.WRITE,
                List.of(new ToolDefinition("exec.fail", "always fails", Map.of())));

        @Override
        public McpPluginManifest getManifest() {
            return manifest;
        }

        @Override
        public ToolDetail getToolDetail(String toolName) {
            return ToolDetail.from("exec", manifest.tools().get(0));
        }

        @Override
        public ExecutionResult call(String toolName, Map<String, Object> params, String idempotencyKey) {
            throw new IllegalStateException("simulated write failure");
        }
    }
}
