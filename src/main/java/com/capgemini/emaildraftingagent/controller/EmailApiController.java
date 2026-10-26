package com.capgemini.emaildraftingagent.controller;

import com.capgemini.emaildraftingagent.dto.ProcessEmailApiRequest;
import com.capgemini.emaildraftingagent.dto.ProcessEmailApiResponse;
import com.capgemini.emaildraftingagent.dto.StoredTaskResponse;
import com.capgemini.emaildraftingagent.service.EmailOrchestratorService;
import com.capgemini.emaildraftingagent.service.TaskQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Email and tasks", description = "Process emails and retrieve saved emails and extracted tasks")
public class EmailApiController {

    private final EmailOrchestratorService emailOrchestratorService;
    private final TaskQueryService taskQueryService;

    public EmailApiController(EmailOrchestratorService emailOrchestratorService, TaskQueryService taskQueryService) {
        this.emailOrchestratorService = emailOrchestratorService;
        this.taskQueryService = taskQueryService;
    }

    @PostMapping("/email/process")
    @Operation(summary = "Process an email", description = "Analyze, classify, extract, schedule, draft, supervise and persist an email.")
    public ProcessEmailApiResponse process(@Valid @RequestBody ProcessEmailApiRequest request) {
        return ProcessEmailApiResponse.from(emailOrchestratorService.process(request.toProcessEmailRequest()));
    }

    @GetMapping("/tasks")
    @Operation(summary = "List extracted tasks")
    public List<StoredTaskResponse> getTasks() {
        return taskQueryService.getTasks();
    }
}
