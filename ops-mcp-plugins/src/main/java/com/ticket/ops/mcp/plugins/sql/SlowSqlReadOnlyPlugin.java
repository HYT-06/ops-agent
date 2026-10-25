package com.ticket.ops.mcp.plugins.sql;

import com.ticket.ops.mcp.manifest.McpPluginManifest;
import com.ticket.ops.mcp.manifest.PluginType;
import com.ticket.ops.mcp.manifest.ToolDefinition;
import com.ticket.ops.mcp.plugins.common.AbstractReadOnlyPlugin;

import java.util.List;
import java.util.Map;

/**
 * 慢 SQL 只读插件。后续接入慢查询日志、EXPLAIN、统计信息。
 */
public class SlowSqlReadOnlyPlugin extends AbstractReadOnlyPlugin {

    public SlowSqlReadOnlyPlugin() {
        super(new McpPluginManifest(
                "slow-sql-plugin",
                "sql",
                "1.0.0",
                PluginType.READ_ONLY,
                List.of(
                        new ToolDefinition("sql.querySlowLog",
                                "查询时间窗口内的慢 SQL 日志",
                                Map.of("type", "object", "properties", Map.of(
                                        "serviceName", Map.of("type", "string"),
                                        "minDurationMs", Map.of("type", "number")))),
                        new ToolDefinition("sql.explain",
                                "获取 SQL 当前执行计划",
                                Map.of("type", "object", "properties", Map.of(
                                        "sql", Map.of("type", "string")))),
                        new ToolDefinition("sql.queryTableStats",
                                "查询表行数与索引基数",
                                Map.of("type", "object", "properties", Map.of(
                                        "table", Map.of("type", "string"))))
                )));
    }
}
