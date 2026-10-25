package com.ticket.ops.eval.dataset;

import com.ticket.ops.common.model.SceneType;

import java.util.Map;
import java.util.Set;

/**
 * 离线评测用例。首期目标 20~30 条历史案例，P0 先提供结构。
 */
public record EvalCase(
        String caseId,
        SceneType scene,
        String triggerDescription,
        Map<String, Object> mockEvidence,
        String expectedRootCause,
        String expectedActionToolName,
        Set<String> acceptableActions) {

    public EvalCase {
        expectedRootCause = expectedRootCause == null ? "" : expectedRootCause;
        expectedActionToolName = expectedActionToolName == null ? "" : expectedActionToolName;
        mockEvidence = mockEvidence == null ? Map.of() : Map.copyOf(mockEvidence);
        acceptableActions = acceptableActions == null ? Set.of() : Set.copyOf(acceptableActions);
    }
}
