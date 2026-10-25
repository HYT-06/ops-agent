package com.ticket.ops.mcp.registry;

import com.ticket.ops.common.util.Maps;
import com.ticket.ops.common.util.Text;
import com.ticket.ops.mcp.exception.ToolNotFoundException;
import com.ticket.ops.mcp.manifest.McpPluginManifest;
import com.ticket.ops.mcp.manifest.PluginType;
import com.ticket.ops.mcp.manifest.ToolDefinition;
import com.ticket.ops.mcp.model.ExecutionResult;
import com.ticket.ops.mcp.model.ToolDetail;
import com.ticket.ops.mcp.model.ToolSummary;
import com.ticket.ops.mcp.plugin.McpPlugin;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * MCP 微内核。
 * <p>
 * 每个实例只接受一种权限类型：只读 Registry 无法注册写插件，从结构上隔离分析 Agent 的写权限。
 */
public final class ToolRegistry {

    private final PluginType acceptedType;
    private final Map<String, McpPlugin> pluginsByName = new LinkedHashMap<>();
    private final Map<String, McpPlugin> pluginsByTool = new LinkedHashMap<>();
    private final Map<String, ToolDefinition> definitionsByTool = new LinkedHashMap<>();

    public ToolRegistry(PluginType acceptedType) {
        this.acceptedType = Objects.requireNonNull(acceptedType, "acceptedType 不能为空");
    }

    public PluginType acceptedType() {
        return acceptedType;
    }

    public synchronized void register(McpPlugin plugin) {
        Objects.requireNonNull(plugin, "plugin 不能为空");
        McpPluginManifest manifest = Objects.requireNonNull(plugin.getManifest(), "plugin.manifest 不能为空");
        if (manifest.type() != acceptedType) {
            throw new IllegalArgumentException("Registry(" + acceptedType + ") 拒绝注册 "
                    + manifest.type() + " 插件: " + manifest.name());
        }
        if (pluginsByName.containsKey(manifest.name())) {
            throw new IllegalArgumentException("插件已注册: " + manifest.name());
        }
        for (ToolDefinition tool : manifest.tools()) {
            if (pluginsByTool.containsKey(tool.name())) {
                throw new IllegalArgumentException("工具名冲突: " + tool.name());
            }
        }
        pluginsByName.put(manifest.name(), plugin);
        for (ToolDefinition tool : manifest.tools()) {
            pluginsByTool.put(tool.name(), plugin);
            definitionsByTool.put(tool.name(), tool);
        }
    }

    public synchronized void unregister(String pluginName) {
        McpPlugin plugin = pluginsByName.remove(pluginName);
        if (plugin == null) {
            return;
        }
        for (ToolDefinition tool : plugin.getManifest().tools()) {
            pluginsByTool.remove(tool.name());
            definitionsByTool.remove(tool.name());
        }
    }

    /**
     * 两级发现第一级：只返回清单，不返回 schema。
     */
    public synchronized List<ToolSummary> list(String namespace) {
        List<ToolSummary> result = new ArrayList<>();
        for (McpPlugin plugin : pluginsByName.values()) {
            McpPluginManifest manifest = plugin.getManifest();
            if (namespace != null && !namespace.isBlank() && !manifest.namespace().equals(namespace)) {
                continue;
            }
            for (ToolDefinition tool : manifest.tools()) {
                result.add(new ToolSummary(tool.name(), manifest.namespace(), tool.description()));
            }
        }
        return List.copyOf(result);
    }

    /**
     * 两级发现第二级：按需查询 schema。
     */
    public synchronized ToolDetail detail(String toolName) {
        McpPlugin plugin = pluginsByTool.get(toolName);
        if (plugin == null) {
            throw new ToolNotFoundException(toolName);
        }
        return ToolDetail.from(plugin.getManifest().namespace(), definitionsByTool.get(toolName));
    }

    public ExecutionResult execute(String toolName, Map<String, Object> params, String idempotencyKey) {
        McpPlugin plugin;
        synchronized (this) {
            plugin = pluginsByTool.get(toolName);
        }
        if (plugin == null) {
            throw new ToolNotFoundException(toolName);
        }
        if (acceptedType == PluginType.WRITE && Text.defaultIfBlank(idempotencyKey, "").isBlank()) {
            throw new IllegalArgumentException("写工具调用必须携带 idempotencyKey: " + toolName);
        }
        return plugin.call(toolName, Maps.immutable(params), idempotencyKey);
    }

    public Map<String, Object> snapshot(String toolName, Map<String, Object> params) {
        McpPlugin plugin;
        synchronized (this) {
            plugin = pluginsByTool.get(toolName);
        }
        if (plugin == null) {
            throw new ToolNotFoundException(toolName);
        }
        return plugin.snapshot(toolName, Maps.immutable(params));
    }

    public synchronized boolean contains(String toolName) {
        return pluginsByTool.containsKey(toolName);
    }

    public synchronized Set<String> toolNames() {
        return Set.copyOf(new LinkedHashSet<>(pluginsByTool.keySet()));
    }
}
