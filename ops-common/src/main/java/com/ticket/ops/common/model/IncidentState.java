package com.ticket.ops.common.model;

/**
 * 事故生命周期状态，与设计文档 4.3.1 一致。
 */
public enum IncidentState {
    RECEIVED,
    ANALYZING,
    GATE_CHECK,
    APPROVING,
    EXECUTING,
    VERIFYING,
    ARCHIVED,
    ROLLED_BACK,
    FAILED
}
