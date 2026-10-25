package com.ticket.ops.rule;

import com.ticket.ops.common.model.AnalysisRequest;
import com.ticket.ops.common.model.SceneType;
import com.ticket.ops.common.model.TriggerSignal;
import com.ticket.ops.common.signal.OpsSignal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * L1 规则引擎：零 token，只做阈值判断、告警去重与标准化事件生成。
 */
public class RuleEngine {

    private final RuleProperties properties;
    private final Map<String, Instant> lastTriggered = new ConcurrentHashMap<>();

    public RuleEngine(RuleProperties properties) {
        this.properties = properties;
    }

    public List<AnalysisRequest> evaluate(OpsSignal signal) {
        List<AnalysisRequest> requests = new ArrayList<>();
        Instant now = Instant.now();

        RuleProperties.FullGc fullGc = properties.fullgc();
        if (signal.gcCount5m() > fullGc.maxGcCount5m()
                || signal.gcAvgPauseMs() > fullGc.maxAvgPauseMs()) {
            String dedupKey = fullGc.ruleId() + ":" + signal.serviceName();
            if (shouldTrigger(dedupKey, fullGc.dedupWindowSeconds(), now)) {
                requests.add(AnalysisRequest.create(SceneType.FULLGC, signal.serviceName(),
                        new TriggerSignal(fullGc.ruleId(),
                                "GC 频率/耗时超阈值: count5m=" + signal.gcCount5m()
                                        + ", avgPauseMs=" + signal.gcAvgPauseMs(),
                                Map.of("gcCount5m", signal.gcCount5m(),
                                        "gcAvgPauseMs", signal.gcAvgPauseMs())),
                        Map.of("serviceName", signal.serviceName())));
            }
        }

        RuleProperties.SlowSql slowSql = properties.slowSql();
        if (signal.slowSqlCountPerMin() > slowSql.maxSlowCountPerMin()
                || signal.slowSqlP99Ms() > slowSql.maxP99Ms()) {
            String dedupKey = slowSql.ruleId() + ":" + signal.serviceName();
            if (shouldTrigger(dedupKey, slowSql.dedupWindowSeconds(), now)) {
                requests.add(AnalysisRequest.create(SceneType.SLOW_SQL, signal.serviceName(),
                        new TriggerSignal(slowSql.ruleId(),
                                "慢 SQL 指标超阈值: countPerMin=" + signal.slowSqlCountPerMin()
                                        + ", p99Ms=" + signal.slowSqlP99Ms(),
                                Map.of("slowSqlCountPerMin", signal.slowSqlCountPerMin(),
                                        "slowSqlP99Ms", signal.slowSqlP99Ms())),
                        Map.of("serviceName", signal.serviceName())));
            }
        }

        RuleProperties.Dlq dlq = properties.dlq();
        if (signal.dlqDepth() > dlq.maxDepth()) {
            String dedupKey = dlq.ruleId() + ":" + signal.serviceName();
            if (shouldTrigger(dedupKey, dlq.dedupWindowSeconds(), now)) {
                requests.add(AnalysisRequest.create(SceneType.DLQ, signal.serviceName(),
                        new TriggerSignal(dlq.ruleId(),
                                "DLQ 深度超阈值: depth=" + signal.dlqDepth(),
                                Map.of("dlqDepth", signal.dlqDepth())),
                        Map.of("serviceName", signal.serviceName())));
            }
        }

        return List.copyOf(requests);
    }

    private boolean shouldTrigger(String dedupKey, int dedupWindowSeconds, Instant now) {
        AtomicBoolean triggered = new AtomicBoolean(false);
        lastTriggered.compute(dedupKey, (key, last) -> {
            if (last == null || !last.plusSeconds(dedupWindowSeconds).isAfter(now)) {
                triggered.set(true);
                return now;
            }
            return last;
        });
        return triggered.get();
    }
}
