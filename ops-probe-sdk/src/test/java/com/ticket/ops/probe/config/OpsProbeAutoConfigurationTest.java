package com.ticket.ops.probe.config;

import com.ticket.ops.probe.annotation.OpsProbe;
import com.ticket.ops.probe.model.ProbeRecord;
import com.ticket.ops.probe.model.ProbeType;
import com.ticket.ops.probe.sink.ProbeRecordSink;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证「引入依赖 + 自动装配 + 方法注解」即可生效，无需业务代码显式配置。
 */
class OpsProbeAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpsProbeAutoConfiguration.class))
            .withUserConfiguration(TestConfig.class);

    @Test
    void appliesAspectAutomaticallyAndMasksArguments() {
        runner.withPropertyValues("ops.probe.upload-interval-ms=50")
                .run(context -> {
                    ProbeTarget target = context.getBean(ProbeTarget.class);
                    CapturingSink sink = context.getBean(CapturingSink.class);

                    target.query("13812345678");

                    assertTrue(sink.latch.await(2, TimeUnit.SECONDS), "探针记录未在预期时间内上报");
                    ProbeRecord record = sink.records.get(0);
                    assertEquals(ProbeType.SQL_CONTEXT, record.type());
                    assertEquals("13812345678", record.bizKey());
                    assertTrue(record.params().contains("1**********"));
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class TestConfig {

        @Bean
        ProbeTarget probeTarget() {
            return new ProbeTarget();
        }

        @Bean
        CapturingSink capturingSink() {
            return new CapturingSink();
        }
    }

    public static class ProbeTarget {

        @OpsProbe(value = ProbeType.SQL_CONTEXT, bizKey = "#p0")
        public String query(String phone) {
            return "ok";
        }
    }

    static class CapturingSink implements ProbeRecordSink {

        final List<ProbeRecord> records = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);

        @Override
        public void upload(List<ProbeRecord> batch) {
            records.addAll(batch);
            latch.countDown();
        }
    }
}
