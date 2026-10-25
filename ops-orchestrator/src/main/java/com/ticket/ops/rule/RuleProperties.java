package com.ticket.ops.rule;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * L1 规则引擎阈值配置，P0 通过 application.yml 加载，后续可接 Nacos 热更新。
 */
@ConfigurationProperties(prefix = "ops.rules")
public record RuleProperties(FullGc fullgc, SlowSql slowSql, Dlq dlq) {

    public RuleProperties {
        fullgc = fullgc == null ? new FullGc("fullgc-trigger", 3, 1000, 300) : fullgc;
        slowSql = slowSql == null ? new SlowSql("slow-sql-trigger", 10, 2000, 300) : slowSql;
        dlq = dlq == null ? new Dlq("dlq-trigger", 0, 60) : dlq;
    }

    public record FullGc(String ruleId, double maxGcCount5m,
                         double maxAvgPauseMs, int dedupWindowSeconds) {

        public FullGc {
            ruleId = ruleId == null || ruleId.isBlank() ? "fullgc-trigger" : ruleId;
            dedupWindowSeconds = dedupWindowSeconds <= 0 ? 300 : dedupWindowSeconds;
        }
    }

    public record SlowSql(String ruleId, double maxSlowCountPerMin,
                          double maxP99Ms, int dedupWindowSeconds) {

        public SlowSql {
            ruleId = ruleId == null || ruleId.isBlank() ? "slow-sql-trigger" : ruleId;
            dedupWindowSeconds = dedupWindowSeconds <= 0 ? 300 : dedupWindowSeconds;
        }
    }

    public record Dlq(String ruleId, long maxDepth, int dedupWindowSeconds) {

        public Dlq {
            ruleId = ruleId == null || ruleId.isBlank() ? "dlq-trigger" : ruleId;
            dedupWindowSeconds = dedupWindowSeconds <= 0 ? 60 : dedupWindowSeconds;
        }
    }
}
