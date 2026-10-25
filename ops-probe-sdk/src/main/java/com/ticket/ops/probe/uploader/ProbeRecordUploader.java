package com.ticket.ops.probe.uploader;

import com.ticket.ops.probe.model.ProbeRecord;
import com.ticket.ops.probe.queue.ProbeRecordQueue;
import com.ticket.ops.probe.sink.ProbeRecordSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 单线程异步上报器。所有异常都被吞掉，绝不回传到业务线程。
 */
public class ProbeRecordUploader {

    private static final Logger log = LoggerFactory.getLogger(ProbeRecordUploader.class);

    private final ProbeRecordQueue queue;
    private final ProbeRecordSink sink;
    private final int batchSize;
    private final long intervalMs;
    private volatile boolean running;
    private Thread worker;

    public ProbeRecordUploader(ProbeRecordQueue queue, ProbeRecordSink sink,
                               int batchSize, long intervalMs) {
        this.queue = queue;
        this.sink = sink;
        this.batchSize = Math.max(1, batchSize);
        this.intervalMs = Math.max(1L, intervalMs);
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        worker = new Thread(this::loop, "ops-probe-uploader");
        worker.setDaemon(true);
        worker.start();
    }

    public synchronized void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            worker = null;
        }
    }

    private void loop() {
        List<ProbeRecord> batch = new ArrayList<>(batchSize);
        while (running) {
            try {
                ProbeRecord first = queue.poll(intervalMs);
                if (first == null) {
                    continue;
                }
                batch.add(first);
                queue.drainTo(batch, batchSize - 1);
                try {
                    sink.upload(List.copyOf(batch));
                } catch (Throwable uploadError) {
                    log.warn("ops-probe 上报失败，已丢弃本批次 {} 条", batch.size(), uploadError);
                }
                batch.clear();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Throwable unexpected) {
                log.warn("ops-probe 上报循环异常，继续运行", unexpected);
            }
        }
    }
}
