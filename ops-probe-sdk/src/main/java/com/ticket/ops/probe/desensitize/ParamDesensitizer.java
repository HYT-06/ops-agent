package com.ticket.ops.probe.desensitize;

import com.ticket.ops.probe.util.ProbeText;

import java.util.StringJoiner;
import java.util.regex.Pattern;

/**
 * 参数脱敏。必须在切面内完成，不得依赖下游。
 */
public class ParamDesensitizer {

    private static final Pattern MOBILE = Pattern.compile("1[3-9]\\d{9}");
    private static final Pattern ID_CARD = Pattern.compile("\\d{17}[\\dXx]");
    private static final Pattern BANK_CARD = Pattern.compile("\\d{16,19}");
    private static final Pattern EMAIL = Pattern.compile("[\\w.-]+@[\\w.-]+");

    public String mask(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        for (Object arg : args) {
            joiner.add(maskOne(arg));
        }
        return joiner.toString();
    }

    public String maskString(String value) {
        if (value == null) {
            return null;
        }
        String masked = ID_CARD.matcher(value).replaceAll("******************");
        masked = MOBILE.matcher(masked).replaceAll("1**********");
        masked = BANK_CARD.matcher(masked).replaceAll("**** **** **** ****");
        return EMAIL.matcher(masked).replaceAll("***@***");
    }

    private String maskOne(Object value) {
        if (value == null) {
            return "null";
        }
        if (value instanceof Number || value instanceof Boolean || value instanceof Enum<?>) {
            return String.valueOf(value);
        }
        return ProbeText.truncate(maskString(String.valueOf(value)), 256);
    }
}
