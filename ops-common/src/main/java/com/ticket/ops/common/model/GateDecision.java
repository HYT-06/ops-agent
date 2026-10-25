package com.ticket.ops.common.model;

/**
 * 门禁判定结果。
 */
public enum GateDecision {
    /** 仅诊断，无执行建议 */
    NO_ACTION,
    /** 低风险，自动放行 */
    AUTO_APPROVED,
    /** 高风险，等待人工审批 */
    REQUIRES_APPROVAL,
    /** 校验失败，拒绝 */
    REJECTED
}
