package com.ticket.ops.mcp.model;

import com.ticket.ops.common.util.Maps;

import java.util.Map;

/**
 * 工具执行结果。
 */
public record ExecutionResult(boolean success, String message, Map<String, Object> data) {

    public ExecutionResult {
        message = message == null ? "" : message;
        data = Maps.immutable(data);
    }

    public static ExecutionResult ok(String message, Map<String, Object> data) {
        return new ExecutionResult(true, message, data);
    }

    public static ExecutionResult failed(String message) {
        return new ExecutionResult(false, message, Map.of());
    }
}
