package com.ticket.ops.knowledge.audit;

import com.ticket.ops.common.model.AnalysisRequest;
import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.SceneType;
import com.ticket.ops.common.model.SuggestedAction;
import com.ticket.ops.common.model.TriggerSignal;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryAuditLedgerTest {

    private final InMemoryAuditLedger ledger = new InMemoryAuditLedger();

    @Test
    void executionConsumesIdempotencyKey() {
        SuggestedAction action = new SuggestedAction("exec.scaleConsumer",
                Map.of("consumerCount", 3), "consumerCount=2", "idem-1");

        ledger.recordExecution("req-1", action, "ok");

        assertTrue(ledger.existsIdempotencyKey("idem-1"));
        assertEquals(1, ledger.findByRequestId("req-1").size());
        assertEquals("EXECUTION", ledger.findByRequestId("req-1").get(0).eventType());
    }

    @Test
    void failedExecutionDoesNotConsumeIdempotencyKey() {
        SuggestedAction action = new SuggestedAction("exec.scaleConsumer",
                Map.of("consumerCount", 3), "consumerCount=2", "idem-2");

        ledger.recordExecutionFailure("req-2", action, new IllegalStateException("boom"));

        assertFalse(ledger.existsIdempotencyKey("idem-2"));
        assertEquals(1, ledger.findByRequestId("req-2").size());
    }

    @Test
    void gateCheckIsAppended() {
        AnalysisRequest request = AnalysisRequest.create(SceneType.SLOW_SQL, "demo",
                TriggerSignal.manual("test"), Map.of());
        AnalysisReport report = AnalysisReport.placeholder(request, "stub");

        ledger.recordGateChecked(request.requestId(), report, com.ticket.ops.common.model.RiskLevel.LOW);

        assertEquals(1, ledger.findByRequestId(request.requestId()).size());
        assertEquals("GATE", ledger.findByRequestId(request.requestId()).get(0).eventType());
    }
}
