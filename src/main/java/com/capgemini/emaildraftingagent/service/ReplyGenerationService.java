package com.capgemini.emaildraftingagent.service;

import com.capgemini.emaildraftingagent.dto.ReplyGenerationRequest;
import com.capgemini.emaildraftingagent.dto.ReplyGenerationResponse;
import com.capgemini.emaildraftingagent.dto.ExtractedTask;
import com.capgemini.emaildraftingagent.model.EmailIntent;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ReplyGenerationService {

    private final OpenAIService aiClient;

    public ReplyGenerationService(OpenAIService aiClient) {
        this.aiClient = aiClient;
    }

    public ReplyGenerationResponse generate(ReplyGenerationRequest request) {
        if (request.intent() == null) {
            throw new IllegalArgumentException("Intent is required to generate an email reply.");
        }
        List<ExtractedTask> tasks = request.tasks() == null ? List.of() : List.copyOf(request.tasks());
        List<String> meetingSuggestions = request.meetingSuggestions() == null
                ? List.of() : request.meetingSuggestions().stream()
                .filter(suggestion -> suggestion != null && !suggestion.isBlank())
                .map(String::strip)
                .toList();

        ReplyDraftAiResult result = aiClient.generateReply(request.intent(), tasks, meetingSuggestions);
        if (result == null || result.reply() == null || result.reply().isBlank()) {
            throw new AiAnalysisException("The AI provider returned an empty reply draft.");
        }
        return new ReplyGenerationResponse(result.reply().strip());
    }

    public ReplyGenerationResponse generate(
            EmailIntent intent, List<ExtractedTask> tasks, List<String> meetingSuggestions) {
        return generate(new ReplyGenerationRequest(intent, tasks, meetingSuggestions));
    }
}
