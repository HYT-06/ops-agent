package com.ticket.ops.probe.config;

import com.ticket.ops.probe.aspect.OpsProbeAspect;
import com.ticket.ops.probe.desensitize.ParamDesensitizer;
import com.ticket.ops.probe.queue.ProbeRecordQueue;
import com.ticket.ops.probe.resolver.ProbeTypeResolver;
import com.ticket.ops.probe.sampler.ProbeSampler;
import com.ticket.ops.probe.sink.LoggingProbeRecordSink;
import com.ticket.ops.probe.sink.ProbeRecordSink;
import com.ticket.ops.probe.spel.SpelBizKeyExtractor;
import com.ticket.ops.probe.uploader.ProbeRecordUploader;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 探针 SDK 自动装配。
 * <p>
 * 被监控应用（如 12306 demo）只需把 ops-probe-sdk 加入依赖，无需任何显式配置。
 * 可通过 ops.probe.enabled=false 整体关闭。
 */
@AutoConfiguration
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
@ConditionalOnProperty(prefix = "ops.probe", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(OpsProbeProperties.class)
public class OpsProbeAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ProbeTypeResolver probeTypeResolver() {
        return new ProbeTypeResolver();
    }

    @Bean
    @ConditionalOnMissingBean
    public ParamDesensitizer paramDesensitizer() {
        return new ParamDesensitizer();
    }

    @Bean
    @ConditionalOnMissingBean
    public SpelBizKeyExtractor spelBizKeyExtractor() {
        return new SpelBizKeyExtractor();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProbeSampler probeSampler() {
        return new ProbeSampler();
    }

    @Bean
    @ConditionalOnMissingBean
    public ProbeRecordQueue probeRecordQueue(OpsProbeProperties properties) {
        return new ProbeRecordQueue(properties.getQueueCapacity());
    }

    @Bean
    @ConditionalOnMissingBean
    public ProbeRecordSink probeRecordSink() {
        return new LoggingProbeRecordSink();
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnMissingBean
    public ProbeRecordUploader probeRecordUploader(ProbeRecordQueue queue,
                                                   ProbeRecordSink sink,
                                                   OpsProbeProperties properties) {
        return new ProbeRecordUploader(queue, sink,
                properties.getBatchSize(), properties.getUploadIntervalMs());
    }

    @Bean
    @ConditionalOnMissingBean
    public OpsProbeAspect opsProbeAspect(ProbeTypeResolver typeResolver,
                                         ProbeSampler sampler,
                                         ParamDesensitizer desensitizer,
                                         SpelBizKeyExtractor bizKeyExtractor,
                                         ProbeRecordQueue queue) {
        return new OpsProbeAspect(typeResolver, sampler, desensitizer, bizKeyExtractor, queue);
    }
}
