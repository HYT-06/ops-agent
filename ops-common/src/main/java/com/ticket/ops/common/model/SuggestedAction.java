package com.ticket.ops.common.model;

import com.ticket.ops.common.util.Maps;
import com.ticket.ops.common.util.Text;

import java.util.Map;

/**
 * 结构化执行建议：工具名 + 参数 + 回滚点 + 幂等键，缺一不可。
 */
public record SuggestedAction(
        String toolName,
        Map<String, Object> params,
        String rollbackPoint,
        String idempotencyKey) {

    public SuggestedAction {
        toolName = Text.defaultIfBlank(toolName, "");
        params = Maps.immutable(params);
        rollbackPoint = Text.defaultIfBlank(rollbackPoint, "");
        idempotencyKey = Text.defaultIfBlank(idempotencyKey, "");
    }

    /**
     * 三件套完整性校验：工具名、参数、回滚点、幂等键。
     */
    public boolean isComplete() {
        return !toolName.isBlank() && !rollbackPoint.isBlank() && !idempotencyKey.isBlank();
    }
}
