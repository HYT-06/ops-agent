package com.ticket.ops.probe.sampler;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.IntSupplier;

/**
 * 采样器：100 表示全量，1~99 表示百分比。
 */
public class ProbeSampler {

    private final IntSupplier randomHundred;

    public ProbeSampler() {
        this(() -> ThreadLocalRandom.current().nextInt(100));
    }

    public ProbeSampler(IntSupplier randomHundred) {
        this.randomHundred = randomHundred;
    }

    public boolean hit(int sampleRate) {
        if (sampleRate <= 0) {
            return false;
        }
        if (sampleRate >= 100) {
            return true;
        }
        return randomHundred.getAsInt() < sampleRate;
    }
}
