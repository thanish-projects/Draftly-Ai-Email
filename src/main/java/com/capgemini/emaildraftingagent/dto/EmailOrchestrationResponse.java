package com.capgemini.emaildraftingagent.dto;

import com.capgemini.emaildraftingagent.model.EmailIntent;
import com.capgemini.emaildraftingagent.model.SupervisionStatus;
import java.util.List;

public record EmailOrchestrationResponse(
        Long emailId,
        EmailIntent intent,
        List<ExtractedTask> tasks,
        List<String> meetingSuggestions,
        String reply,
        SupervisionStatus status
) {
}
