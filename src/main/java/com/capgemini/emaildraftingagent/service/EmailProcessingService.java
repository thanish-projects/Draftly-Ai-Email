package com.capgemini.emaildraftingagent.service;

import com.capgemini.emaildraftingagent.dto.EmailProcessingResponse;
import com.capgemini.emaildraftingagent.dto.EmailTask;
import com.capgemini.emaildraftingagent.dto.MeetingSlot;
import com.capgemini.emaildraftingagent.model.EmailHistory;
import com.capgemini.emaildraftingagent.repository.EmailHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailProcessingService {

    private static final TypeReference<List<EmailTask>> TASK_LIST = new TypeReference<>() { };
    private static final TypeReference<List<MeetingSlot>> MEETING_SLOT_LIST = new TypeReference<>() { };

    private final EmailHistoryRepository repository;
    private final ObjectMapper objectMapper;

    public EmailProcessingService(EmailHistoryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public EmailProcessingResponse getById(long id) {
        EmailHistory history = repository.findById(id)
                .orElseThrow(() -> new EmailNotFoundException(id));
        return toResponse(history, readTasks(history.getTasksJson()),
                readMeetingSlots(history.getMeetingSlotsJson()));
    }

    @Transactional(readOnly = true)
    public Page<EmailProcessingResponse> getHistory(Pageable pageable) {
        return repository.findAll(pageable).map(history -> toResponse(
                history, readTasks(history.getTasksJson()), readMeetingSlots(history.getMeetingSlotsJson())));
    }

    private List<EmailTask> readTasks(String value) {
        return readJson(value, TASK_LIST);
    }

    private List<MeetingSlot> readMeetingSlots(String value) {
        return readJson(value, MEETING_SLOT_LIST);
    }

    private <T> T readJson(String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored email analysis contains invalid JSON.", exception);
        }
    }

    private static EmailProcessingResponse toResponse(EmailHistory history, List<EmailTask> tasks,
                                                       List<MeetingSlot> meetingSlots) {
        return new EmailProcessingResponse(history.getId(), history.getSender(), history.getSubject(),
                history.getIntent(), tasks, history.getPriority(), meetingSlots, history.getReplyDraft(),
                history.getCreatedAt());
    }

}
