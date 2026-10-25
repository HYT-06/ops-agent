package com.ticket.ops.mcp.manifest;

import com.ticket.ops.common.util.Maps;
import com.ticket.ops.common.util.Text;

import java.util.Map;

/**
 * 单个 MCP 工具定义。
 */
public record ToolDefinition(String name, String description, Map<String, Object> schema) {

    public ToolDefinition {
        name = Text.requireNonBlank(name, "tool.name");
        description = description == null ? "" : description;
        schema = Maps.immutable(schema);
    }
}
