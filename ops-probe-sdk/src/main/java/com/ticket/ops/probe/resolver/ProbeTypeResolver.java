package com.ticket.ops.probe.resolver;

import com.ticket.ops.probe.model.ProbeType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * AUTO 类型推断，规则与设计文档 4.1.2 一致。
 */
public class ProbeTypeResolver {

    public ProbeType resolve(Method method, Class<?> targetClass) {
        if (hasAnnotation(targetClass, "KafkaListener")
                || hasAnnotation(targetClass, "RabbitListener")
                || hasAnnotation(targetClass, "RocketMQMessageListener")
                || hasAnnotation(method, "KafkaListener")
                || hasAnnotation(method, "RabbitListener")
                || hasAnnotation(method, "RocketMQMessageListener")) {
            return ProbeType.MQ_CONTEXT;
        }

        String className = targetClass.getSimpleName();
        if (className.matches(".*(Mapper|Repository|Dao|DAO)$")) {
            return ProbeType.SQL_CONTEXT;
        }

        String methodName = method.getName().toLowerCase();
        if (methodName.matches(".*(batch|export|queryall|listall|report).*")
                || Collection.class.isAssignableFrom(method.getReturnType())
                || method.getReturnType().isArray()) {
            return ProbeType.MEMORY_PRESSURE;
        }
        return ProbeType.GENERAL;
    }

    private boolean hasAnnotation(Class<?> type, String simpleName) {
        if (type == null) {
            return false;
        }
        for (Annotation annotation : type.getAnnotations()) {
            if (annotation.annotationType().getSimpleName().equals(simpleName)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnnotation(Method method, String simpleName) {
        if (method == null) {
            return false;
        }
        for (Annotation annotation : method.getAnnotations()) {
            if (annotation.annotationType().getSimpleName().equals(simpleName)) {
                return true;
            }
        }
        return false;
    }
}
