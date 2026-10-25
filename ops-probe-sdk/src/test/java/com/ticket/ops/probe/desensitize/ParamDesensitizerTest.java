package com.ticket.ops.probe.desensitize;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParamDesensitizerTest {

    private final ParamDesensitizer desensitizer = new ParamDesensitizer();

    @Test
    void masksMobileNumber() {
        assertEquals("1**********", desensitizer.maskString("13812345678"));
    }

    @Test
    void masksEmail() {
        assertEquals("***@***", desensitizer.maskString("alice@example.com"));
    }

    @Test
    void masksIdCard() {
        assertEquals("******************", desensitizer.maskString("110101199001011234"));
    }

    @Test
    void masksBankCard() {
        assertEquals("**** **** **** ****", desensitizer.maskString("6222021234567890"));
    }

    @Test
    void masksArgsAndKeepsNonSensitiveValues() {
        String masked = desensitizer.mask(new Object[]{"13812345678", 42, true});
        assertTrue(masked.contains("1**********"));
        assertTrue(masked.contains("42"));
        assertTrue(masked.contains("true"));
        assertFalse(masked.contains("13812345678"));
    }
}
