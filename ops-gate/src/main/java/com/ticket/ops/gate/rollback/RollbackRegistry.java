package com.ticket.ops.gate.rollback;

import com.ticket.ops.common.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 回滚注册表。
 * <p>
 * 硬约束：register 必须在写工具 execute 之前调用，保证任何失败都可回滚。
 */
public class RollbackRegistry {

    private static final Logger log = LoggerFactory.getLogger(RollbackRegistry.class);

    private final Map<String, RollbackEntry> entries = new ConcurrentHashMap<>();

    public void register(String requestId, String toolName, Map<String, Object> params,
                         Map<String, Object> before, String idempotencyKey) {
        entries.put(requestId, new RollbackEntry(requestId, toolName,
                Maps.immutable(params), Maps.immutable(before), idempotencyKey));
    }

    public Optional<RollbackEntry> find(String requestId) {
        return Optional.ofNullable(entries.get(requestId));
    }

    /**
     * 触发回滚。P0 记录日志并移除回滚点；P2 接入真实补偿动作。
     */
    public Optional<RollbackEntry> triggerRollback(String requestId) {
        RollbackEntry entry = entries.remove(requestId);
        if (entry != null) {
            log.warn("触发回滚: requestId={}, tool={}, before={}",
                    requestId, entry.toolName(), entry.before());
        }
        return Optional.ofNullable(entry);
    }

    public int size() {
        return entries.size();
    }
}
