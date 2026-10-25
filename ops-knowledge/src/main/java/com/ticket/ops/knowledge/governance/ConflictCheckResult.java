package com.ticket.ops.knowledge.governance;

import com.ticket.ops.knowledge.knowledge.KnowledgeEntry;

/**
 * 冲突检测结果。存在冲突时禁止自动晋升，转人工裁决。
 */
public record ConflictCheckResult(boolean conflict, KnowledgeEntry existingEntry) {

    public static ConflictCheckResult ok() {
        return new ConflictCheckResult(false, null);
    }

    public static ConflictCheckResult conflict(KnowledgeEntry existing) {
        return new ConflictCheckResult(true, existing);
    }
}
