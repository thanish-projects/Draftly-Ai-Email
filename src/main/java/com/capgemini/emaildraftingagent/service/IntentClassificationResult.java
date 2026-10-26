package com.capgemini.emaildraftingagent.service;

import com.capgemini.emaildraftingagent.model.EmailIntent;

public record IntentClassificationResult(EmailIntent intent, Integer confidence) {
}
