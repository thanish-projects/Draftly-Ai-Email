package com.capgemini.emaildraftingagent.dto;

import com.capgemini.emaildraftingagent.model.EmailIntent;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ReplyGenerationRequest(
        @NotNull(message = "intent is required")
        EmailIntent intent,
        List<ExtractedTask> tasks,
        List<String> meetingSuggestions
) {
}
