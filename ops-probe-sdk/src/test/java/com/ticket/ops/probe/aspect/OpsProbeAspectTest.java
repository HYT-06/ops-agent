package com.ticket.ops.probe.aspect;

import com.ticket.ops.probe.annotation.OpsProbe;
import com.ticket.ops.probe.desensitize.ParamDesensitizer;
import com.ticket.ops.probe.model.ProbeRecord;
import com.ticket.ops.probe.model.ProbeType;
import com.ticket.ops.probe.queue.ProbeRecordQueue;
import com.ticket.ops.probe.resolver.ProbeTypeResolver;
import com.ticket.ops.probe.sampler.ProbeSampler;
import com.ticket.ops.probe.spel.SpelBizKeyExtractor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpsProbeAspectTest {

    @Test
    void recordsInvocationWithoutChangingBusinessResult() throws Throwable {
        ProbeRecordQueue queue = new ProbeRecordQueue(10);
        OpsProbeAspect aspect = newAspect(queue);
        Target target = new Target();
        Method method = Target.class.getMethod("query", String.class);
        OpsProbe probe = method.getAnnotation(OpsProbe.class);

        ProceedingJoinPoint pjp = mockJoinPoint(method, target, new Object[]{"13812345678"});
        when(pjp.proceed()).thenReturn("ok");

        Object result = aspect.probe(pjp, probe);

        assertEquals("ok", result);
        ProbeRecord record = drainSingle(queue);
        assertEquals(ProbeType.SQL_CONTEXT, record.type());
        assertEquals("13812345678", record.bizKey());
        assertNotNull(record.params());
        assertTrue(record.params().contains("1**********"));
    }

    @Test
    void recordsEvenWhenBusinessMethodThrows() throws Throwable {
        ProbeRecordQueue queue = new ProbeRecordQueue(10);
        OpsProbeAspect aspect = newAspect(queue);
        Target target = new Target();
        Method method = Target.class.getMethod("fail", String.class);
        OpsProbe probe = method.getAnnotation(OpsProbe.class);

        ProceedingJoinPoint pjp = mockJoinPoint(method, target, new Object[]{"13812345678"});
        when(pjp.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThrows(IllegalStateException.class, () -> aspect.probe(pjp, probe));
        ProbeRecord record = drainSingle(queue);
        assertEquals(IllegalStateException.class.getName(), record.errorClass());
    }

    private OpsProbeAspect newAspect(ProbeRecordQueue queue) {
        return new OpsProbeAspect(
                new ProbeTypeResolver(),
                new ProbeSampler(() -> 0),
                new ParamDesensitizer(),
                new SpelBizKeyExtractor(),
                queue);
    }

    private ProceedingJoinPoint mockJoinPoint(Method method, Object target, Object[] args) {
        ProceedingJoinPoint pjp = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(pjp.getSignature()).thenReturn(signature);
        when(signature.getMethod()).thenReturn(method);
        when(signature.toShortString()).thenReturn("Target." + method.getName() + "(..)");
        when(signature.getDeclaringType()).thenReturn(Target.class);
        when(pjp.getTarget()).thenReturn(target);
        when(pjp.getArgs()).thenReturn(args);
        return pjp;
    }

    private ProbeRecord drainSingle(ProbeRecordQueue queue) {
        List<ProbeRecord> records = new ArrayList<>();
        queue.drainTo(records, 10);
        assertEquals(1, records.size());
        return records.get(0);
    }

    static class Target {

        @OpsProbe(value = ProbeType.SQL_CONTEXT, bizKey = "#p0", desensitize = true)
        public String query(String phone) {
            return "ok";
        }

        @OpsProbe(value = ProbeType.GENERAL, bizKey = "#p0", desensitize = true)
        public String fail(String phone) {
            return "unused";
        }
    }
}
