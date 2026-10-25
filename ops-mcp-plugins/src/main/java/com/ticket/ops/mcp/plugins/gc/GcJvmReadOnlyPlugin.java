package com.ticket.ops.mcp.plugins.gc;

import com.ticket.ops.mcp.manifest.McpPluginManifest;
import com.ticket.ops.mcp.manifest.PluginType;
import com.ticket.ops.mcp.manifest.ToolDefinition;
import com.ticket.ops.mcp.plugins.common.AbstractReadOnlyPlugin;

import java.util.List;
import java.util.Map;

/**
 * GC / JVM 只读插件。后续接入 GC 日志与 Micrometer/JFR。
 */
public class GcJvmReadOnlyPlugin extends AbstractReadOnlyPlugin {

    public GcJvmReadOnlyPlugin() {
        super(new McpPluginManifest(
                "gc-jvm-plugin",
                "gc",
                "1.0.0",
                PluginType.READ_ONLY,
                List.of(
                        new ToolDefinition("gc.queryGcLog",
                                "查询指定时间窗口内的 GC 日志摘要",
                                Map.of("type", "object", "properties", Map.of(
                                        "serviceName", Map.of("type", "string"),
                                        "start", Map.of("type", "string"),
                                        "end", Map.of("type", "string")))),
                        new ToolDefinition("gc.queryJvmMetrics",
                                "查询 JVM 堆、线程、CPU 指标",
                                Map.of("type", "object", "properties", Map.of(
                                        "serviceName", Map.of("type", "string"),
                                        "timeRange", Map.of("type", "object"))))
                )));
    }
}
