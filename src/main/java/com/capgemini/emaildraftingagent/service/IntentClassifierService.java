package com.capgemini.emaildraftingagent.service;

import com.capgemini.emaildraftingagent.dto.IntentClassificationRequest;
import com.capgemini.emaildraftingagent.dto.IntentClassificationResponse;
import org.springframework.stereotype.Service;

@Service
public class IntentClassifierService {

    private final OpenAIService aiClient;

    public IntentClassifierService(OpenAIService aiClient) {
        this.aiClient = aiClient;
    }

    public IntentClassificationResponse classify(IntentClassificationRequest request) {
        IntentClassificationResult result = aiClient.classifyIntent(request.emailContent().strip());
        if (result.intent() == null || result.confidence() == null
                || result.confidence() < 0 || result.confidence() > 100) {
            throw new AiAnalysisException(
                    "The AI provider returned an invalid intent classification; confidence must be between 0 and 100.");
        }
        return new IntentClassificationResponse(result.intent(), result.confidence());
    }
}
