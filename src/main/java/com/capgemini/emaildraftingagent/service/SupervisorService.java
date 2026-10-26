package com.capgemini.emaildraftingagent.service;

import com.capgemini.emaildraftingagent.dto.ExtractedTask;
import com.capgemini.emaildraftingagent.dto.SupervisorRequest;
import com.capgemini.emaildraftingagent.dto.SupervisorResponse;
import com.capgemini.emaildraftingagent.model.EmailIntent;
import com.capgemini.emaildraftingagent.model.SupervisionStatus;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class SupervisorService {

    private static final Pattern ISO_DATE = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}\\b");
    private static final Pattern NAMED_DATE = Pattern.compile(
            "(?i)\\b(?:Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|"
                    + "Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)"
                    + "\\s+\\d{1,2}(?:,?\\s+\\d{4})?\\b|\\b\\d{1,2}\\s+(?:Jan(?:uary)?|Feb(?:ruary)?|"
                    + "Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|"
                    + "Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)(?:\\s+\\d{4})?\\b");
    private static final Pattern WEEKDAY = Pattern.compile(
            "(?i)\\b(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|"
                    + "Mon|Tue(?:s)?|Wed(?:nes)?|Thu(?:rs)?|Fri|Sat|Sun)\\b");
    private static final Pattern RELATIVE_DATE = Pattern.compile(
            "(?i)\\b(today|tomorrow|yesterday|this\\s+week|next\\s+week|this\\s+month|next\\s+month)\\b");
    private static final Pattern REQUIRED_REPLY_OPENING = Pattern.compile("(?i)^\\s*hello\\b");
    private static final Pattern REQUIRED_REPLY_CLOSING = Pattern.compile("(?is)\\bregards,?\\s*\\n\\s*AI Assistant\\s*$");

    public SupervisorResponse supervise(SupervisorRequest request) {
        List<String> issues = new ArrayList<>();
        if (request == null) {
            return new SupervisorResponse(SupervisionStatus.REJECTED, List.of("Request is missing"));
        }
        if (request.emailContent() == null || request.emailContent().isBlank()) {
            issues.add("Email content is missing");
        }
        validateTasks(request.tasks(), request.intent(), issues);
        validateMeetingSlots(request.meetingSuggestions(), request.intent(), issues);
        validateReply(request.reply(), issues);
        if (request.emailContent() != null && request.tasks() != null && request.meetingSuggestions() != null
                && request.reply() != null) {
            validateReplyDates(request, issues);
        }
        return new SupervisorResponse(issues.isEmpty() ? SupervisionStatus.APPROVED : SupervisionStatus.REJECTED,
                List.copyOf(issues));
    }

    private static void validateTasks(List<ExtractedTask> tasks, EmailIntent intent, List<String> issues) {
        if (tasks == null) {
            issues.add("Tasks list is missing");
            return;
        }
        if ((intent == EmailIntent.TASK || intent == EmailIntent.BOTH) && tasks.isEmpty()) {
            issues.add("Tasks are missing for an actionable intent");
        }
        for (int index = 0; index < tasks.size(); index++) {
            ExtractedTask task = tasks.get(index);
            if (task == null) {
                issues.add("Task %d is empty".formatted(index + 1));
                continue;
            }
            if (task.title() == null || task.title().isBlank()) {
                issues.add("Task %d title is missing".formatted(index + 1));
            }
            if (task.owner() == null || task.owner().isBlank()) {
                issues.add("Task %d owner is missing".formatted(index + 1));
            }
            if (task.dueDate() == null) {
                issues.add("Due date missing");
            } else {
                try {
                    LocalDate.parse(task.dueDate().toString());
                } catch (RuntimeException exception) {
                    issues.add("Due date is invalid for task %d".formatted(index + 1));
                }
            }
            if (task.priority() == null) {
                issues.add("Task %d priority is missing".formatted(index + 1));
            }
        }
    }

    private static void validateMeetingSlots(List<String> meetingSuggestions, EmailIntent intent,
                                             List<String> issues) {
        if (meetingSuggestions == null) {
            issues.add("Meeting suggestions list is missing");
            return;
        }
        if ((intent == EmailIntent.MEETING || intent == EmailIntent.BOTH) && meetingSuggestions.isEmpty()) {
            issues.add("Meeting slots are missing for a meeting intent");
        }
        for (int index = 0; index < meetingSuggestions.size(); index++) {
            String slot = meetingSuggestions.get(index);
            if (slot == null || slot.isBlank()) {
                issues.add("Meeting slot %d is empty".formatted(index + 1));
            }
        }
    }

    private static void validateReply(String reply, List<String> issues) {
        if (reply == null || reply.isBlank()) {
            issues.add("Generated reply is empty");
            return;
        }
        if (reply.length() > 10000) {
            issues.add("Generated reply exceeds the 10000-character limit");
        }
        if (!REQUIRED_REPLY_OPENING.matcher(reply).find()) {
            issues.add("Generated reply must begin with a professional greeting");
        }
        if (!REQUIRED_REPLY_CLOSING.matcher(reply).find()) {
            issues.add("Generated reply must end with Regards, AI Assistant");
        }
    }

    private static void validateReplyDates(SupervisorRequest request, List<String> issues) {
        StringBuilder allowedDateEvidence = new StringBuilder(request.emailContent().toLowerCase(Locale.ROOT));
        request.meetingSuggestions().stream()
                .filter(slot -> slot != null)
                .map(slot -> slot.toLowerCase(Locale.ROOT))
                .forEach(allowedDateEvidence::append);

        for (ExtractedTask task : request.tasks()) {
            if (task == null) {
                continue;
            }
            if (task.title() != null) {
                allowedDateEvidence.append(' ').append(task.title().toLowerCase(Locale.ROOT));
            }
            if (task.dueDate() != null) {
                LocalDate dueDate = task.dueDate();
                allowedDateEvidence.append(' ').append(dueDate)
                        .append(' ').append(dueDate.getMonth().name().toLowerCase(Locale.ROOT))
                        .append(' ').append(dueDate.getDayOfMonth())
                        .append(' ').append(dueDate.getDayOfWeek().name().toLowerCase(Locale.ROOT))
                        .append(' ').append(dueDate.format(DateTimeFormatter.ofPattern("MMMM d, uuuu",
                                Locale.ENGLISH)).toLowerCase(Locale.ROOT))
                        .append(' ').append(dueDate.format(DateTimeFormatter.ofPattern("d MMMM uuuu",
                                Locale.ENGLISH)).toLowerCase(Locale.ROOT));
            }
        }

        String evidence = allowedDateEvidence.toString();
        String reply = request.reply();
        validateDateMentions(ISO_DATE, reply, evidence, issues);
        validateDateMentions(NAMED_DATE, reply, evidence, issues);
        validateDateMentions(WEEKDAY, reply, evidence, issues);
        validateDateMentions(RELATIVE_DATE, reply, evidence, issues);
    }

    private static void validateDateMentions(Pattern pattern, String reply, String evidence, List<String> issues) {
        Matcher matcher = pattern.matcher(reply);
        while (matcher.find()) {
            String mention = matcher.group().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
            if (!evidence.contains(mention)) {
                issues.add("Reply contains an unsupported date: " + matcher.group());
            }
        }
    }
}
