package com.capgemini.emaildraftingagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProcessEmailRequest(
        @NotBlank(message = "emailContent is required")
        @Size(max = 50000, message = "emailContent must be at most 50000 characters")
        String emailContent,
        @Size(max = 500, message = "sender must be at most 500 characters")
        String sender,
        @Size(max = 1000, message = "subject must be at most 1000 characters")
        String subject
) {
}
