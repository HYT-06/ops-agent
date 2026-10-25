package com.ticket.ops.common.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 小工具：保留 null 值的不可变 Map 复制，避免 Map.copyOf 在反序列化含 null 时抛异常。
 */
public final class Maps {

    private Maps() {
    }

    public static <K, V> Map<K, V> immutable(Map<K, V> source) {
        if (source == null || source.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(source));
    }
}
