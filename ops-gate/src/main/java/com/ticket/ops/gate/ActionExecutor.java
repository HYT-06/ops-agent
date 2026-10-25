package com.ticket.ops.gate;

import com.ticket.ops.common.model.SuggestedAction;
import com.ticket.ops.gate.rollback.RollbackRegistry;
import com.ticket.ops.knowledge.audit.AuditLedger;
import com.ticket.ops.mcp.model.ExecutionResult;
import com.ticket.ops.mcp.registry.ToolRegistry;

import java.util.Map;
import java.util.Objects;

/**
 * 写动作执行器：幂等前置检查 → 快照 → 注册回滚点 → 执行 → 审计。
 */
public class ActionExecutor {

    private final ToolRegistry writeRegistry;
    private final AuditLedger auditLedger;
    private final RollbackRegistry rollbackRegistry;

    public ActionExecutor(ToolRegistry writeRegistry,
                          AuditLedger auditLedger,
                          RollbackRegistry rollbackRegistry) {
        this.writeRegistry = Objects.requireNonNull(writeRegistry, "writeRegistry 不能为空");
        this.auditLedger = Objects.requireNonNull(auditLedger, "auditLedger 不能为空");
        this.rollbackRegistry = Objects.requireNonNull(rollbackRegistry, "rollbackRegistry 不能为空");
    }

    public ExecutionResult execute(String requestId, SuggestedAction action) {
        if (!action.isComplete()) {
            throw new IllegalArgumentException("三件套不完整，拒绝执行");
        }

        if (auditLedger.existsIdempotencyKey(action.idempotencyKey())) {
            return ExecutionResult.ok("幂等键已消费，跳过重复执行",
                    Map.of("skipped", true, "idempotencyKey", action.idempotencyKey()));
        }

        Map<String, Object> before = writeRegistry.snapshot(action.toolName(), action.params());
        rollbackRegistry.register(requestId, action.toolName(), action.params(),
                before, action.idempotencyKey());

        try {
            ExecutionResult result = writeRegistry.execute(
                    action.toolName(), action.params(), action.idempotencyKey());
            if (!result.success()) {
                auditLedger.recordExecutionFailure(requestId, action,
                        new IllegalStateException(result.message()));
                rollbackRegistry.triggerRollback(requestId);
                return result;
            }
            auditLedger.recordExecution(requestId, action, result.message());
            return result;
        } catch (RuntimeException error) {
            auditLedger.recordExecutionFailure(requestId, action, error);
            rollbackRegistry.triggerRollback(requestId);
            throw error;
        }
    }
}
