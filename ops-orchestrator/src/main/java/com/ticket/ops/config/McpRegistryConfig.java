package com.ticket.ops.config;

import com.ticket.ops.mcp.manifest.PluginType;
import com.ticket.ops.mcp.plugin.McpPlugin;
import com.ticket.ops.mcp.registry.ToolRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 读写 Registry 必须是两个独立 Bean。
 * 分析层只能引用 readOnlyRegistry，写权限由门禁独占。
 */
@Configuration
public class McpRegistryConfig {

    @Bean("readOnlyRegistry")
    public ToolRegistry readOnlyRegistry(List<McpPlugin> plugins) {
        ToolRegistry registry = new ToolRegistry(PluginType.READ_ONLY);
        plugins.stream()
                .filter(plugin -> plugin.getManifest().isReadOnly())
                .forEach(registry::register);
        return registry;
    }

    @Bean("writeRegistry")
    public ToolRegistry writeRegistry(List<McpPlugin> plugins) {
        ToolRegistry registry = new ToolRegistry(PluginType.WRITE);
        plugins.stream()
                .filter(plugin -> plugin.getManifest().isWrite())
                .forEach(registry::register);
        return registry;
    }
}
