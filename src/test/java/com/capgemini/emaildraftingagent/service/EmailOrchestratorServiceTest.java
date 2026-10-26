package com.capgemini.emaildraftingagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.capgemini.emaildraftingagent.dto.EmailReaderRequest;
import com.capgemini.emaildraftingagent.dto.ExtractedTask;
import com.capgemini.emaildraftingagent.dto.IntentClassificationRequest;
import com.capgemini.emaildraftingagent.dto.IntentClassificationResponse;
import com.capgemini.emaildraftingagent.dto.ProcessEmailRequest;
import com.capgemini.emaildraftingagent.dto.ReplyGenerationResponse;
import com.capgemini.emaildraftingagent.dto.SchedulingRequest;
import com.capgemini.emaildraftingagent.dto.SchedulingResponse;
import com.capgemini.emaildraftingagent.dto.SupervisorRequest;
import com.capgemini.emaildraftingagent.dto.SupervisorResponse;
import com.capgemini.emaildraftingagent.model.EmailHistory;
import com.capgemini.emaildraftingagent.model.EmailIntent;
import com.capgemini.emaildraftingagent.model.SupervisionStatus;
import com.capgemini.emaildraftingagent.model.TaskPriority;
import com.capgemini.emaildraftingagent.repository.EmailHistoryRepository;
import com.capgemini.emaildraftingagent.repository.EmailTaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class EmailOrchestratorServiceTest {

    private final OpenAIService aiClient = new StubAiClient();
    private final EmailHistoryRepository emailHistoryRepository = mock(EmailHistoryRepository.class);

    @Test
    void runsBothFlowInOrderSupervisesAndPersistsResults() {
        String emailContent = "Let's meet tomorrow and finalize the budget by Friday.";
        EmailReaderContext readerContext = new EmailReaderContext(
                EmailIntent.BOTH, "Meet and finalize the budget.", List.of(), emailContent);
        List<ExtractedTask> tasks = List.of(new ExtractedTask("Finalize budget", "Current User",
                LocalDate.of(2026, 10, 16), TaskPriority.HIGH));
        List<String> suggestions = List.of("Tomorrow 2:00 PM", "Tomorrow 3:00 PM", "Tomorrow 4:00 PM");
        String reply = "Hello,\n\nThank you for your email.\n\nRegards,\nAI Assistant";
        List<String> events = new ArrayList<>();

        EmailReaderService reader = new StubReaderService(aiClient, readerContext, events);
        IntentClassifierService classifier = new StubIntentClassifierService(aiClient,
                new IntentClassificationResponse(EmailIntent.BOTH, 95), events);
        TaskExtractionService extractor = new StubTaskExtractionService(aiClient, tasks, events);
        SchedulingService scheduler = new StubSchedulingService(aiClient, suggestions, events);
        ReplyGenerationService replyGenerator = new StubReplyGenerationService(aiClient, reply, events);
        SupervisorService supervisor = new StubSupervisorService(
                new SupervisorResponse(SupervisionStatus.APPROVED, List.of()), events);
        when(emailHistoryRepository.save(any(EmailHistory.class))).thenAnswer(invocation -> {
            events.add("persist");
            return invocation.getArgument(0);
        });

        EmailOrchestratorService service = new EmailOrchestratorService(reader, classifier, extractor, scheduler,
                replyGenerator, supervisor, emailHistoryRepository, new ObjectMapper());
        var response = service.process(new ProcessEmailRequest(emailContent, "Jordan", "Budget"));

        assertEquals(List.of("reader", "classifier", "tasks", "schedule", "reply", "supervisor",
                "persist", "store-tasks"), events);
        assertEquals(EmailIntent.BOTH, response.intent());
        assertEquals(tasks, response.tasks());
        assertEquals(suggestions, response.meetingSuggestions());
        assertEquals(reply, response.reply());
        assertEquals(SupervisionStatus.APPROVED, response.status());
    }

    @Test
    void skipsTaskAndMeetingStagesForGeneralIntent() {
        String emailContent = "Thanks for the update.";
        EmailReaderContext readerContext = new EmailReaderContext(
                EmailIntent.GENERAL, "A general update.", List.of(), emailContent);
        String reply = "Hello,\n\nThank you for your email.\n\nRegards,\nAI Assistant";
        List<String> events = new ArrayList<>();

        when(emailHistoryRepository.save(any(EmailHistory.class))).thenAnswer(invocation -> {
            events.add("persist");
            return invocation.getArgument(0);
        });
        EmailOrchestratorService service = new EmailOrchestratorService(
                new StubReaderService(aiClient, readerContext, events),
                new StubIntentClassifierService(aiClient,
                        new IntentClassificationResponse(EmailIntent.GENERAL, 90), events),
                new StubTaskExtractionService(aiClient, List.of(), events),
                new StubSchedulingService(aiClient, List.of(), events),
                new StubReplyGenerationService(aiClient, reply, events),
                new StubSupervisorService(new SupervisorResponse(SupervisionStatus.APPROVED, List.of()), events),
                emailHistoryRepository, new ObjectMapper());

        var response = service.process(new ProcessEmailRequest(emailContent, null, null));

        assertEquals(List.of("reader", "classifier", "reply", "supervisor", "persist", "store-tasks"), events);
        assertEquals(List.of(), response.tasks());
        assertEquals(List.of(), response.meetingSuggestions());
    }

    @Test
    void doesNotPersistWhenSupervisorRejectsOutput() {
        String emailContent = "Please review the update.";
        EmailReaderContext readerContext = new EmailReaderContext(
                EmailIntent.GENERAL, "A general update.", List.of(), emailContent);
        List<String> events = new ArrayList<>();
        EmailOrchestratorService service = new EmailOrchestratorService(
                new StubReaderService(aiClient, readerContext, events),
                new StubIntentClassifierService(aiClient,
                        new IntentClassificationResponse(EmailIntent.GENERAL, 90), events),
                new StubTaskExtractionService(aiClient, List.of(), events),
                new StubSchedulingService(aiClient, List.of(), events),
                new StubReplyGenerationService(aiClient,
                        "Hello,\n\nThank you for your email.\n\nRegards,\nAI Assistant", events),
                new StubSupervisorService(new SupervisorResponse(
                        SupervisionStatus.REJECTED, List.of("Reply contains an unsupported date")), events),
                emailHistoryRepository, new ObjectMapper());

        org.junit.jupiter.api.Assertions.assertThrows(SupervisorRejectedException.class,
                () -> service.process(new ProcessEmailRequest(emailContent, null, null)));

        assertEquals(List.of("reader", "classifier", "reply", "supervisor"), events);
        verify(emailHistoryRepository, never()).save(any(EmailHistory.class));
    }

    private static final class StubAiClient extends OpenAIService {

        private StubAiClient() {
            super(RestClient.builder(), new ObjectMapper(), "http://localhost", "test-key", "test-model");
        }
    }

    private static final class StubReaderService extends EmailReaderService {

        private final EmailReaderContext result;
        private final List<String> events;

        private StubReaderService(OpenAIService aiClient, EmailReaderContext result, List<String> events) {
            super(aiClient);
            this.result = result;
            this.events = events;
        }

        @Override
        public EmailReaderContext analyzeForProcessing(EmailReaderRequest request) {
            events.add("reader");
            return result;
        }
    }

    private static final class StubIntentClassifierService extends IntentClassifierService {

        private final IntentClassificationResponse result;
        private final List<String> events;

        private StubIntentClassifierService(OpenAIService aiClient,
                                            IntentClassificationResponse result, List<String> events) {
            super(aiClient);
            this.result = result;
            this.events = events;
        }

        @Override
        public IntentClassificationResponse classify(IntentClassificationRequest request) {
            events.add("classifier");
            return result;
        }
    }

    private static final class StubTaskExtractionService extends TaskExtractionService {

        private final List<ExtractedTask> result;
        private final List<String> events;

        private StubTaskExtractionService(OpenAIService aiClient,
                                          List<ExtractedTask> result, List<String> events) {
            super(aiClient, mock(EmailTaskRepository.class), Clock.systemUTC());
            this.result = result;
            this.events = events;
        }

        @Override
        public List<ExtractedTask> extract(String emailContent) {
            events.add("tasks");
            return result;
        }

        @Override
        public void storeForHistory(List<ExtractedTask> tasks, String sourceEmail, EmailHistory history) {
            events.add("store-tasks");
        }
    }

    private static final class StubSchedulingService extends SchedulingService {

        private final List<String> suggestions;
        private final List<String> events;

        private StubSchedulingService(OpenAIService aiClient, List<String> suggestions,
                                      List<String> events) {
            super(aiClient);
            this.suggestions = suggestions;
            this.events = events;
        }

        @Override
        public SchedulingResponse suggest(SchedulingRequest request) {
            events.add("schedule");
            return new SchedulingResponse(suggestions);
        }
    }

    private static final class StubReplyGenerationService extends ReplyGenerationService {

        private final String reply;
        private final List<String> events;

        private StubReplyGenerationService(OpenAIService aiClient, String reply, List<String> events) {
            super(aiClient);
            this.reply = reply;
            this.events = events;
        }

        @Override
        public ReplyGenerationResponse generate(
                EmailIntent intent, List<ExtractedTask> tasks, List<String> meetingSuggestions) {
            events.add("reply");
            return new ReplyGenerationResponse(reply);
        }
    }

    private static final class StubSupervisorService extends SupervisorService {

        private final SupervisorResponse result;
        private final List<String> events;

        private StubSupervisorService(SupervisorResponse result, List<String> events) {
            this.result = result;
            this.events = events;
        }

        @Override
        public SupervisorResponse supervise(SupervisorRequest request) {
            events.add("supervisor");
            return result;
        }
    }
}
