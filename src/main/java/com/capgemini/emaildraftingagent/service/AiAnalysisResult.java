package com.capgemini.emaildraftingagent.service;

import com.capgemini.emaildraftingagent.dto.EmailTask;
import com.capgemini.emaildraftingagent.dto.MeetingSlot;
import com.capgemini.emaildraftingagent.model.EmailIntent;
import com.capgemini.emaildraftingagent.model.EmailPriority;
import java.util.List;

public record AiAnalysisResult(
        EmailIntent intent,
        String sender,
        String subject,
        List<EmailTask> tasks,
        EmailPriority priority,
        List<MeetingSlot> meetingSlots,
        String replyDraft
) {
}
