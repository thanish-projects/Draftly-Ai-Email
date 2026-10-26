package com.capgemini.emaildraftingagent.dto;

import com.capgemini.emaildraftingagent.model.TaskPriority;
import java.time.LocalDate;

public record ExtractedTask(String title, String owner, LocalDate dueDate, TaskPriority priority) {
}
