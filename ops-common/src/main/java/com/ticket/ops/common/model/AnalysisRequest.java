package com.ticket.ops.common.model;

import com.ticket.ops.common.util.Maps;
import com.ticket.ops.common.util.Text;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 由规则引擎或人工入口产生的标准化分析请求。
 */
public record AnalysisRequest(
        String requestId,
        SceneType scene,
        String resourceId,
        Instant triggerTime,
        TriggerSignal signal,
        Map<String, Object> attrs) {

    public AnalysisRequest {
        requestId = Text.requireNonBlank(requestId, "requestId");
        scene = Objects.requireNonNull(scene, "scene 不能为空");
        resourceId = Text.requireNonBlank(resourceId, "resourceId");
        triggerTime = triggerTime == null ? Instant.now() : triggerTime;
        signal = signal == null ? TriggerSignal.manual("") : signal;
        attrs = Maps.immutable(attrs);
    }

    public static AnalysisRequest create(SceneType scene, String resourceId,
                                         TriggerSignal signal, Map<String, Object> attrs) {
        return new AnalysisRequest(UUID.randomUUID().toString(), scene, resourceId,
                Instant.now(), signal, attrs);
    }
}
