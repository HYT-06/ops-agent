package com.ticket.ops.config;

import com.ticket.ops.knowledge.audit.AuditLedger;
import com.ticket.ops.knowledge.audit.InMemoryAuditLedger;
import com.ticket.ops.knowledge.governance.DefaultKnowledgeRiskPolicy;
import com.ticket.ops.knowledge.governance.KnowledgeApprovalSink;
import com.ticket.ops.knowledge.governance.KnowledgeConflictDetector;
import com.ticket.ops.knowledge.governance.KnowledgeGovernanceService;
import com.ticket.ops.knowledge.governance.KnowledgeRiskPolicy;
import com.ticket.ops.knowledge.governance.LoggingKnowledgeApprovalSink;
import com.ticket.ops.knowledge.repository.InMemoryKnowledgeRepository;
import com.ticket.ops.knowledge.repository.KnowledgeRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 知识层装配。P0 使用内存实现，P4 替换为 MySQL + 向量库。
 */
@Configuration
public class KnowledgeConfig {

    @Bean
    public KnowledgeRepository knowledgeRepository() {
        return new InMemoryKnowledgeRepository();
    }

    @Bean
    public AuditLedger auditLedger() {
        return new InMemoryAuditLedger();
    }

    @Bean
    public KnowledgeConflictDetector knowledgeConflictDetector(KnowledgeRepository repository) {
        return new KnowledgeConflictDetector(repository);
    }

    @Bean
    public KnowledgeRiskPolicy knowledgeRiskPolicy() {
        return new DefaultKnowledgeRiskPolicy();
    }

    @Bean
    public KnowledgeApprovalSink knowledgeApprovalSink() {
        return new LoggingKnowledgeApprovalSink();
    }

    @Bean
    public KnowledgeGovernanceService knowledgeGovernanceService(
            KnowledgeRepository repository,
            KnowledgeConflictDetector conflictDetector,
            KnowledgeRiskPolicy riskPolicy,
            KnowledgeApprovalSink approvalSink,
            AuditLedger auditLedger,
            OpsAgentProperties properties) {
        return new KnowledgeGovernanceService(repository, conflictDetector, riskPolicy,
                approvalSink, auditLedger,
                properties.knowledge().autoPromoteThreshold(),
                properties.knowledge().autoPromoteConfidence());
    }
}
