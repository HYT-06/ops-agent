package com.ticket.ops.probe.sink;

import com.ticket.ops.probe.model.ProbeRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 默认上报实现：只打印批次摘要，避免日志爆炸。
 */
public class LoggingProbeRecordSink implements ProbeRecordSink {

    private static final Logger log = LoggerFactory.getLogger(LoggingProbeRecordSink.class);

    @Override
    public void upload(List<ProbeRecord> batch) {
        if (batch == null || batch.isEmpty()) {
            return;
        }
        ProbeRecord first = batch.get(0);
        log.debug("ops-probe 批量上报 {} 条，首条: type={}, method={}, durationMs={}, bizKey={}",
                batch.size(), first.type(), first.methodName(), first.durationMs(), first.bizKey());
    }
}
