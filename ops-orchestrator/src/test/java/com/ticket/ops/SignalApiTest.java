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
class SignalApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signalEvaluationCreatesIncidents() throws Exception {
        mockMvc.perform(post("/ops/signals/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceName": "signal-api-test",
                                  "gcCount5m": 10,
                                  "gcAvgPauseMs": 300,
                                  "slowSqlCountPerMin": 0,
                                  "slowSqlP99Ms": 100,
                                  "dlqDepth": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].state").value("RECEIVED"));
    }
}
