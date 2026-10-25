package com.ticket.ops.common.model;

import java.util.Set;

/**
 * 一次事故在执行层面的上下文。
 */
public record IncidentContext(
        String requestId,
        SceneType scene,
        String resourceId,
        Set<String> affectedInstances) {

    public IncidentContext {
        affectedInstances = affectedInstances == null ? Set.of() : Set.copyOf(affectedInstances);
    }

    public static IncidentContext of(AnalysisRequest request) {
        return new IncidentContext(request.requestId(), request.scene(), request.resourceId(), Set.of());
    }
}
