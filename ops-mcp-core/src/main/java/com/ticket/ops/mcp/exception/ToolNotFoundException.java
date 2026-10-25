package com.ticket.ops.mcp.exception;

/**
 * 工具未在目标 Registry 注册（如分析 Agent 尝试调用写工具）。
 */
public class ToolNotFoundException extends RuntimeException {

    public ToolNotFoundException(String toolName) {
        super("工具未授权或不存在: " + toolName);
    }
}
