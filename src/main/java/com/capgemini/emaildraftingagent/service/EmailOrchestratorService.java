package com.capgemini.emaildraftingagent.service;

import com.capgemini.emaildraftingagent.dto.EmailOrchestrationResponse;
import com.capgemini.emaildraftingagent.dto.EmailReaderRequest;
import com.capgemini.emaildraftingagent.dto.EmailTask;
import com.capgemini.emaildraftingagent.dto.ExtractedTask;
import com.capgemini.emaildraftingagent.dto.IntentClassificationRequest;
import com.capgemini.emaildraftingagent.dto.IntentClassificationResponse;
import com.capgemini.emaildraftingagent.dto.MeetingSlot;
import com.capgemini.emaildraftingagent.dto.ProcessEmailRequest;
import com.capgemini.emaildraftingagent.dto.ReplyGenerationResponse;
import com.capgemini.emaildraftingagent.dto.SchedulingRequest;
import com.capgemini.emaildraftingagent.dto.SchedulingResponse;
import com.capgemini.emaildraftingagent.dto.SupervisorRequest;
import com.capgemini.emaildraftingagent.dto.SupervisorResponse;
import com.capgemini.emaildraftingagent.model.EmailHistory;
import com.capgemini.emaildraftingagent.model.EmailIntent;
import com.capgemini.emaildraftingagent.model.EmailPriority;
import com.capgemini.emaildraftingagent.model.SupervisionStatus;
import com.capgemini.emaildraftingagent.model.TaskPriority;
import com.capgemini.emaildraftingagent.repository.EmailHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailOrchestratorService {

    private final EmailReaderService emailReaderService;
    private final IntentClassifierService intentClassifierService;
    private final TaskExtractionService taskExtractionService;
    private final SchedulingService schedulingService;
    private final ReplyGenerationService replyGenerationService;
    private final SupervisorService supervisorService;
    private final EmailHistoryRepository emailHistoryRepository;
    private final ObjectMapper objectMapper;

    public EmailOrchestratorService(
            EmailReaderService emailReaderService,
            IntentClassifierService intentClassifierService,
            TaskExtractionService taskExtractionService,
            SchedulingService schedulingService,
            ReplyGenerationService replyGenerationService,
            SupervisorService supervisorService,
            EmailHistoryRepository emailHistoryRepository,
            ObjectMapper objectMapper) {
        this.emailReaderService = emailReaderService;
        this.intentClassifierService = intentClassifierService;
        this.taskExtractionService = taskExtractionService;
        this.schedulingService = schedulingService;
        this.replyGenerationService = replyGenerationService;
        this.supervisorService = supervisorService;
        this.emailHistoryRepository = emailHistoryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EmailOrchestrationResponse process(ProcessEmailRequest request) {
        var readerContext = emailReaderService.analyzeForProcessing(
                new EmailReaderRequest(request.subject(), request.emailContent()));

        IntentClassificationResponse classification = intentClassifierService.classify(
                new IntentClassificationRequest(readerContext.cleanedEmail()));
        EmailIntent intent = classification.intent();

        List<ExtractedTask> tasks = switch (intent) {
            case TASK, BOTH -> taskExtractionService.extract(readerContext.cleanedEmail());
            case MEETING, GENERAL -> List.of();
        };

        List<String> meetingSuggestions = switch (intent) {
            case MEETING, BOTH -> {
                SchedulingResponse scheduling = schedulingService.suggest(
                        new SchedulingRequest(readerContext.cleanedEmail()));
                yield scheduling.suggestions();
            }
            case TASK, GENERAL -> List.of();
        };

        ReplyGenerationResponse generatedReply = replyGenerationService.generate(intent, tasks, meetingSuggestions);
        String reply = generatedReply.reply();

        SupervisorResponse supervision = supervisorService.supervise(new SupervisorRequest(
                readerContext.cleanedEmail(), intent, tasks, meetingSuggestions, reply));
        if (supervision.status() != SupervisionStatus.APPROVED) {
            throw new SupervisorRejectedException(supervision);
        }

        EmailHistory history = new EmailHistory(
                request.emailContent(),
                request.sender(),
                request.subject(),
                intent,
                resolveEmailPriority(tasks),
                writeJson(tasks.stream()
                        .map(task -> new EmailTask(task.title(),
                                task.dueDate() == null ? null : task.dueDate().toString()))
                        .toList()),
                writeJson(meetingSuggestions.stream()
                        .map(slot -> new MeetingSlot(slot, null, null,
                                "Suggested based on available meeting times"))
                        .toList()),
                reply);
        EmailHistory savedHistory = emailHistoryRepository.save(history);
        taskExtractionService.storeForHistory(tasks, readerContext.cleanedEmail(), savedHistory);

        return new EmailOrchestrationResponse(savedHistory.getId(), intent, tasks, meetingSuggestions,
                reply, SupervisionStatus.APPROVED);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize orchestrated email results.", exception);
        }
    }

    private static EmailPriority resolveEmailPriority(List<ExtractedTask> tasks) {
        boolean hasHigh = tasks.stream().anyMatch(task -> task.priority() == TaskPriority.HIGH);
        if (hasHigh) {
            return EmailPriority.HIGH;
        }
        boolean hasMedium = tasks.stream().anyMatch(task -> task.priority() == TaskPriority.MEDIUM);
        if (hasMedium) {
            return EmailPriority.NORMAL;
        }
        boolean hasLow = tasks.stream().anyMatch(task -> task.priority() == TaskPriority.LOW);
        return hasLow ? EmailPriority.LOW : EmailPriority.NORMAL;
    }
}
