package com.ticket.ops.controller;

import com.ticket.ops.workflow.IncidentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 基础健康检查。Actuator 的 /actuator/health 同时可用。
 */
@RestController
@RequestMapping("/ops")
public class HealthController {

    private final IncidentService incidentService;

    public HealthController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "application", "ops-agent",
                "version", "0.1.0-SNAPSHOT",
                "incidentCount", incidentService.count(),
                "modules", List.of(
                        "ops-probe-sdk",
                        "ops-mcp-core",
                        "ops-mcp-plugins",
                        "ops-analyzer",
                        "ops-gate",
                        "ops-knowledge",
                        "ops-orchestrator"));
    }
}
