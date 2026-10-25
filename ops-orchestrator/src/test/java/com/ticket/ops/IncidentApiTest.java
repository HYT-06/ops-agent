package com.ticket.ops;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class IncidentApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createAndAdvanceIncident() throws Exception {
        MvcResult created = mockMvc.perform(post("/ops/incidents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "scene": "FULLGC",
                                  "resourceId": "demo",
                                  "summary": "GC 频率超阈值",
                                  "metrics": {"gcCount5m": 10}
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.state").value("RECEIVED"))
                .andExpect(jsonPath("$.request.scene").value("FULLGC"))
                .andReturn();

        String requestId = requestId(created);

        mockMvc.perform(post("/ops/incidents/{requestId}/analyze", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("ARCHIVED"))
                .andExpect(jsonPath("$.report").exists())
                .andExpect(jsonPath("$.gateResult.decision").value("NO_ACTION"));

        mockMvc.perform(get("/ops/incidents/{requestId}", requestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.request.requestId").value(requestId))
                .andExpect(jsonPath("$.state").value("ARCHIVED"));
    }

    @Test
    void unknownIncidentReturns404() throws Exception {
        mockMvc.perform(get("/ops/incidents/not-exists"))
                .andExpect(status().isNotFound());
    }

    private String requestId(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("request").path("requestId").asText();
    }
}
