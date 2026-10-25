package com.ticket.ops.knowledge.repository;

import com.ticket.ops.common.model.SceneType;
import com.ticket.ops.knowledge.knowledge.KnowledgeEntry;
import com.ticket.ops.knowledge.knowledge.KnowledgeLayer;

import java.util.List;
import java.util.Optional;

/**
 * 知识库仓储。P0 内存实现，P4 替换为 MySQL + 向量库。
 */
public interface KnowledgeRepository {

    KnowledgeEntry save(KnowledgeEntry entry);

    Optional<KnowledgeEntry> findById(long id);

    List<KnowledgeEntry> findCandidatesByIncident(String requestId);

    List<KnowledgeEntry> searchVerified(SceneType scene, String rootCausePattern, int limit);

    Optional<KnowledgeEntry> incrementVerified(long id);

    Optional<KnowledgeEntry> incrementRejected(long id);

    Optional<KnowledgeEntry> promote(long id, KnowledgeLayer layer);
}
