package com.ticket.ops.dto;

import java.time.Instant;

/**
 * 统一错误响应。
 */
public record ErrorResponse(Instant timestamp, int status, String error, String message, String path) {
}
