package com.ticket.ops.eval.dataset;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EvalDatasetTest {

    @Test
    void defaultCasesAreUniqueAndComplete() {
        var cases = EvalDataset.defaultCases();
        assertEquals(3, cases.size());
        assertEquals(cases.size(), new HashSet<>(cases.stream().map(EvalCase::caseId).toList()).size());
        assertTrue(cases.stream().allMatch(evalCase ->
                evalCase.scene() != null && !evalCase.expectedRootCause().isBlank()));
    }
}
