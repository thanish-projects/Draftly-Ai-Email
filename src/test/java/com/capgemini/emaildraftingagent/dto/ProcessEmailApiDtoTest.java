package com.capgemini.emaildraftingagent.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.capgemini.emaildraftingagent.model.EmailIntent;
import com.capgemini.emaildraftingagent.model.SupervisionStatus;
import com.capgemini.emaildraftingagent.model.TaskPriority;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessEmailApiDtoTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void mapsPublicEmailPropertyToOrchestratorRequest() {
        ProcessEmailApiRequest request = new ProcessEmailApiRequest(
                "Budget Planning",
                "Let's connect tomorrow afternoon and finalize budget proposal by Friday.");

        ProcessEmailRequest mapped = request.toProcessEmailRequest();

        assertEquals(request.email(), mapped.emailContent());
        assertEquals(request.subject(), mapped.subject());
    }

    @Test
    void processResponseExposesOnlyRequestedFields() throws Exception {
        ProcessEmailApiResponse response = ProcessEmailApiResponse.from(new EmailOrchestrationResponse(
                1L,
                EmailIntent.BOTH,
                List.of(new ExtractedTask("Finalize proposal", "Current User",
                        LocalDate.of(2026, 10, 16), TaskPriority.HIGH)),
                List.of("Tomorrow 2:00 PM"),
                "Hello,\n\nRegards,\nAI Assistant",
                SupervisionStatus.APPROVED));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertEquals(4, json.size());
        assertEquals("BOTH", json.get("intent").asText());
        assertEquals("Finalize proposal", json.get("tasks").get(0).get("title").asText());
        assertEquals("Tomorrow 2:00 PM", json.get("meetingSuggestions").get(0).asText());
        assertEquals("Hello,\n\nRegards,\nAI Assistant", json.get("reply").asText());
        assertFalse(json.has("emailId"));
        assertFalse(json.has("status"));
    }
}
