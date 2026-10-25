package com.ticket.ops.knowledge.repository;

import com.ticket.ops.common.model.SceneType;
import com.ticket.ops.knowledge.knowledge.KnowledgeEntry;
import com.ticket.ops.knowledge.knowledge.KnowledgeLayer;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * P0 内存知识库。
 */
public class InMemoryKnowledgeRepository implements KnowledgeRepository {

    private final AtomicLong idSequence = new AtomicLong();
    private final Map<Long, KnowledgeEntry> entries = new ConcurrentHashMap<>();

    @Override
    public synchronized KnowledgeEntry save(KnowledgeEntry entry) {
        long id = entry.id() > 0 ? entry.id() : idSequence.incrementAndGet();
        KnowledgeEntry stored = new KnowledgeEntry(id, entry.scene(), entry.layer(), entry.content(),
                entry.rootCausePattern(), entry.applicableBoundary(), entry.confidence(),
                entry.verifiedCount(), entry.rejectedCount(), entry.suggestedToolName(),
                entry.sourceIncidentIds(), entry.createdAt(), entry.lastVerifiedAt(), entry.deprecatedAt());
        entries.put(id, stored);
        return stored;
    }

    @Override
    public Optional<KnowledgeEntry> findById(long id) {
        return Optional.ofNullable(entries.get(id));
    }

    @Override
    public List<KnowledgeEntry> findCandidatesByIncident(String requestId) {
        return entries.values().stream()
                .filter(entry -> entry.layer() == KnowledgeLayer.CANDIDATE)
                .filter(entry -> entry.sourceIncidentIds().contains(requestId))
                .sorted(Comparator.comparingLong(KnowledgeEntry::id))
                .toList();
    }

    @Override
    public List<KnowledgeEntry> searchVerified(SceneType scene, String rootCausePattern, int limit) {
        int max = Math.max(1, limit);
        return entries.values().stream()
                .filter(entry -> entry.layer() == KnowledgeLayer.VERIFIED)
                .filter(entry -> entry.scene() == scene)
                .limit(max)
                .toList();
    }

    @Override
    public synchronized Optional<KnowledgeEntry> incrementVerified(long id) {
        return update(id, KnowledgeEntry::verifiedOnce);
    }

    @Override
    public synchronized Optional<KnowledgeEntry> incrementRejected(long id) {
        return update(id, KnowledgeEntry::rejectedOnce);
    }

    @Override
    public synchronized Optional<KnowledgeEntry> promote(long id, KnowledgeLayer layer) {
        return update(id, entry -> entry.withLayer(layer));
    }

    private Optional<KnowledgeEntry> update(long id, java.util.function.UnaryOperator<KnowledgeEntry> updater) {
        KnowledgeEntry existing = entries.get(id);
        if (existing == null) {
            return Optional.empty();
        }
        KnowledgeEntry updated = updater.apply(existing);
        entries.put(id, updated);
        return Optional.of(updated);
    }
}
