package com.ticket.ops.probe.spel;

import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 从方法参数中按 SpEL 提取业务标识。失败时返回 null，不影响采集主流程。
 */
public class SpelBizKeyExtractor {

    private final ExpressionParser parser = new SpelExpressionParser();

    public String extract(String expression, Method method, Object[] args, Object target) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            StandardEvaluationContext context = target == null
                    ? new StandardEvaluationContext()
                    : new StandardEvaluationContext(target);
            Parameter[] parameters = method.getParameters();
            Object[] safeArgs = args == null ? new Object[0] : args;
            for (int i = 0; i < safeArgs.length; i++) {
                context.setVariable("p" + i, safeArgs[i]);
                context.setVariable("a" + i, safeArgs[i]);
                if (i < parameters.length) {
                    context.setVariable(parameters[i].getName(), safeArgs[i]);
                }
            }
            Object value = parser.parseExpression(expression).getValue(context);
            return value == null ? null : String.valueOf(value);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
