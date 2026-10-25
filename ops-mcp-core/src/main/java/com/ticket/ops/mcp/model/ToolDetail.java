package com.ticket.ops.mcp.model;

import com.ticket.ops.common.util.Maps;
import com.ticket.ops.mcp.manifest.ToolDefinition;

import java.util.Map;

/**
 * 工具详情：两级发现的第二级。
 */
public record ToolDetail(String name, String namespace, String description, Map<String, Object> schema) {

    public ToolDetail {
        schema = Maps.immutable(schema);
    }

    public static ToolDetail from(String namespace, ToolDefinition definition) {
        return new ToolDetail(definition.name(), namespace, definition.description(), definition.schema());
    }
}
