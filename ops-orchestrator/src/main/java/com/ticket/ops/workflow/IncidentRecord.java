package com.ticket.ops.workflow;

import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.AnalysisRequest;
import com.ticket.ops.common.model.GateResult;
import com.ticket.ops.common.model.IncidentState;
import com.ticket.ops.common.model.VerificationResult;
import com.ticket.ops.mcp.model.ExecutionResult;

import java.time.Instant;

/**
 * 事故快照。每次状态变更都生成新记录，便于审计与并发读取。
 */
public record IncidentRecord(
        AnalysisRequest request,
        IncidentState state,
        AnalysisReport report,
        GateResult gateResult,
        ExecutionResult executionResult,
        VerificationResult verification,
        Instant createdAt,
        Instant updatedAt) {

    public IncidentRecord {
        state = state == null ? IncidentState.RECEIVED : state;
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public static IncidentRecord received(AnalysisRequest request) {
        Instant now = Instant.now();
        return new IncidentRecord(request, IncidentState.RECEIVED, null,
                null, null, null, now, now);
    }

    public IncidentRecord withState(IncidentState newState) {
        return new IncidentRecord(request, newState, report, gateResult,
                executionResult, verification, createdAt, Instant.now());
    }

    public IncidentRecord withReport(AnalysisReport newReport, IncidentState newState) {
        return new IncidentRecord(request, newState, newReport, gateResult,
                executionResult, verification, createdAt, Instant.now());
    }

    public IncidentRecord withGateResult(GateResult newGateResult, IncidentState newState) {
        return new IncidentRecord(request, newState, report, newGateResult,
                executionResult, verification, createdAt, Instant.now());
    }

    public IncidentRecord withExecutionResult(ExecutionResult newExecutionResult, IncidentState newState) {
        return new IncidentRecord(request, newState, report, gateResult,
                newExecutionResult, verification, createdAt, Instant.now());
    }

    public IncidentRecord withVerification(VerificationResult newVerification, IncidentState newState) {
        return new IncidentRecord(request, newState, report, gateResult,
                executionResult, newVerification, createdAt, Instant.now());
    }
}
