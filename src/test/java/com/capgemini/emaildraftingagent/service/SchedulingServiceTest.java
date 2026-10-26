package com.capgemini.emaildraftingagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.capgemini.emaildraftingagent.dto.SchedulingRequest;
import com.capgemini.emaildraftingagent.dto.SchedulingResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class SchedulingServiceTest {

    @Test
    void returnsTomorrowSlotsForTomorrowAfternoonRequest() {
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(
                List.of("tomorrow-2-pm", "tomorrow-3-pm", "tomorrow-4-pm"));

        SchedulingResponse response = new SchedulingService(aiClient)
                .suggest(new SchedulingRequest("Let's connect tomorrow afternoon."));

        assertEquals(List.of("Tomorrow 2:00 PM", "Tomorrow 3:00 PM", "Tomorrow 4:00 PM"),
                response.suggestions());
        assertEquals("Let's connect tomorrow afternoon.", aiClient.emailContent);
    }

    @Test
    void prioritizesRequestedSlotThenFillsTopThreeWithOtherAvailableSlots() {
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(
                List.of("next-week-tuesday-2-pm"));

        SchedulingResponse response = new SchedulingService(aiClient)
                .suggest(new SchedulingRequest("Let's meet next Tuesday at 2 PM."));

        assertEquals(List.of("Next Week Tuesday 2:00 PM", "Tomorrow 2:00 PM", "Tomorrow 3:00 PM"),
                response.suggestions());
    }

    @Test
    void usesDefaultAvailabilityOrderWhenNoTimePreferenceWasDetected() {
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(List.of());

        SchedulingResponse response = new SchedulingService(aiClient)
                .suggest(new SchedulingRequest("Could we arrange a meeting?"));

        assertEquals(List.of("Tomorrow 2:00 PM", "Tomorrow 3:00 PM", "Tomorrow 4:00 PM"),
                response.suggestions());
    }

    @Test
    void rejectsUnavailableSlotReturnedByAi() {
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(List.of("friday-9-am"));

        assertThrows(AiAnalysisException.class, () -> new SchedulingService(aiClient)
                .suggest(new SchedulingRequest("Let's meet Friday at 9 AM.")));
    }

    @Test
    void rejectsMissingAiPreferences() {
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(null);

        assertThrows(AiAnalysisException.class, () -> new SchedulingService(aiClient)
                .suggest(new SchedulingRequest("Let's meet tomorrow.")));
    }

    private static final class StubOpenAiCompatibleClient extends OpenAIService {

        private final List<String> preferredSlotIds;
        private String emailContent;

        private StubOpenAiCompatibleClient(List<String> preferredSlotIds) {
            super(RestClient.builder(), new ObjectMapper(), "http://localhost", "test-key", "test-model");
            this.preferredSlotIds = preferredSlotIds;
        }

        @Override
        public MeetingTimePreferences extractMeetingTimePreferences(String emailContent) {
            this.emailContent = emailContent;
            return preferredSlotIds == null ? null : new MeetingTimePreferences(preferredSlotIds);
        }
    }
}
