package com.ticket.ops.probe.aspect;

import com.ticket.ops.probe.util.ProbeText;
import com.ticket.ops.probe.annotation.OpsProbe;
import com.ticket.ops.probe.desensitize.ParamDesensitizer;
import com.ticket.ops.probe.model.ProbeRecord;
import com.ticket.ops.probe.model.ProbeType;
import com.ticket.ops.probe.queue.ProbeRecordQueue;
import com.ticket.ops.probe.resolver.ProbeTypeResolver;
import com.ticket.ops.probe.sampler.ProbeSampler;
import com.ticket.ops.probe.spel.SpelBizKeyExtractor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.Instant;

/**
 * 探针切面。
 * <p>
 * 硬约束：采集逻辑全部包在 try-catch 中，任何采集失败都不得影响业务方法。
 */
@Aspect
public class OpsProbeAspect {

    private static final Logger log = LoggerFactory.getLogger(OpsProbeAspect.class);

    private final ProbeTypeResolver typeResolver;
    private final ProbeSampler sampler;
    private final ParamDesensitizer desensitizer;
    private final SpelBizKeyExtractor bizKeyExtractor;
    private final ProbeRecordQueue queue;

    public OpsProbeAspect(ProbeTypeResolver typeResolver,
                          ProbeSampler sampler,
                          ParamDesensitizer desensitizer,
                          SpelBizKeyExtractor bizKeyExtractor,
                          ProbeRecordQueue queue) {
        this.typeResolver = typeResolver;
        this.sampler = sampler;
        this.desensitizer = desensitizer;
        this.bizKeyExtractor = bizKeyExtractor;
        this.queue = queue;
    }

    @Around("@annotation(probe)")
    public Object probe(ProceedingJoinPoint pjp, OpsProbe probe) throws Throwable {
        if (!sampler.hit(probe.sampleRate())) {
            return pjp.proceed();
        }
        long startNanos = System.nanoTime();
        Throwable error = null;
        Object result = null;
        try {
            result = pjp.proceed();
            return result;
        } catch (Throwable throwable) {
            error = throwable;
            throw throwable;
        } finally {
            try {
                record(pjp, probe, startNanos, result, error);
            } catch (Throwable collectError) {
                log.debug("ops-probe 采集失败，已忽略，不影响业务方法", collectError);
            }
        }
    }

    private void record(ProceedingJoinPoint pjp, OpsProbe probe,
                        long startNanos, Object result, Throwable error) {
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = pjp.getTarget() == null
                ? signature.getDeclaringType()
                : pjp.getTarget().getClass();
        ProbeType type = probe.value() == ProbeType.AUTO
                ? typeResolver.resolve(method, targetClass)
                : probe.value();

        Object[] args = pjp.getArgs();
        String params = type == ProbeType.SQL_CONTEXT && probe.desensitize()
                ? desensitizer.mask(args)
                : null;
        Long argsBytes = probe.recordSize() ? estimateSize(args) : null;
        Long resultBytes = probe.recordSize() ? estimateSize(result) : null;
        String bizKey = bizKeyExtractor.extract(probe.bizKey(), method, args, pjp.getTarget());

        ProbeRecord record = new ProbeRecord(
                type,
                signature.toShortString(),
                Thread.currentThread().getName(),
                (System.nanoTime() - startNanos) / 1_000_000L,
                Instant.now(),
                error == null ? null : error.getClass().getName(),
                error == null ? null : ProbeText.truncate(error.getMessage(), 512),
                bizKey,
                argsBytes,
                resultBytes,
                params);
        queue.offer(record);
    }

    private long estimateSize(Object value) {
        if (value == null) {
            return 0L;
        }
        try {
            if (value instanceof CharSequence text) {
                return text.length() * 2L;
            }
            if (value instanceof byte[] bytes) {
                return bytes.length;
            }
            return Math.min(1_048_576L, String.valueOf(value).length() * 2L);
        } catch (Throwable ignored) {
            return 0L;
        }
    }
}
