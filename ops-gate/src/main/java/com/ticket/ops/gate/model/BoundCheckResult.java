package com.ticket.ops.gate.model;

/**
 * 参数边界校验结果。
 */
public record BoundCheckResult(boolean passed, String reason) {

    public static BoundCheckResult ok() {
        return new BoundCheckResult(true, "");
    }

    public static BoundCheckResult fail(String reason) {
        return new BoundCheckResult(false, reason);
    }
}
