package com.ticket.ops.probe.queue;

import com.ticket.ops.probe.model.ProbeRecord;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 有界探针队列：offer 永不阻塞业务线程，队列满则丢弃。
 */
public class ProbeRecordQueue {

    private final BlockingQueue<ProbeRecord> queue;

    public ProbeRecordQueue(int capacity) {
        this.queue = new LinkedBlockingQueue<>(Math.max(1, capacity));
    }

    public boolean offer(ProbeRecord record) {
        return queue.offer(record);
    }

    public ProbeRecord poll(long timeoutMs) throws InterruptedException {
        return queue.poll(Math.max(1L, timeoutMs), TimeUnit.MILLISECONDS);
    }

    public int drainTo(List<ProbeRecord> target, int maxElements) {
        return queue.drainTo(target, Math.max(0, maxElements));
    }

    public int size() {
        return queue.size();
    }
}
