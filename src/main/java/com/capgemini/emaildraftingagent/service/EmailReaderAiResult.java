package com.capgemini.emaildraftingagent.service;

import com.capgemini.emaildraftingagent.model.EmailIntent;
import java.util.List;

public record EmailReaderAiResult(EmailIntent intent, String summary, List<String> importantSentences) {
}
