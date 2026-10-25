package com.ticket.ops.gate;

import com.ticket.ops.common.util.Text;
import com.ticket.ops.gate.model.BoundCheckResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 参数硬边界校验。即使人工审批通过，越界参数也不可执行。
 */
public class ParameterBoundValidator {

    public record Bound(String parameter, double min, double max) {

        public Bound {
            parameter = Text.requireNonBlank(parameter, "bound.parameter");
            if (min > max) {
                throw new IllegalArgumentException("参数边界非法: " + parameter + " min=" + min + " max=" + max);
            }
        }
    }

    private final Map<String, List<Bound>> boundsByTool;

    public ParameterBoundValidator(Map<String, List<Bound>> boundsByTool) {
        Map<String, List<Bound>> copy = new HashMap<>();
        if (boundsByTool != null) {
            boundsByTool.forEach((tool, bounds) -> copy.put(tool, List.copyOf(bounds)));
        }
        this.boundsByTool = Map.copyOf(copy);
    }

    public BoundCheckResult validate(String toolName, Map<String, Object> params) {
        List<Bound> bounds = boundsByTool.getOrDefault(toolName, List.of());
        if (bounds.isEmpty() || params == null) {
            return BoundCheckResult.ok();
        }
        for (Bound bound : bounds) {
            Object raw = params.get(bound.parameter());
            if (raw == null) {
                continue;
            }
            Double value = toDouble(raw);
            if (value == null) {
                return BoundCheckResult.fail(bound.parameter() + " 不是数值: " + raw);
            }
            if (value < bound.min() || value > bound.max()) {
                return BoundCheckResult.fail(bound.parameter() + "=" + value
                        + " 超出允许范围 [" + bound.min() + ", " + bound.max() + "]");
            }
        }
        return BoundCheckResult.ok();
    }

    private Double toDouble(Object raw) {
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        if (raw instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public static Map<String, List<Bound>> defaultBounds() {
        Map<String, List<Bound>> bounds = new HashMap<>();
        bounds.put("exec.updateThreadPool", new ArrayList<>(List.of(
                new Bound("coreSize", 8, 64),
                new Bound("maxSize", 8, 128))));
        bounds.put("exec.scaleConsumer", new ArrayList<>(List.of(
                new Bound("consumerCount", 1, 16))));
        bounds.put("dlq.replay", new ArrayList<>(List.of(
                new Bound("batchSize", 1, 100))));
        return bounds;
    }
}
