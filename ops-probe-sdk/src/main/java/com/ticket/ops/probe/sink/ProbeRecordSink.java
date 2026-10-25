package com.ticket.ops.probe.sink;

import com.ticket.ops.probe.model.ProbeRecord;

import java.util.List;

/**
 * 探针批量上报出口。P0 默认写日志，后续可替换为 HTTP/Kafka 上报。
 */
public interface ProbeRecordSink {

    void upload(List<ProbeRecord> batch);
}
