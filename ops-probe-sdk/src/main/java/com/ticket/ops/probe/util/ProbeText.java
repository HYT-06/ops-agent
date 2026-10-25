package com.ticket.ops.probe.util;

/**
 * SDK 内部轻量文本工具，保证 ops-probe-sdk 可独立打包注入被监控应用。
 */
public final class ProbeText {

    private ProbeText() {
    }

    public static String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    public static String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
