package com.ticket.ops.config;

import com.ticket.ops.mcp.plugins.dlq.DlqReadOnlyPlugin;
import com.ticket.ops.mcp.plugins.dlq.DlqReplayWritePlugin;
import com.ticket.ops.mcp.plugins.exec.ThreadPoolExecWritePlugin;
import com.ticket.ops.mcp.plugins.gc.GcJvmReadOnlyPlugin;
import com.ticket.ops.mcp.plugins.sql.SlowSqlReadOnlyPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 内置 MCP 插件装配。P0 全部为占位实现。
 */
@Configuration
public class McpPluginConfig {

    @Bean
    public GcJvmReadOnlyPlugin gcJvmReadOnlyPlugin() {
        return new GcJvmReadOnlyPlugin();
    }

    @Bean
    public SlowSqlReadOnlyPlugin slowSqlReadOnlyPlugin() {
        return new SlowSqlReadOnlyPlugin();
    }

    @Bean
    public DlqReadOnlyPlugin dlqReadOnlyPlugin() {
        return new DlqReadOnlyPlugin();
    }

    @Bean
    public ThreadPoolExecWritePlugin threadPoolExecWritePlugin() {
        return new ThreadPoolExecWritePlugin();
    }

    @Bean
    public DlqReplayWritePlugin dlqReplayWritePlugin() {
        return new DlqReplayWritePlugin();
    }
}
