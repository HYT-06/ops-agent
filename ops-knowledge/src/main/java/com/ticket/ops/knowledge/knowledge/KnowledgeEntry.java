package com.ticket.ops.knowledge.knowledge;

import com.ticket.ops.common.model.SceneType;
import com.ticket.ops.common.util.Text;

import java.time.Instant;
import java.util.List;

/**
 * 知识条目。P0 使用内存模型，字段与文档 knowledge_entry 表对齐。
 */
public record KnowledgeEntry(
        long id,
        SceneType scene,
        KnowledgeLayer layer,
        String content,
        String rootCausePattern,
        String applicableBoundary,
        double confidence,
        int verifiedCount,
        int rejectedCount,
        String suggestedToolName,
        List<String> sourceIncidentIds,
        Instant createdAt,
        Instant lastVerifiedAt,
        Instant deprecatedAt) {

    public KnowledgeEntry {
        scene = java.util.Objects.requireNonNull(scene, "scene 不能为空");
        layer = layer == null ? KnowledgeLayer.CANDIDATE : layer;
        content = Text.requireNonBlank(content, "content");
        rootCausePattern = Text.defaultIfBlank(rootCausePattern, content);
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        sourceIncidentIds = sourceIncidentIds == null ? List.of() : List.copyOf(sourceIncidentIds);
        suggestedToolName = Text.defaultIfBlank(suggestedToolName, "");
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static KnowledgeEntry candidate(long id, SceneType scene, String content,
                                          String rootCausePattern, String applicableBoundary,
                                          String suggestedToolName, double confidence,
                                          String requestId) {
        return new KnowledgeEntry(id, scene, KnowledgeLayer.CANDIDATE, content,
                rootCausePattern, applicableBoundary, confidence, 0, 0,
                suggestedToolName,
                requestId == null || requestId.isBlank() ? List.of() : List.of(requestId),
                Instant.now(), null, null);
    }

    public KnowledgeEntry verifiedOnce() {
        return new KnowledgeEntry(id, scene, layer, content, rootCausePattern, applicableBoundary,
                confidence, verifiedCount + 1, rejectedCount, suggestedToolName,
                sourceIncidentIds, createdAt, Instant.now(), deprecatedAt);
    }

    public KnowledgeEntry rejectedOnce() {
        return new KnowledgeEntry(id, scene, layer, content, rootCausePattern, applicableBoundary,
                confidence, verifiedCount, rejectedCount + 1, suggestedToolName,
                sourceIncidentIds, createdAt, lastVerifiedAt, deprecatedAt);
    }

    public KnowledgeEntry withLayer(KnowledgeLayer newLayer) {
        return new KnowledgeEntry(id, scene, newLayer, content, rootCausePattern, applicableBoundary,
                confidence, verifiedCount, rejectedCount, suggestedToolName,
                sourceIncidentIds, createdAt, lastVerifiedAt,
                newLayer == KnowledgeLayer.DEPRECATED ? Instant.now() : deprecatedAt);
    }
}
