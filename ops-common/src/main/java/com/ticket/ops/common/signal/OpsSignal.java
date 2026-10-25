package com.ticket.ops.common.signal;

import com.ticket.ops.common.util.Text;

/**
 * 规则引擎输入：一次采集周期内的核心指标快照。
 * P0 用请求体直接上报，后续可替换为 Prometheus/日志采集器。
 */
public record OpsSignal(
        String serviceName,
        double gcCount5m,
        double gcAvgPauseMs,
        double slowSqlCountPerMin,
        double slowSqlP99Ms,
        long dlqDepth) {

    public OpsSignal {
        serviceName = Text.defaultIfBlank(serviceName, "unknown-service");
        gcCount5m = Math.max(0.0, gcCount5m);
        gcAvgPauseMs = Math.max(0.0, gcAvgPauseMs);
        slowSqlCountPerMin = Math.max(0.0, slowSqlCountPerMin);
        slowSqlP99Ms = Math.max(0.0, slowSqlP99Ms);
        dlqDepth = Math.max(0L, dlqDepth);
    }
}
