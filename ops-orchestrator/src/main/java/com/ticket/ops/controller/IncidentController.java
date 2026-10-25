package com.ticket.ops.controller;

import com.ticket.ops.dto.CreateIncidentRequest;
import com.ticket.ops.workflow.IncidentRecord;
import com.ticket.ops.workflow.IncidentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 运维事件入口。
 */
@RestController
@RequestMapping("/ops/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @PostMapping
    public ResponseEntity<IncidentRecord> create(@Valid @RequestBody CreateIncidentRequest request) {
        IncidentRecord record = incidentService.create(request.toAnalysisRequest());
        return ResponseEntity.status(HttpStatus.CREATED).body(record);
    }

    @GetMapping
    public List<IncidentRecord> list() {
        return incidentService.list();
    }

    @GetMapping("/{requestId}")
    public IncidentRecord get(@PathVariable String requestId) {
        return incidentService.getRequired(requestId);
    }

    /**
     * 推进事件：分析 → 门禁 → 执行 → 验证（P0 分析技能为占位实现）。
     */
    @PostMapping("/{requestId}/analyze")
    public IncidentRecord analyze(@PathVariable String requestId) {
        return incidentService.advance(requestId);
    }
}
