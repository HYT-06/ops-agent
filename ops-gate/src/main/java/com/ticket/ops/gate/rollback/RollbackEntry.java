package com.ticket.ops.gate.rollback;

import com.ticket.ops.common.util.Maps;

import java.util.Map;

/**
 * 回滚点：记录执行前快照与幂等键。
 */
public record RollbackEntry(
        String requestId,
        String toolName,
        Map<String, Object> params,
        Map<String, Object> before,
        String idempotencyKey) {

    public RollbackEntry {
        params = Maps.immutable(params);
        before = Maps.immutable(before);
    }
}
