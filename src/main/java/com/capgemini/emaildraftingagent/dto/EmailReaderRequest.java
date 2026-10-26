package com.capgemini.emaildraftingagent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailReaderRequest(
        @Size(max = 1000, message = "subject must be at most 1000 characters")
        String subject,
        @NotBlank(message = "email is required")
        @Size(max = 50000, message = "email must be at most 50000 characters")
        String email
) {
}
