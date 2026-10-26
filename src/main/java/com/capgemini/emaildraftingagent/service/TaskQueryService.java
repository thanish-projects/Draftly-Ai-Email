package com.capgemini.emaildraftingagent.service;

import com.capgemini.emaildraftingagent.dto.StoredTaskResponse;
import com.capgemini.emaildraftingagent.model.EmailTaskEntity;
import com.capgemini.emaildraftingagent.repository.EmailTaskRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskQueryService {

    private final EmailTaskRepository repository;

    public TaskQueryService(EmailTaskRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<StoredTaskResponse> getTasks() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(TaskQueryService::toResponse).toList();
    }

    private static StoredTaskResponse toResponse(EmailTaskEntity task) {
        return new StoredTaskResponse(task.getId(),
                task.getEmailHistory() == null ? null : task.getEmailHistory().getId(),
                task.getTitle(), task.getOwner(), task.getDueDate(), task.getPriority(), task.getCreatedAt());
    }
}
