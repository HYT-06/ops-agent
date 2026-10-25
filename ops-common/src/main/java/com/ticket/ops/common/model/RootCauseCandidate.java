package com.ticket.ops.common.model;

import com.ticket.ops.common.util.Text;

import java.util.List;

/**
 * 单个根因候选。
 */
public record RootCauseCandidate(String cause, double confidence, List<String> evidenceIds) {

    public RootCauseCandidate {
        cause = Text.requireNonBlank(cause, "cause");
        confidence = Math.max(0.0, Math.min(1.0, confidence));
        evidenceIds = evidenceIds == null ? List.of() : List.copyOf(evidenceIds);
    }
}
