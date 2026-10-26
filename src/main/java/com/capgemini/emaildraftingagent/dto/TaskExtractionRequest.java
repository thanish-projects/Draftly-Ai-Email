package com.capgemini.emaildraftingagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TaskExtractionRequest(
        @NotBlank(message = "emailContent is required")
        @Size(max = 50000, message = "emailContent must be at most 50000 characters")
        String emailContent
) {
}
