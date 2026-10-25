package com.ticket.ops.gate.approval;

import com.ticket.ops.common.model.AnalysisReport;
import com.ticket.ops.common.model.EvidenceRef;
import com.ticket.ops.common.model.IncidentContext;
import com.ticket.ops.common.model.SuggestedAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

/**
 * P0 审批通知：写日志。卡片必须包含完整三件套 + 证据摘要。
 */
public class LoggingApprovalNotifier implements ApprovalNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingApprovalNotifier.class);

    @Override
    public void notify(SuggestedAction action, AnalysisReport report,
                       IncidentContext context, String callbackId) {
        if (!action.isComplete()) {
            throw new IllegalArgumentException("审批卡片三件套不完整，拒绝发送");
        }
        String evidence = report.evidenceRefs().stream()
                .map(EvidenceRef::summary)
                .collect(Collectors.joining("\n"));
        log.info("""
                        【变更审批请求】 #{}
                        场景：{}
                        资源：{}
                        建议动作：{}
                        参数：{}
                        回滚点：{}
                        证据摘要：
                        {}
                        风险等级：HIGH
                        回调 ID：{}
                        """,
                callbackId, report.scene(), context.resourceId(),
                action.toolName(), action.params(), action.rollbackPoint(),
                evidence, callbackId);
    }
}
