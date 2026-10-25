package com.ticket.ops.knowledge.governance;

import com.ticket.ops.knowledge.knowledge.KnowledgeEntry;
import com.ticket.ops.knowledge.repository.KnowledgeRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 知识冲突检测：同场景、根因模式高度相似但处置动作不同 → 冲突。
 */
public class KnowledgeConflictDetector {

    private static final double SIMILARITY_THRESHOLD = 0.85;

    private final KnowledgeRepository repository;

    public KnowledgeConflictDetector(KnowledgeRepository repository) {
        this.repository = repository;
    }

    public ConflictCheckResult check(KnowledgeEntry candidate) {
        List<KnowledgeEntry> similar = repository.searchVerified(
                candidate.scene(), candidate.rootCausePattern(), 5);
        for (KnowledgeEntry existing : similar) {
            if (similarity(candidate.rootCausePattern(), existing.rootCausePattern()) >= SIMILARITY_THRESHOLD
                    && !sameAction(candidate, existing)) {
                return ConflictCheckResult.conflict(existing);
            }
        }
        return ConflictCheckResult.ok();
    }

    private boolean sameAction(KnowledgeEntry left, KnowledgeEntry right) {
        return Objects.equals(left.suggestedToolName(), right.suggestedToolName());
    }

    double similarity(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return 0.0;
        }
        Set<String> leftGrams = bigrams(left);
        Set<String> rightGrams = bigrams(right);
        if (leftGrams.isEmpty() || rightGrams.isEmpty()) {
            return left.equals(right) ? 1.0 : 0.0;
        }
        Set<String> intersection = new HashSet<>(leftGrams);
        intersection.retainAll(rightGrams);
        Set<String> union = new HashSet<>(leftGrams);
        union.addAll(rightGrams);
        return (double) intersection.size() / union.size();
    }

    private Set<String> bigrams(String value) {
        String normalized = value.replaceAll("\\s+", "");
        Set<String> grams = new HashSet<>();
        if (normalized.length() == 1) {
            grams.add(normalized);
            return grams;
        }
        for (int i = 0; i < normalized.length() - 1; i++) {
            grams.add(normalized.substring(i, i + 2));
        }
        return grams;
    }
}
