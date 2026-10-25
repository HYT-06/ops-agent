package com.ticket.ops.rule;

import com.ticket.ops.common.model.AnalysisRequest;
import com.ticket.ops.common.model.SceneType;
import com.ticket.ops.common.signal.OpsSignal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEngineTest {

    private final RuleEngine engine = new RuleEngine(new RuleProperties(
            new RuleProperties.FullGc("fullgc-trigger", 3, 1000, 300),
            new RuleProperties.SlowSql("slow-sql-trigger", 10, 2000, 300),
            new RuleProperties.Dlq("dlq-trigger", 0, 60)));

    @Test
    void triggersAllThreeScenesWhenThresholdsBreached() {
        List<AnalysisRequest> requests = engine.evaluate(
                new OpsSignal("demo", 10, 500, 20, 500, 2));

        assertEquals(3, requests.size());
        assertTrue(requests.stream().anyMatch(request -> request.scene() == SceneType.FULLGC));
        assertTrue(requests.stream().anyMatch(request -> request.scene() == SceneType.SLOW_SQL));
        assertTrue(requests.stream().anyMatch(request -> request.scene() == SceneType.DLQ));
    }

    @Test
    void deduplicatesWithinWindow() {
        OpsSignal signal = new OpsSignal("dedup-service", 10, 500, 0, 0, 0);

        assertEquals(1, engine.evaluate(signal).size());
        assertTrue(engine.evaluate(signal).isEmpty());
    }

    @Test
    void noTriggerWhenBelowThresholds() {
        assertTrue(engine.evaluate(new OpsSignal("idle-service", 0, 100, 0, 100, 0)).isEmpty());
    }
}
