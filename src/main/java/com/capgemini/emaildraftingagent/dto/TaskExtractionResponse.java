package com.capgemini.emaildraftingagent.dto;

import java.util.List;

public record TaskExtractionResponse(List<ExtractedTask> tasks) {
}
