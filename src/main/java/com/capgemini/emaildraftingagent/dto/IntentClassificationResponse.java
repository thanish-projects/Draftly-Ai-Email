package com.capgemini.emaildraftingagent.dto;

import com.capgemini.emaildraftingagent.model.EmailIntent;

public record IntentClassificationResponse(EmailIntent intent, int confidence) {
}
