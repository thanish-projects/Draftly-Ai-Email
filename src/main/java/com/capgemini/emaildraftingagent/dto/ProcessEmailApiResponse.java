package com.capgemini.emaildraftingagent.dto;

import com.capgemini.emaildraftingagent.model.EmailIntent;
import java.util.List;

public record ProcessEmailApiResponse(
        EmailIntent intent,
        List<ExtractedTask> tasks,
        List<String> meetingSuggestions,
        String reply
) {
    public static ProcessEmailApiResponse from(EmailOrchestrationResponse response) {
        return new ProcessEmailApiResponse(response.intent(), response.tasks(),
                response.meetingSuggestions(), response.reply());
    }
}
