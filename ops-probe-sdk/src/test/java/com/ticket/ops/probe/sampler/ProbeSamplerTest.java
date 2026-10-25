package com.ticket.ops.probe.sampler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProbeSamplerTest {

    @Test
    void fullAndZeroRatesAreDeterministic() {
        ProbeSampler sampler = new ProbeSampler(() -> 0);
        assertFalse(sampler.hit(0));
        assertTrue(sampler.hit(100));
    }

    @Test
    void percentageRateUsesRandomValue() {
        assertTrue(new ProbeSampler(() -> 49).hit(50));
        assertFalse(new ProbeSampler(() -> 50).hit(50));
    }
}
