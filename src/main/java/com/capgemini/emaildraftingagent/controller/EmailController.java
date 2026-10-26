package com.capgemini.emaildraftingagent.controller;

import com.capgemini.emaildraftingagent.dto.EmailProcessingResponse;
import com.capgemini.emaildraftingagent.dto.EmailOrchestrationResponse;
import com.capgemini.emaildraftingagent.dto.EmailReaderRequest;
import com.capgemini.emaildraftingagent.dto.EmailReaderResponse;
import com.capgemini.emaildraftingagent.dto.IntentClassificationRequest;
import com.capgemini.emaildraftingagent.dto.IntentClassificationResponse;
import com.capgemini.emaildraftingagent.dto.ProcessEmailRequest;
import com.capgemini.emaildraftingagent.dto.ReplyGenerationRequest;
import com.capgemini.emaildraftingagent.dto.ReplyGenerationResponse;
import com.capgemini.emaildraftingagent.dto.SchedulingRequest;
import com.capgemini.emaildraftingagent.dto.SchedulingResponse;
import com.capgemini.emaildraftingagent.dto.SupervisorRequest;
import com.capgemini.emaildraftingagent.dto.SupervisorResponse;
import com.capgemini.emaildraftingagent.dto.TaskExtractionRequest;
import com.capgemini.emaildraftingagent.dto.TaskExtractionResponse;
import com.capgemini.emaildraftingagent.service.IntentClassifierService;
import com.capgemini.emaildraftingagent.service.EmailReaderService;
import com.capgemini.emaildraftingagent.service.EmailProcessingService;
import com.capgemini.emaildraftingagent.service.TaskExtractionService;
import com.capgemini.emaildraftingagent.service.SchedulingService;
import com.capgemini.emaildraftingagent.service.SupervisorService;
import com.capgemini.emaildraftingagent.service.EmailOrchestratorService;
import com.capgemini.emaildraftingagent.service.ReplyGenerationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/emails")
public class EmailController {

    private final EmailProcessingService emailProcessingService;
    private final EmailReaderService emailReaderService;
    private final IntentClassifierService intentClassifierService;
    private final TaskExtractionService taskExtractionService;
    private final SchedulingService schedulingService;
    private final ReplyGenerationService replyGenerationService;
    private final SupervisorService supervisorService;
    private final EmailOrchestratorService emailOrchestratorService;

    public EmailController(EmailProcessingService emailProcessingService, EmailReaderService emailReaderService,
                           IntentClassifierService intentClassifierService,
                           TaskExtractionService taskExtractionService, SchedulingService schedulingService,
                           ReplyGenerationService replyGenerationService, SupervisorService supervisorService,
                           EmailOrchestratorService emailOrchestratorService) {
        this.emailProcessingService = emailProcessingService;
        this.emailReaderService = emailReaderService;
        this.intentClassifierService = intentClassifierService;
        this.taskExtractionService = taskExtractionService;
        this.schedulingService = schedulingService;
        this.replyGenerationService = replyGenerationService;
        this.supervisorService = supervisorService;
        this.emailOrchestratorService = emailOrchestratorService;
    }

    @PostMapping("/read")
    public EmailReaderResponse read(@Valid @RequestBody EmailReaderRequest request) {
        return emailReaderService.read(request);
    }

    @PostMapping("/classify")
    public IntentClassificationResponse classify(@Valid @RequestBody IntentClassificationRequest request) {
        return intentClassifierService.classify(request);
    }

    @PostMapping("/tasks/extract")
    public TaskExtractionResponse extractTasks(@Valid @RequestBody TaskExtractionRequest request) {
        return taskExtractionService.extractAndStore(request);
    }

    @PostMapping("/schedule")
    public SchedulingResponse suggestMeetingSlots(@Valid @RequestBody SchedulingRequest request) {
        return schedulingService.suggest(request);
    }

    @PostMapping("/reply/generate")
    public ReplyGenerationResponse generateReply(@Valid @RequestBody ReplyGenerationRequest request) {
        return replyGenerationService.generate(request);
    }

    @PostMapping("/supervise")
    public SupervisorResponse supervise(@Valid @RequestBody SupervisorRequest request) {
        return supervisorService.supervise(request);
    }

    @PostMapping("/process")
    public EmailOrchestrationResponse process(@Valid @RequestBody ProcessEmailRequest request) {
        return emailOrchestratorService.process(request);
    }

    @GetMapping("/{id}")
    public EmailProcessingResponse getById(@PathVariable long id) {
        return emailProcessingService.getById(id);
    }

    @GetMapping
    public Page<EmailProcessingResponse> getHistory(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return emailProcessingService.getHistory(pageable);
    }
}
