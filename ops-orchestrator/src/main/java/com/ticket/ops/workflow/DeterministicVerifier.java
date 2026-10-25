package com.ticket.ops.workflow;

import com.ticket.ops.common.model.VerificationResult;
import com.ticket.ops.mcp.model.ExecutionResult;

/**
 * L5 确定性验证器：不使用 LLM，只根据执行结果与阈值判定。
 */
public class DeterministicVerifier {

    public VerificationResult verify(IncidentRecord incident) {
        ExecutionResult execution = incident.executionResult();
        if (execution == null) {
            return VerificationResult.fail("没有可验证的执行结果");
        }
        if (!execution.success()) {
            return VerificationResult.fail("写工具执行失败: " + execution.message());
        }
        return VerificationResult.pass("写工具返回成功（P0 仅校验执行结果，P2 接入真实指标阈值）");
    }
}
