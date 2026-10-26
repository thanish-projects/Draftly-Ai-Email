package com.capgemini.emaildraftingagent.dto;

import com.capgemini.emaildraftingagent.model.TaskPriority;
import java.time.Instant;
import java.time.LocalDate;

public record StoredTaskResponse(
        Long id,
        Long emailId,
        String title,
        String owner,
        LocalDate dueDate,
        TaskPriority priority,
        Instant createdAt
) {
}
