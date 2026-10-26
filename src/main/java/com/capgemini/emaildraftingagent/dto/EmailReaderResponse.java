package com.capgemini.emaildraftingagent.dto;

import com.capgemini.emaildraftingagent.model.EmailIntent;

public record EmailReaderResponse(EmailIntent intent, String summary) {
}
