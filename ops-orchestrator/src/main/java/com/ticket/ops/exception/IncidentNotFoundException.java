package com.ticket.ops.exception;

/**
 * 事故不存在。
 */
public class IncidentNotFoundException extends RuntimeException {

    public IncidentNotFoundException(String requestId) {
        super("事故不存在: " + requestId);
    }
}
