package com.ticket.ops.analyzer.skill;

import com.ticket.ops.common.model.AnalysisRequest;
import com.ticket.ops.common.model.SceneType;
import com.ticket.ops.common.model.TriggerSignal;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AnalysisSkillRegistryTest {

    private final FullGcSkill fullGc = new FullGcSkill();
    private final SlowSqlSkill slowSql = new SlowSqlSkill();
    private final DlqSkill dlq = new DlqSkill();

    @Test
    void resolvesSkillByScene() {
        AnalysisSkillRegistry registry = new AnalysisSkillRegistry(List.of(fullGc, slowSql, dlq));
        assertSame(fullGc, registry.get(SceneType.FULLGC));
        assertSame(slowSql, registry.get(SceneType.SLOW_SQL));
        assertSame(dlq, registry.get(SceneType.DLQ));
    }

    @Test
    void rejectsDuplicateSceneRegistrations() {
        assertThrows(IllegalArgumentException.class,
                () -> new AnalysisSkillRegistry(List.of(fullGc, new FullGcSkill())));
    }

    @Test
    void placeholderReportHasNoAction() {
        AnalysisRequest request = AnalysisRequest.create(SceneType.FULLGC, "demo",
                TriggerSignal.manual("test"), Map.of());
        var context = com.ticket.ops.analyzer.model.AnalysisContext.of(request);
        var report = fullGc.analyze(context);
        assertEquals(SceneType.FULLGC, report.scene());
        assertEquals(null, report.suggestedAction());
        assertEquals(0.0, report.confidence());
    }
}
