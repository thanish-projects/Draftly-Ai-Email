package com.capgemini.emaildraftingagent.service;

import java.util.List;

public record TaskExtractionAiResult(List<Task> tasks) {

    public record Task(String title, String owner, String dueDate) {
    }
}
