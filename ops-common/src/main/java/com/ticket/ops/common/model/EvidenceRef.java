package com.ticket.ops.common.model;

import com.ticket.ops.common.util.Text;

import java.util.Objects;

/**
 * 证据引用：报告中只保留可追溯的摘要与原始数据地址。
 */
public record EvidenceRef(String evidenceId, EvidenceType type, String summary, String rawRef) {

    public EvidenceRef {
        evidenceId = Text.requireNonBlank(evidenceId, "evidenceId");
        type = Objects.requireNonNull(type, "type 不能为空");
        summary = summary == null ? "" : summary;
        rawRef = rawRef == null ? "" : rawRef;
    }
}
