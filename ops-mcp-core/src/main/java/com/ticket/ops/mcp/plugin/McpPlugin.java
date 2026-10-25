package com.ticket.ops.mcp.plugin;

import com.ticket.ops.mcp.manifest.McpPluginManifest;
import com.ticket.ops.mcp.model.ExecutionResult;
import com.ticket.ops.mcp.model.ToolDetail;

import java.util.Map;

/**
 * MCP 插件契约。
 */
public interface McpPlugin {

    McpPluginManifest getManifest();

    ToolDetail getToolDetail(String toolName);

    ExecutionResult call(String toolName, Map<String, Object> params, String idempotencyKey);

    /**
     * 写工具执行前快照，用于回滚。只读插件可用默认空实现。
     */
    default Map<String, Object> snapshot(String toolName, Map<String, Object> params) {
        return Map.of();
    }
}
