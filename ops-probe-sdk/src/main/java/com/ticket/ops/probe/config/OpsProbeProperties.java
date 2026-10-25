package com.ticket.ops.probe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 探针 SDK 配置。被监控应用可通过 ops.probe.* 覆盖。
 */
@ConfigurationProperties(prefix = "ops.probe")
public class OpsProbeProperties {

    private boolean enabled = true;
    private int queueCapacity = 10000;
    private int batchSize = 500;
    private long uploadIntervalMs = 2000L;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public long getUploadIntervalMs() {
        return uploadIntervalMs;
    }

    public void setUploadIntervalMs(long uploadIntervalMs) {
        this.uploadIntervalMs = uploadIntervalMs;
    }
}
