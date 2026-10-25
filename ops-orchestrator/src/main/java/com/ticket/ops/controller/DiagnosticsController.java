package com.ticket.ops.controller;

import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.AnalysisRequest;
import com.ticket.ops.common.model.GateResult;
import com.ticket.ops.common.model.IncidentContext;
import com.ticket.ops.common.model.RootCauseCandidate;
import com.ticket.ops.common.model.TriggerSignal;
import com.ticket.ops.dto.GateCheckRequest;
import com.ticket.ops.gate.ExecutionGate;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * P0 诊断入口：不经过 LLM，直接验证门禁三件套 / 参数边界 / 风险分级。
 * 生产环境应关闭或加管理员鉴权。
 */
@RestController
@RequestMapping("/ops/diagnostics")
public class DiagnosticsController {

    private final ExecutionGate executionGate;

    public DiagnosticsController(ExecutionGate executionGate) {
        this.executionGate = executionGate;
    }

    @PostMapping("/gate-check")
    public GateResult gateCheck(@Valid @RequestBody GateCheckRequest request) {
        AnalysisRequest analysisRequest = AnalysisRequest.create(
                request.scene(), request.resourceId(),
                TriggerSignal.manual("diagnostic gate check"), Map.of());
        AnalysisReport report = new AnalysisReport(
                analysisRequest.requestId(),
                request.scene(),
                List.of(new RootCauseCandidate("诊断请求", 1.0, List.of())),
                request.toSuggestedAction(),
                1.0,
                List.of(),
                List.of(),
                Instant.now());
        return executionGate.check(report, IncidentContext.of(analysisRequest));
    }
}
