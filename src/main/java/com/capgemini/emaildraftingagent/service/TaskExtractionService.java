package com.capgemini.emaildraftingagent.service;

import com.capgemini.emaildraftingagent.dto.ExtractedTask;
import com.capgemini.emaildraftingagent.dto.TaskExtractionRequest;
import com.capgemini.emaildraftingagent.dto.TaskExtractionResponse;
import com.capgemini.emaildraftingagent.model.EmailHistory;
import com.capgemini.emaildraftingagent.model.EmailTaskEntity;
import com.capgemini.emaildraftingagent.model.TaskPriority;
import com.capgemini.emaildraftingagent.repository.EmailTaskRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskExtractionService {

    private static final Pattern HIGH_PRIORITY_TERMS =
            Pattern.compile("\\b(urgent|immediately)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern HIGH_PRIORITY_DATES =
            Pattern.compile("\\b(today|tomorrow)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern THIS_WEEK =
            Pattern.compile("\\bthis\\s+week\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    private final OpenAIService aiClient;
    private final EmailTaskRepository taskRepository;
    private final Clock clock;

    public TaskExtractionService(OpenAIService aiClient, EmailTaskRepository taskRepository, Clock clock) {
        this.aiClient = aiClient;
        this.taskRepository = taskRepository;
        this.clock = clock;
    }

    @Transactional
    public TaskExtractionResponse extractAndStore(TaskExtractionRequest request) {
        List<ExtractedTask> tasks = extract(request.emailContent());
        store(tasks, request.emailContent(), null);
        return new TaskExtractionResponse(tasks);
    }

    public List<ExtractedTask> extract(String emailContent) {
        if (emailContent == null || emailContent.isBlank()) {
            throw new AiAnalysisException("Email content is required to extract tasks.");
        }
        TaskExtractionAiResult result = aiClient.extractTasks(emailContent.strip(), LocalDate.now(clock));
        if (result.tasks() == null) {
            throw new AiAnalysisException("The AI provider returned an invalid task extraction result.");
        }
        return result.tasks().stream().map(task -> toExtractedTask(task, emailContent)).toList();
    }

    @Transactional
    public void storeForHistory(List<ExtractedTask> tasks, String sourceEmail, EmailHistory history) {
        store(tasks, sourceEmail, history);
    }

    private void store(List<ExtractedTask> tasks, String sourceEmail, EmailHistory history) {
        List<EmailTaskEntity> entities = tasks.stream()
                .map(task -> new EmailTaskEntity(task.title(), task.owner(), task.dueDate(), task.priority(),
                        sourceEmail, history))
                .toList();
        if (!entities.isEmpty()) {
            taskRepository.saveAll(entities);
        }
    }

    private ExtractedTask toExtractedTask(TaskExtractionAiResult.Task task, String emailContent) {
        if (task == null || task.title() == null || task.title().isBlank()) {
            throw new AiAnalysisException("The AI provider returned a task without a title.");
        }
        LocalDate dueDate = parseDueDate(task.dueDate());
        String owner = task.owner() == null || task.owner().isBlank() ? "Current User" : task.owner().strip();
        TaskPriority priority = inferPriority(task.title(), emailContent, dueDate);
        return new ExtractedTask(task.title().strip(), owner, dueDate, priority);
    }

    private static LocalDate parseDueDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.strip());
        } catch (DateTimeParseException exception) {
            throw new AiAnalysisException(
                    "The AI provider returned a due date that is not in ISO-8601 YYYY-MM-DD format.", exception);
        }
    }

    private TaskPriority inferPriority(String title, String emailContent, LocalDate dueDate) {
        String taskContext = title + " " + emailContent;
        LocalDate today = LocalDate.now(clock);
        if (HIGH_PRIORITY_TERMS.matcher(taskContext).find()
                || HIGH_PRIORITY_DATES.matcher(taskContext).find()
                || dueDate != null && !dueDate.isBefore(today) && !dueDate.isAfter(today.plusDays(1))) {
            return TaskPriority.HIGH;
        }
        if (THIS_WEEK.matcher(taskContext).find()
                || dueDate != null && !dueDate.isBefore(today)
                && ChronoUnit.DAYS.between(today, dueDate) <= 7) {
            return TaskPriority.MEDIUM;
        }
        return TaskPriority.LOW;
    }
}
