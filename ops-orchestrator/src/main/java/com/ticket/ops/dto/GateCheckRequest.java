package com.ticket.ops.dto;

import com.ticket.ops.common.model.SceneType;
import com.ticket.ops.common.model.SuggestedAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * P0 门禁诊断请求，仅用于框架验证，生产环境应关闭或加鉴权。
 */
public record GateCheckRequest(
        @NotNull(message = "scene 不能为空") SceneType scene,
        @NotBlank(message = "resourceId 不能为空") String resourceId,
        @NotBlank(message = "toolName 不能为空") String toolName,
        Map<String, Object> params,
        @NotBlank(message = "rollbackPoint 不能为空") String rollbackPoint,
        @NotBlank(message = "idempotencyKey 不能为空") String idempotencyKey) {

    public SuggestedAction toSuggestedAction() {
        return new SuggestedAction(toolName, params, rollbackPoint, idempotencyKey);
    }
}
