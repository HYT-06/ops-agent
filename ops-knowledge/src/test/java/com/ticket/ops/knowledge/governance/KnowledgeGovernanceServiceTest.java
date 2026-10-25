package com.ticket.ops.knowledge.governance;

import com.ticket.ops.common.model.IncidentContext;
import com.ticket.ops.common.model.SceneType;
import com.ticket.ops.common.model.VerificationResult;
import com.ticket.ops.knowledge.audit.InMemoryAuditLedger;
import com.ticket.ops.knowledge.knowledge.KnowledgeEntry;
import com.ticket.ops.knowledge.knowledge.KnowledgeLayer;
import com.ticket.ops.knowledge.repository.InMemoryKnowledgeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeGovernanceServiceTest {

    private InMemoryKnowledgeRepository repository;
    private RecordingApprovalSink approvalSink;
    private KnowledgeGovernanceService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryKnowledgeRepository();
        approvalSink = new RecordingApprovalSink();
        service = new KnowledgeGovernanceService(
                repository,
                new KnowledgeConflictDetector(repository),
                new DefaultKnowledgeRiskPolicy(),
                approvalSink,
                new InMemoryAuditLedger(),
                3,
                0.75);
    }

    @Test
    void highRiskKnowledgeNeverAutoPromotes() {
        KnowledgeEntry saved = repository.save(KnowledgeEntry.candidate(
                0, SceneType.FULLGC, "调整 GC 参数", "调整 GC 参数",
                "resource=demo", "jvm.updateGcParam", 0.9, "req-1"));
        IncidentContext context = context("req-1");

        for (int i = 0; i < 4; i++) {
            service.onVerified(context, VerificationResult.pass("ok"));
        }

        KnowledgeEntry current = repository.findById(saved.id()).orElseThrow();
        assertEquals(KnowledgeLayer.CANDIDATE, current.layer());
        assertEquals(4, current.verifiedCount());
        assertEquals(4, approvalSink.entries.size());
    }

    @Test
    void lowRiskKnowledgeAutoPromotesAfterThreshold() {
        KnowledgeEntry saved = repository.save(KnowledgeEntry.candidate(
                0, SceneType.FULLGC, "扩容消费者", "扩容消费者",
                "resource=demo", "exec.scaleConsumer", 0.9, "req-2"));
        IncidentContext context = context("req-2");

        service.onVerified(context, VerificationResult.pass("ok"));
        service.onVerified(context, VerificationResult.pass("ok"));
        assertEquals(KnowledgeLayer.CANDIDATE, repository.findById(saved.id()).orElseThrow().layer());

        service.onVerified(context, VerificationResult.pass("ok"));

        assertEquals(KnowledgeLayer.VERIFIED, repository.findById(saved.id()).orElseThrow().layer());
        assertTrue(approvalSink.entries.isEmpty());
    }

    @Test
    void conflictWithVerifiedKnowledgeBlocksAutoPromotion() {
        KnowledgeEntry verified = KnowledgeEntry.candidate(
                0, SceneType.FULLGC, "线程池过小导致 FullGC", "线程池过小导致 FullGC",
                "resource=demo", "exec.updateThreadPool", 0.9, "old")
                .withLayer(KnowledgeLayer.VERIFIED);
        repository.save(verified);

        KnowledgeEntry candidate = repository.save(KnowledgeEntry.candidate(
                0, SceneType.FULLGC, "线程池过小导致 FullGC", "线程池过小导致 FullGC",
                "resource=demo", "jvm.updateGcParam", 0.9, "req-3"));
        IncidentContext context = context("req-3");

        for (int i = 0; i < 3; i++) {
            service.onVerified(context, VerificationResult.pass("ok"));
        }

        assertEquals(KnowledgeLayer.CANDIDATE,
                repository.findById(candidate.id()).orElseThrow().layer());
        assertEquals(3, approvalSink.entries.size());
    }

    private IncidentContext context(String requestId) {
        return new IncidentContext(requestId, SceneType.FULLGC, "demo", Set.of());
    }

    static class RecordingApprovalSink implements KnowledgeApprovalSink {

        final List<KnowledgeEntry> entries = new ArrayList<>();

        @Override
        public void sendKnowledgeApproval(KnowledgeEntry entry) {
            entries.add(entry);
        }
    }
}
