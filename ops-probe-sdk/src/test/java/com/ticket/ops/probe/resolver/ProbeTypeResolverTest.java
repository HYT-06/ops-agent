package com.ticket.ops.probe.resolver;

import com.ticket.ops.probe.model.ProbeType;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProbeTypeResolverTest {

    private final ProbeTypeResolver resolver = new ProbeTypeResolver();

    @Test
    void detectsSqlContextByClassName() throws Exception {
        Method method = OrderMapper.class.getMethod("findById", Long.class);
        assertEquals(ProbeType.SQL_CONTEXT, resolver.resolve(method, OrderMapper.class));
    }

    @Test
    void detectsMemoryPressureByMethodName() throws Exception {
        Method method = ReportService.class.getMethod("reportAll");
        assertEquals(ProbeType.MEMORY_PRESSURE, resolver.resolve(method, ReportService.class));
    }

    @Test
    void detectsMemoryPressureByReturnType() throws Exception {
        Method method = BatchService.class.getMethod("load");
        assertEquals(ProbeType.MEMORY_PRESSURE, resolver.resolve(method, BatchService.class));
    }

    @Test
    void detectsMqContextByAnnotation() throws Exception {
        Method method = MqConsumer.class.getMethod("onMessage", String.class);
        assertEquals(ProbeType.MQ_CONTEXT, resolver.resolve(method, MqConsumer.class));
    }

    @Test
    void fallsBackToGeneral() throws Exception {
        Method method = PlainService.class.getMethod("ping");
        assertEquals(ProbeType.GENERAL, resolver.resolve(method, PlainService.class));
    }

    static class OrderMapper {
        public Object findById(Long id) {
            return null;
        }
    }

    static class ReportService {
        public void reportAll() {
        }
    }

    static class BatchService {
        public List<String> load() {
            return List.of();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface RabbitListener {
    }

    @RabbitListener
    static class MqConsumer {
        public void onMessage(String message) {
        }
    }

    static class PlainService {
        public String ping() {
            return "pong";
        }
    }
}
