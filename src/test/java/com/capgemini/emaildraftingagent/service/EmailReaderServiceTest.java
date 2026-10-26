package com.capgemini.emaildraftingagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.capgemini.emaildraftingagent.dto.EmailReaderRequest;
import com.capgemini.emaildraftingagent.dto.EmailReaderResponse;
import com.capgemini.emaildraftingagent.model.EmailIntent;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class EmailReaderServiceTest {

    @Test
    void cleansEmailAndReturnsIntentAndSummary() {
        String cleanedEmail = "Let's connect tomorrow afternoon and finalize the budget proposal by Friday.";
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(new EmailReaderAiResult(
                EmailIntent.BOTH,
                "Schedule meeting tomorrow afternoon and finalize budget proposal by Friday",
                List.of("Let's connect tomorrow afternoon", "Finalize the budget proposal by Friday.")));

        EmailReaderResponse response = new EmailReaderService(aiClient).read(new EmailReaderRequest(
                "Project Update",
                "  Let's   connect tomorrow afternoon and finalize the budget proposal by Friday.  \r\n"
                        + "\r\nBest regards,\r\nAlex Smith\r\nalex@example.com  "));

        assertEquals(EmailIntent.BOTH, response.intent());
        assertEquals("Schedule meeting tomorrow afternoon and finalize budget proposal by Friday",
                response.summary());
        assertEquals("Project Update", aiClient.subject);
        assertEquals(cleanedEmail, aiClient.email);
    }

    @Test
    void removesSignatureSeparatorAndRedundantBlankLines() {
        assertEquals("Please review the attached plan.\n\nThe response is needed by Friday.",
                EmailReaderService.cleanEmail(" Please review the attached plan. \r\n\r\n\r\n"
                        + "The response is needed by Friday.\r\n-- \r\nSignature"));
    }

    @Test
    void keepsImportantSentencesAndCleanedEmailForTheProcessingFlow() {
        List<String> importantSentences = List.of("Let's meet tomorrow.", "Send the budget by Friday.");
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(new EmailReaderAiResult(
                EmailIntent.BOTH, "Meet tomorrow and send the budget by Friday.", importantSentences));

        EmailReaderContext context = new EmailReaderService(aiClient).analyzeForProcessing(
                new EmailReaderRequest("Update", "Let's meet tomorrow.\n\nSend the budget by Friday.\n\nRegards,\nAlex"));

        assertEquals(EmailIntent.BOTH, context.intent());
        assertEquals("Meet tomorrow and send the budget by Friday.", context.summary());
        assertEquals(importantSentences, context.importantSentences());
        assertEquals("Let's meet tomorrow.\n\nSend the budget by Friday.", context.cleanedEmail());
        assertEquals(context.cleanedEmail(), aiClient.email);
    }

    @Test
    void rejectsEmailThatContainsOnlyWhitespace() {
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(null);
        EmailReaderService service = new EmailReaderService(aiClient);

        AiAnalysisException exception = assertThrows(AiAnalysisException.class,
                () -> service.read(new EmailReaderRequest("subject", " \r\n\t ")));

        assertEquals("Email content is empty after removing whitespace and signatures.", exception.getMessage());
        assertNull(aiClient.email);
    }

    @Test
    void rejectsIncompleteAiResponse() {
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(
                new EmailReaderAiResult(EmailIntent.TASK, " ", List.of()));

        assertThrows(AiAnalysisException.class,
                () -> new EmailReaderService(aiClient).read(new EmailReaderRequest("subject", "A real email.")));
    }

    private static final class StubOpenAiCompatibleClient extends OpenAIService {

        private final EmailReaderAiResult result;
        private String subject;
        private String email;

        private StubOpenAiCompatibleClient(EmailReaderAiResult result) {
            super(RestClient.builder(), new ObjectMapper(), "http://localhost", "test-key", "test-model");
            this.result = result;
        }

        @Override
        public EmailReaderAiResult readEmail(String subject, String cleanedEmail) {
            this.subject = subject;
            this.email = cleanedEmail;
            return result;
        }
    }
}
