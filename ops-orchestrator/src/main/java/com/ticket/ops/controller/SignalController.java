package com.ticket.ops.controller;

import com.ticket.ops.common.signal.OpsSignal;
import com.ticket.ops.rule.RuleEngine;
import com.ticket.ops.workflow.IncidentRecord;
import com.ticket.ops.workflow.IncidentService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * L1 规则引擎入口：上报指标快照，按阈值生成标准化运维事件。
 */
@RestController
@RequestMapping("/ops/signals")
public class SignalController {

    private final RuleEngine ruleEngine;
    private final IncidentService incidentService;

    public SignalController(RuleEngine ruleEngine, IncidentService incidentService) {
        this.ruleEngine = ruleEngine;
        this.incidentService = incidentService;
    }

    @PostMapping("/evaluate")
    public List<IncidentRecord> evaluate(@RequestBody OpsSignal signal) {
        return ruleEngine.evaluate(signal).stream()
                .map(incidentService::create)
                .toList();
    }
}
