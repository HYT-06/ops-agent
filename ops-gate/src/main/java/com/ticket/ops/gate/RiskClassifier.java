package com.ticket.ops.gate;

import com.ticket.ops.common.model.IncidentContext;
import com.ticket.ops.common.model.RiskLevel;
import com.ticket.ops.common.model.SuggestedAction;

/**
 * 风险分类器。规则优先级从高到低，未识别动作默认高风险。
 */
public class RiskClassifier {

    public RiskLevel classify(SuggestedAction action, IncidentContext context) {
        String tool = action.toolName();
        if (tool.startsWith("dlq.replay")
                || tool.startsWith("sql.addIndex")
                || tool.startsWith("sql.executeDDL")
                || tool.equals("jvm.updateGcParam")) {
            return RiskLevel.HIGH;
        }
        if (tool.equals("exec.updateThreadPool")) {
            Double current = number(action.params().get("currentCoreSize"));
            Double target = number(action.params().get("coreSize"));
            if (current != null && target != null && current > 0 && target / current <= 1.3) {
                return RiskLevel.LOW;
            }
            return RiskLevel.HIGH;
        }
        if (tool.equals("exec.scaleConsumer")) {
            return RiskLevel.LOW;
        }
        return RiskLevel.HIGH;
    }

    private Double number(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
