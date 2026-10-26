package com.capgemini.emaildraftingagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.capgemini.emaildraftingagent.dto.IntentClassificationRequest;
import com.capgemini.emaildraftingagent.dto.IntentClassificationResponse;
import com.capgemini.emaildraftingagent.model.EmailIntent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class IntentClassifierServiceTest {

    @Test
    void returnsIntentAndConfidenceForEmail() {
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(
                new IntentClassificationResult(EmailIntent.BOTH, 95));

        IntentClassificationResponse result = new IntentClassifierService(aiClient).classify(
                new IntentClassificationRequest("  Let's connect tomorrow and share the report by Friday.  "));

        assertEquals(new IntentClassificationResponse(EmailIntent.BOTH, 95), result);
        assertEquals("Let's connect tomorrow and share the report by Friday.", aiClient.emailContent);
    }

    @Test
    void rejectsConfidenceOutsideSupportedRange() {
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(
                new IntentClassificationResult(EmailIntent.TASK, 101));

        assertThrows(AiAnalysisException.class, () -> new IntentClassifierService(aiClient)
                .classify(new IntentClassificationRequest("Share report by Friday.")));
    }

    @Test
    void rejectsMissingIntent() {
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(
                new IntentClassificationResult(null, 80));

        assertThrows(AiAnalysisException.class, () -> new IntentClassifierService(aiClient)
                .classify(new IntentClassificationRequest("Share report by Friday.")));
    }

    private static final class StubOpenAiCompatibleClient extends OpenAIService {

        private final IntentClassificationResult result;
        private String emailContent;

        private StubOpenAiCompatibleClient(IntentClassificationResult result) {
            super(RestClient.builder(), new ObjectMapper(), "http://localhost", "test-key", "test-model");
            this.result = result;
        }

        @Override
        public IntentClassificationResult classifyIntent(String emailContent) {
            this.emailContent = emailContent;
            return result;
        }
    }
}
