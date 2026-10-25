package com.ticket.ops.mcp.manifest;

import com.ticket.ops.common.util.Text;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 插件清单。权限类型在清单层声明，Registry 在注册时强制校验。
 */
public record McpPluginManifest(
        String name,
        String namespace,
        String version,
        PluginType type,
        List<ToolDefinition> tools) {

    public McpPluginManifest {
        name = Text.requireNonBlank(name, "plugin.name");
        namespace = Text.requireNonBlank(namespace, "plugin.namespace");
        version = Text.defaultIfBlank(version, "1.0.0");
        type = Objects.requireNonNull(type, "plugin.type 不能为空");
        tools = tools == null ? List.of() : List.copyOf(tools);
        if (tools.isEmpty()) {
            throw new IllegalArgumentException("插件 " + name + " 至少需要一个工具");
        }
        Set<String> seen = new HashSet<>();
        for (ToolDefinition tool : tools) {
            if (!seen.add(tool.name())) {
                throw new IllegalArgumentException("插件 " + name + " 存在重复工具: " + tool.name());
            }
        }
    }

    public boolean isReadOnly() {
        return type == PluginType.READ_ONLY;
    }

    public boolean isWrite() {
        return type == PluginType.WRITE;
    }

    public boolean hasTool(String toolName) {
        return tools.stream().anyMatch(tool -> tool.name().equals(toolName));
    }
}
