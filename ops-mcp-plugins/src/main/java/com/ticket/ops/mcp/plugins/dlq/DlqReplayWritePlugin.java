package com.ticket.ops.mcp.plugins.dlq;

import com.ticket.ops.mcp.exception.ToolNotFoundException;
import com.ticket.ops.mcp.manifest.McpPluginManifest;
import com.ticket.ops.mcp.manifest.PluginType;
import com.ticket.ops.mcp.manifest.ToolDefinition;
import com.ticket.ops.mcp.model.ExecutionResult;
import com.ticket.ops.mcp.model.ToolDetail;
import com.ticket.ops.mcp.plugin.McpPlugin;

import java.util.List;
import java.util.Map;

/**
 * DLQ 重放写插件占位。P3 接入真实消息重放，必须始终走人工审批。
 */
public class DlqReplayWritePlugin implements McpPlugin {

    private final McpPluginManifest manifest = new McpPluginManifest(
            "dlq-replay-plugin",
            "dlq",
            "1.0.0",
            PluginType.WRITE,
            List.of(new ToolDefinition("dlq.replay",
                    "重放死信消息（高风险，必须人工审批）",
                    Map.of("type", "object", "properties", Map.of(
                            "messageId", Map.of("type", "string"),
                            "batchSize", Map.of("type", "integer"))))));

    @Override
    public McpPluginManifest getManifest() {
        return manifest;
    }

    @Override
    public ToolDetail getToolDetail(String toolName) {
        return ToolDetail.from(manifest.namespace(), manifest.tools().get(0));
    }

    @Override
    public Map<String, Object> snapshot(String toolName, Map<String, Object> params) {
        return Map.of("pendingReplay", 0);
    }

    @Override
    public ExecutionResult call(String toolName, Map<String, Object> params, String idempotencyKey) {
        if (!"dlq.replay".equals(toolName)) {
            throw new ToolNotFoundException(toolName);
        }
        return ExecutionResult.ok("P0 占位：未执行真实重放", Map.of(
                "messageId", params.getOrDefault("messageId", ""),
                "idempotencyKey", idempotencyKey));
    }
}
