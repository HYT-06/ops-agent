package com.ticket.ops.common.model;

import com.ticket.ops.common.util.Text;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * 分析 Agent 的结构化输出。
 */
public record AnalysisReport(
        String requestId,
        SceneType scene,
        List<RootCauseCandidate> rootCauses,
        SuggestedAction suggestedAction,
        double confidence,
        List<String> riskWarnings,
        List<EvidenceRef> evidenceRefs,
        Instant generatedAt) {

    public AnalysisReport {
        requestId = Text.requireNonBlank(requestId, "requestId");
        scene = Objects.requireNonNull(scene, "scene 不能为空");
        rootCauses = rootCauses == null ? List.of() : List.copyOf(rootCauses);
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        riskWarnings = riskWarnings == null ? List.of() : List.copyOf(riskWarnings);
        evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
        generatedAt = generatedAt == null ? Instant.now() : generatedAt;
    }

    /**
     * P0 占位实现：不产出执行建议，门禁将判定为 NO_ACTION。
     */
    public static AnalysisReport placeholder(AnalysisRequest request, String message) {
        return new AnalysisReport(request.requestId(), request.scene(), List.of(), null,
                0.0, List.of(message), List.of(), Instant.now());
    }

    public static AnalysisReport failed(String requestId, SceneType scene, String message) {
        return new AnalysisReport(requestId, scene, List.of(), null, 0.0,
                List.of(message), List.of(), Instant.now());
    }
}
