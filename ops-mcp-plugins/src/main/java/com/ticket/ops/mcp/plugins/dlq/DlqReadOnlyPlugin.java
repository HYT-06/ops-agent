package com.ticket.ops.mcp.plugins.dlq;

import com.ticket.ops.mcp.manifest.McpPluginManifest;
import com.ticket.ops.mcp.manifest.PluginType;
import com.ticket.ops.mcp.manifest.ToolDefinition;
import com.ticket.ops.mcp.plugins.common.AbstractReadOnlyPlugin;

import java.util.List;
import java.util.Map;

/**
 * DLQ 只读插件。后续接入 RabbitMQ 死信队列与业务幂等记录。
 */
public class DlqReadOnlyPlugin extends AbstractReadOnlyPlugin {

    public DlqReadOnlyPlugin() {
        super(new McpPluginManifest(
                "dlq-read-plugin",
                "mq",
                "1.0.0",
                PluginType.READ_ONLY,
                List.of(
                        new ToolDefinition("mq.queryDlqMessages",
                                "查询死信消息列表与异常摘要",
                                Map.of("type", "object", "properties", Map.of(
                                        "queue", Map.of("type", "string"),
                                        "limit", Map.of("type", "integer")))),
                        new ToolDefinition("mq.queryDlqSummary",
                                "查询死信队列按异常类型聚合结果",
                                Map.of("type", "object", "properties", Map.of(
                                        "queue", Map.of("type", "string"))))
                )));
    }
}
