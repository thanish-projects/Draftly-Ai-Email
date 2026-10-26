package com.capgemini.emaildraftingagent.service;

import com.capgemini.emaildraftingagent.dto.SchedulingRequest;
import com.capgemini.emaildraftingagent.dto.SchedulingResponse;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class SchedulingService {

    private static final List<AvailableSlot> AVAILABLE_SLOTS = List.of(
            new AvailableSlot("tomorrow-2-pm", "Tomorrow 2:00 PM"),
            new AvailableSlot("tomorrow-3-pm", "Tomorrow 3:00 PM"),
            new AvailableSlot("tomorrow-4-pm", "Tomorrow 4:00 PM"),
            new AvailableSlot("next-week-monday-10-am", "Next Week Monday 10:00 AM"),
            new AvailableSlot("next-week-tuesday-2-pm", "Next Week Tuesday 2:00 PM"),
            new AvailableSlot("next-week-wednesday-11-am", "Next Week Wednesday 11:00 AM"));

    private static final int SUGGESTION_COUNT = 3;

    private final OpenAIService aiClient;

    public SchedulingService(OpenAIService aiClient) {
        this.aiClient = aiClient;
    }

    public SchedulingResponse suggest(SchedulingRequest request) {
        if (request.emailContent() == null || request.emailContent().isBlank()) {
            throw new AiAnalysisException("Email content is required to suggest meeting times.");
        }

        MeetingTimePreferences preferences = aiClient.extractMeetingTimePreferences(request.emailContent().strip());
        if (preferences == null || preferences.preferredSlotIds() == null) {
            throw new AiAnalysisException("The AI provider returned invalid meeting time preferences.");
        }

        Set<String> preferredIds = new LinkedHashSet<>(preferences.preferredSlotIds());
        Set<String> availableIds = AVAILABLE_SLOTS.stream().map(AvailableSlot::id)
                .collect(java.util.stream.Collectors.toSet());
        if (!availableIds.containsAll(preferredIds)) {
            throw new AiAnalysisException("The AI provider referenced an unavailable meeting slot.");
        }

        List<AvailableSlot> orderedSlots = new ArrayList<>();
        preferences.preferredSlotIds().stream()
                .map(SchedulingService::findSlot)
                .filter(slot -> !orderedSlots.contains(slot))
                .forEach(orderedSlots::add);
        AVAILABLE_SLOTS.stream()
                .filter(slot -> !orderedSlots.contains(slot))
                .forEach(orderedSlots::add);

        return new SchedulingResponse(orderedSlots.stream()
                .limit(SUGGESTION_COUNT)
                .map(AvailableSlot::label)
                .toList());
    }

    private static AvailableSlot findSlot(String id) {
        return AVAILABLE_SLOTS.stream()
                .filter(slot -> slot.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AiAnalysisException("Unknown meeting slot: " + id));
    }

    private record AvailableSlot(String id, String label) {
    }
}
