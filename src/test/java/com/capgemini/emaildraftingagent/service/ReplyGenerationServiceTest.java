package com.capgemini.emaildraftingagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.capgemini.emaildraftingagent.dto.ExtractedTask;
import com.capgemini.emaildraftingagent.dto.ReplyGenerationRequest;
import com.capgemini.emaildraftingagent.dto.ReplyGenerationResponse;
import com.capgemini.emaildraftingagent.model.EmailIntent;
import com.capgemini.emaildraftingagent.model.TaskPriority;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class ReplyGenerationServiceTest {

    @Test
    void generatesConciseReplyFromIntentTasksAndMeetingSuggestions() {
        List<ExtractedTask> tasks = List.of(new ExtractedTask(
                "Finalize budget proposal", "Current User", LocalDate.of(2026, 10, 16), TaskPriority.HIGH));
        List<String> meetingSuggestions = List.of("Tomorrow 2:00 PM", "Tomorrow 3:00 PM", "Tomorrow 4:00 PM");
        String expectedReply = """
                Hello,

                Thank you for your email.

                I have noted the following action item:
                - Finalize budget proposal by 2026-10-16.

                For the meeting, I am available at:
                - Tomorrow 2:00 PM
                - Tomorrow 3:00 PM
                - Tomorrow 4:00 PM

                Please let me know your preferred slot.

                Regards,
                AI Assistant""";
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(
                new ReplyDraftAiResult("  " + expectedReply + "  "));

        ReplyGenerationResponse response = new ReplyGenerationService(aiClient)
                .generate(new ReplyGenerationRequest(EmailIntent.BOTH, tasks, meetingSuggestions));

        assertEquals(expectedReply, response.reply());
        assertEquals(EmailIntent.BOTH, aiClient.intent);
        assertEquals(tasks, aiClient.tasks);
        assertEquals(meetingSuggestions, aiClient.meetingSuggestions);
    }

    @Test
    void allowsEmptyTaskAndMeetingListsForGeneralIntent() {
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(
                new ReplyDraftAiResult("Hello,\n\nThank you for your email.\n\nRegards,\nAI Assistant"));

        ReplyGenerationResponse response = new ReplyGenerationService(aiClient)
                .generate(new ReplyGenerationRequest(EmailIntent.GENERAL, null, null));

        assertEquals(EmailIntent.GENERAL, aiClient.intent);
        assertEquals(List.of(), aiClient.tasks);
        assertEquals(List.of(), aiClient.meetingSuggestions);
        assertEquals("Hello,\n\nThank you for your email.\n\nRegards,\nAI Assistant", response.reply());
    }

    @Test
    void rejectsEmptyAiDraft() {
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(new ReplyDraftAiResult(" "));

        assertThrows(AiAnalysisException.class, () -> new ReplyGenerationService(aiClient)
                .generate(new ReplyGenerationRequest(EmailIntent.TASK, List.of(), List.of())));
    }

    private static final class StubOpenAiCompatibleClient extends OpenAIService {

        private final ReplyDraftAiResult result;
        private EmailIntent intent;
        private List<ExtractedTask> tasks;
        private List<String> meetingSuggestions;

        private StubOpenAiCompatibleClient(ReplyDraftAiResult result) {
            super(RestClient.builder(), new ObjectMapper(), "http://localhost", "test-key", "test-model");
            this.result = result;
        }

        @Override
        public ReplyDraftAiResult generateReply(
                EmailIntent intent, List<ExtractedTask> tasks, List<String> meetingSuggestions) {
            this.intent = intent;
            this.tasks = tasks;
            this.meetingSuggestions = meetingSuggestions;
            return result;
        }
    }
}
