package com.ticket.ops.mcp.plugins.exec;

import com.ticket.ops.mcp.exception.ToolNotFoundException;
import com.ticket.ops.mcp.manifest.McpPluginManifest;
import com.ticket.ops.mcp.manifest.PluginType;
import com.ticket.ops.mcp.manifest.ToolDefinition;
import com.ticket.ops.mcp.model.ExecutionResult;
import com.ticket.ops.mcp.model.ToolDetail;
import com.ticket.ops.mcp.plugin.McpPlugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池 / 消费者扩容写插件。
 * <p>
 * P0 为内存占位实现，用于验证「快照 → 门禁 → 执行 → 回滚」链路，不会改动真实应用。
 */
public class ThreadPoolExecWritePlugin implements McpPlugin {

    private final McpPluginManifest manifest = new McpPluginManifest(
            "threadpool-exec-plugin",
            "exec",
            "1.0.0",
            PluginType.WRITE,
            List.of(
                    new ToolDefinition("exec.updateThreadPool",
                            "调整线程池 coreSize / maxSize",
                            Map.of("type", "object", "properties", Map.of(
                                    "coreSize", Map.of("type", "integer"),
                                    "maxSize", Map.of("type", "integer")))),
                    new ToolDefinition("exec.scaleConsumer",
                            "调整消费者实例数",
                            Map.of("type", "object", "properties", Map.of(
                                    "consumerCount", Map.of("type", "integer"))))
            ));

    private final AtomicInteger coreSize = new AtomicInteger(8);
    private final AtomicInteger maxSize = new AtomicInteger(32);
    private final AtomicInteger consumerCount = new AtomicInteger(2);

    @Override
    public McpPluginManifest getManifest() {
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
    public Map<String, Object> snapshot(String toolName, Map<String, Object> params) {
        return Map.of(
                "coreSize", coreSize.get(),
                "maxSize", maxSize.get(),
                "consumerCount", consumerCount.get());
    }

    @Override
    public ExecutionResult call(String toolName, Map<String, Object> params, String idempotencyKey) {
        return switch (toolName) {
            case "exec.updateThreadPool" -> updateThreadPool(params);
            case "exec.scaleConsumer" -> scaleConsumer(params);
            default -> throw new ToolNotFoundException(toolName);
        };
    }

    private ExecutionResult updateThreadPool(Map<String, Object> params) {
        int newCore = intValue(params, "coreSize", coreSize.get());
        int newMax = intValue(params, "maxSize", maxSize.get());
        int beforeCore = coreSize.getAndSet(newCore);
        int beforeMax = maxSize.getAndSet(newMax);
        return ExecutionResult.ok("线程池参数已更新（P0 内存占位）", Map.of(
                "beforeCoreSize", beforeCore,
                "afterCoreSize", newCore,
                "beforeMaxSize", beforeMax,
                "afterMaxSize", newMax));
    }

    private ExecutionResult scaleConsumer(Map<String, Object> params) {
        int newCount = intValue(params, "consumerCount", consumerCount.get());
        int before = consumerCount.getAndSet(newCount);
        return ExecutionResult.ok("消费者数量已更新（P0 内存占位）", Map.of(
                "beforeConsumerCount", before,
                "afterConsumerCount", newCount));
    }

    private int intValue(Map<String, Object> params, String key, int fallback) {
        Object value = params.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            return Integer.parseInt(text);
        }
        return fallback;
    }
}
