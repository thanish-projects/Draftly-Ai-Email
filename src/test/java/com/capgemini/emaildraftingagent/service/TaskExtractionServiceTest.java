package com.capgemini.emaildraftingagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.capgemini.emaildraftingagent.dto.ExtractedTask;
import com.capgemini.emaildraftingagent.dto.TaskExtractionRequest;
import com.capgemini.emaildraftingagent.dto.TaskExtractionResponse;
import com.capgemini.emaildraftingagent.model.EmailTaskEntity;
import com.capgemini.emaildraftingagent.model.TaskPriority;
import com.capgemini.emaildraftingagent.repository.EmailTaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class TaskExtractionServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 14);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-14T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private EmailTaskRepository taskRepository;

    @Test
    void extractsTaskConvertsDateInfersPriorityAndPersistsIt() {
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(new TaskExtractionAiResult(List.of(
                new TaskExtractionAiResult.Task("Finalize proposal", null, "2026-10-16"))));
        TaskExtractionService service = new TaskExtractionService(aiClient, taskRepository, CLOCK);

        TaskExtractionResponse response = service.extractAndStore(
                new TaskExtractionRequest("Finalize the proposal this week."));

        assertEquals(List.of(new ExtractedTask("Finalize proposal", "Current User",
                LocalDate.of(2026, 10, 16), TaskPriority.MEDIUM)), response.tasks());
        assertEquals(TODAY, aiClient.currentDate);
        verify(taskRepository).saveAll(anyList());
    }

    @Test
    void infersHighPriorityForUrgentTask() {
        assertEquals(TaskPriority.HIGH, extractOne(
                "Urgent: finalize the proposal next month.", "2026-11-14").priority());
    }

    @Test
    void infersHighPriorityForTomorrowDeadline() {
        assertEquals(TaskPriority.HIGH, extractOne(
                "Finalize the proposal by tomorrow.", "2026-10-15").priority());
    }

    @Test
    void infersMediumPriorityForThisWeek() {
        assertEquals(TaskPriority.MEDIUM, extractOne(
                "Finalize the proposal this week.", null).priority());
    }

    @Test
    void defaultsToLowPriority() {
        assertEquals(TaskPriority.LOW, extractOne(
                "Please finalize the proposal.", null).priority());
    }

    @Test
    void rejectsMalformedAiDueDate() {
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(new TaskExtractionAiResult(List.of(
                new TaskExtractionAiResult.Task("Finalize proposal", "Sam", "next Friday"))));

        assertThrows(AiAnalysisException.class,
                () -> new TaskExtractionService(aiClient, taskRepository, CLOCK)
                        .extract("Finalize the proposal next Friday."));
    }

    private ExtractedTask extractOne(String email, String dueDate) {
        StubOpenAiCompatibleClient aiClient = new StubOpenAiCompatibleClient(new TaskExtractionAiResult(List.of(
                new TaskExtractionAiResult.Task("Finalize proposal", "Sam", dueDate))));
        return new TaskExtractionService(aiClient, taskRepository, CLOCK).extract(email).getFirst();
    }

    private static final class StubOpenAiCompatibleClient extends OpenAIService {

        private final TaskExtractionAiResult result;
        private LocalDate currentDate;

        private StubOpenAiCompatibleClient(TaskExtractionAiResult result) {
            super(RestClient.builder(), new ObjectMapper(), "http://localhost", "test-key", "test-model");
            this.result = result;
        }

        @Override
        public TaskExtractionAiResult extractTasks(String emailContent, LocalDate currentDate) {
            this.currentDate = currentDate;
            return result;
        }
    }
}
