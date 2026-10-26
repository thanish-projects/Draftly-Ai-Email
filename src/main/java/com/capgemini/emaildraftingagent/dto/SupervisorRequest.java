package com.capgemini.emaildraftingagent.dto;

import com.capgemini.emaildraftingagent.model.EmailIntent;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SupervisorRequest(
        @NotBlank(message = "emailContent is required")
        @Size(max = 50000, message = "emailContent must be at most 50000 characters")
        String emailContent,
        @NotNull(message = "intent is required")
        EmailIntent intent,
        @NotNull(message = "tasks is required")
        List<@Valid ExtractedTask> tasks,
        @NotNull(message = "meetingSuggestions is required")
        List<String> meetingSuggestions,
        @NotBlank(message = "reply is required")
        @Size(max = 10000, message = "reply must be at most 10000 characters")
        String reply
) {
}
