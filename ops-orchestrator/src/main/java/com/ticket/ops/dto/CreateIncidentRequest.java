package com.ticket.ops.dto;

import com.ticket.ops.common.model.AnalysisRequest;
import com.ticket.ops.common.model.SceneType;
import com.ticket.ops.common.model.TriggerSignal;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 人工创建运维事件请求。
 */
public record CreateIncidentRequest(
        @NotNull(message = "scene 不能为空") SceneType scene,
        @NotBlank(message = "resourceId 不能为空") String resourceId,
        String summary,
        Map<String, Object> attrs,
        Map<String, Object> metrics) {

    public AnalysisRequest toAnalysisRequest() {
        TriggerSignal signal = new TriggerSignal("manual", summary, metrics);
        return AnalysisRequest.create(scene, resourceId, signal, attrs);
    }
}
