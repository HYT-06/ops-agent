package com.ticket.ops.eval.dataset;

import com.ticket.ops.common.model.SceneType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * P0 示例评测集。后续按设计文档扩充到 20~30 条并接入历史案例。
 */
public final class EvalDataset {

    private EvalDataset() {
    }

    public static List<EvalCase> defaultCases() {
        return List.of(
                new EvalCase("fullgc-001", SceneType.FULLGC,
                        "FullGC 频率突增，堆内存持续上涨",
                        Map.of("gcCount5m", 20, "gcAvgPauseMs", 1800),
                        "内存泄漏",
                        "jvm.updateGcParam",
                        Set.of("jvm.updateGcParam", "exec.updateThreadPool")),
                new EvalCase("slow-sql-001", SceneType.SLOW_SQL,
                        "订单查询 P99 超过 2 秒",
                        Map.of("slowSqlP99Ms", 3500, "slowSqlCountPerMin", 30),
                        "订单表索引缺失",
                        "sql.addIndex",
                        Set.of("sql.addIndex", "sql.executeDDL")),
                new EvalCase("dlq-001", SceneType.DLQ,
                        "下单消息进入死信队列",
                        Map.of("dlqDepth", 5),
                        "下游依赖超时",
                        "dlq.replay",
                        Set.of("dlq.replay"))
        );
    }
}
