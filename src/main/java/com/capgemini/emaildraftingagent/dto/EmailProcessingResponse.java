package com.capgemini.emaildraftingagent.dto;

import com.capgemini.emaildraftingagent.model.EmailIntent;
import com.capgemini.emaildraftingagent.model.EmailPriority;
import java.time.Instant;
import java.util.List;

public record EmailProcessingResponse(
        Long id,
        String sender,
        String subject,
        EmailIntent intent,
        List<EmailTask> tasks,
        EmailPriority priority,
        List<MeetingSlot> meetingSlots,
        String replyDraft,
        Instant createdAt
) {
}
