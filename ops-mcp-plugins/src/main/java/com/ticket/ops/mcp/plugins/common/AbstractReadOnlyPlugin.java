package com.ticket.ops.mcp.plugins.common;

import com.ticket.ops.mcp.exception.ToolNotFoundException;
import com.ticket.ops.mcp.manifest.McpPluginManifest;
import com.ticket.ops.mcp.manifest.ToolDefinition;
import com.ticket.ops.mcp.model.ExecutionResult;
import com.ticket.ops.mcp.model.ToolDetail;
import com.ticket.ops.mcp.plugin.McpPlugin;

import java.util.Map;

/**
 * 只读插件基类：P0 只做工具契约与占位返回，后续接入真实数据源。
 */
public abstract class AbstractReadOnlyPlugin implements McpPlugin {

    private final McpPluginManifest manifest;

    protected AbstractReadOnlyPlugin(McpPluginManifest manifest) {
        if (!manifest.isReadOnly()) {
            throw new IllegalArgumentException("只读插件基类不能承载写插件: " + manifest.name());
        }
        this.manifest = manifest;
    }

    @Override
    public final McpPluginManifest getManifest() {
        return manifest;
    }

    @Override
    public ToolDetail getToolDetail(String toolName) {
        ToolDefinition definition = manifest.tools().stream()
                .filter(tool -> tool.name().equals(toolName))
                .findFirst()
                .orElseThrow(() -> new ToolNotFoundException(toolName));
        return ToolDetail.from(manifest.namespace(), definition);
    }

    @Override
    public ExecutionResult call(String toolName, Map<String, Object> params, String idempotencyKey) {
        if (!manifest.hasTool(toolName)) {
            throw new ToolNotFoundException(toolName);
        }
        return ExecutionResult.ok("P0 占位：数据源尚未接入", Map.of(
                "plugin", manifest.name(),
                "tool", toolName,
                "params", params));
    }
}
