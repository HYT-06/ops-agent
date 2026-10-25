package com.ticket.ops.common.model;

/**
 * 确定性验证结果，验证层不得使用 LLM。
 */
public record VerificationResult(boolean passed, String detail) {

    public static VerificationResult pass(String detail) {
        return new VerificationResult(true, detail);
    }

    public static VerificationResult fail(String detail) {
        return new VerificationResult(false, detail);
    }
}
