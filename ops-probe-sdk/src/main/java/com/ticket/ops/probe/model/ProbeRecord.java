package com.ticket.ops.probe.model;

import com.ticket.ops.probe.util.ProbeText;

import java.time.Instant;
import java.util.Objects;

/**
 * 单条探针记录。参数在切面内完成脱敏后才允许进入该对象。
 */
public record ProbeRecord(
        ProbeType type,
        String methodName,
        String threadName,
        long durationMs,
        Instant timestamp,
        String errorClass,
        String errorMsg,
        String bizKey,
        Long argsBytes,
        Long resultBytes,
        String params) {

    public ProbeRecord {
        type = Objects.requireNonNull(type, "type 不能为空");
        methodName = ProbeText.defaultIfBlank(methodName, "");
        threadName = ProbeText.defaultIfBlank(threadName, "");
        durationMs = Math.max(0L, durationMs);
        timestamp = timestamp == null ? Instant.now() : timestamp;
        errorMsg = ProbeText.truncate(errorMsg, 512);
    }
}
