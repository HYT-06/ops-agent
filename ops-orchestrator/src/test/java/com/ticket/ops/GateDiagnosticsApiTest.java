package com.ticket.ops;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GateDiagnosticsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void outOfBoundParameterIsRejected() throws Exception {
        mockMvc.perform(post("/ops/diagnostics/gate-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scene": "FULLGC",
                                  "resourceId": "demo",
                                  "toolName": "exec.updateThreadPool",
                                  "params": {"coreSize": 999},
                                  "rollbackPoint": "coreSize=8",
                                  "idempotencyKey": "diag-bound-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("REJECTED"))
                .andExpect(jsonPath("$.reason").value(org.hamcrest.Matchers.containsString("参数越界")));
    }

    @Test
    void smallThreadPoolExpansionIsAutoApproved() throws Exception {
        mockMvc.perform(post("/ops/diagnostics/gate-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scene": "FULLGC",
                                  "resourceId": "demo",
                                  "toolName": "exec.updateThreadPool",
                                  "params": {"currentCoreSize": 10, "coreSize": 12},
                                  "rollbackPoint": "coreSize=10",
                                  "idempotencyKey": "diag-auto-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("AUTO_APPROVED"))
                .andExpect(jsonPath("$.riskLevel").value("LOW"));
    }

    @Test
    void dlqReplayRequiresApproval() throws Exception {
        mockMvc.perform(post("/ops/diagnostics/gate-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scene": "DLQ",
                                  "resourceId": "order-queue",
                                  "toolName": "dlq.replay",
                                  "params": {"messageId": "m1", "batchSize": 10},
                                  "rollbackPoint": "none",
                                  "idempotencyKey": "diag-approval-1"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("REQUIRES_APPROVAL"))
                .andExpect(jsonPath("$.riskLevel").value("HIGH"));
    }

    @Test
    void missingIdempotencyKeyReturns400() throws Exception {
        mockMvc.perform(post("/ops/diagnostics/gate-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scene": "FULLGC",
                                  "resourceId": "demo",
                                  "toolName": "exec.updateThreadPool",
                                  "params": {"coreSize": 12},
                                  "rollbackPoint": "coreSize=8"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }
}
