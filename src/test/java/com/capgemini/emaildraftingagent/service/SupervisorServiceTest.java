package com.capgemini.emaildraftingagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.capgemini.emaildraftingagent.dto.ExtractedTask;
import com.capgemini.emaildraftingagent.dto.SupervisorRequest;
import com.capgemini.emaildraftingagent.dto.SupervisorResponse;
import com.capgemini.emaildraftingagent.model.EmailIntent;
import com.capgemini.emaildraftingagent.model.SupervisionStatus;
import com.capgemini.emaildraftingagent.model.TaskPriority;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SupervisorServiceTest {

    private final SupervisorService service = new SupervisorService();

    @Test
    void approvesWellFormedTaskAndMeetingReply() {
        SupervisorResponse response = service.supervise(new SupervisorRequest(
                "Let's meet tomorrow and finalize the budget proposal by Friday.",
                EmailIntent.BOTH,
                List.of(new ExtractedTask("Finalize budget proposal", "Current User",
                        LocalDate.of(2026, 10, 16), TaskPriority.HIGH)),
                List.of("Tomorrow 2:00 PM", "Tomorrow 3:00 PM"),
                """
                        Hello,

                        Thank you for your email.

                        I have noted the following action item:
                        - Finalize budget proposal by 2026-10-16.

                        For the meeting, I am available at:
                        - Tomorrow 2:00 PM
                        - Tomorrow 3:00 PM

                        Please let me know your preferred slot.

                        Regards,
                        AI Assistant"""));

        assertEquals(new SupervisorResponse(SupervisionStatus.APPROVED, List.of()), response);
    }

    @Test
    void rejectsMissingTaskDueDate() {
        SupervisorResponse response = service.supervise(new SupervisorRequest(
                "Please finalize the proposal.",
                EmailIntent.TASK,
                List.of(new ExtractedTask("Finalize proposal", "Current User", null, TaskPriority.LOW)),
                List.of(),
                "Hello,\n\nThank you for your email.\n\nRegards,\nAI Assistant"));

        assertEquals(SupervisionStatus.REJECTED, response.status());
        assertEquals(List.of("Due date missing"), response.issues());
    }

    @Test
    void rejectsEmptyTasksForTaskIntent() {
        SupervisorResponse response = service.supervise(new SupervisorRequest(
                "Please finalize the proposal by Friday.",
                EmailIntent.TASK,
                List.of(),
                List.of(),
                "Hello,\n\nThank you for your email.\n\nRegards,\nAI Assistant"));

        assertEquals(SupervisionStatus.REJECTED, response.status());
        assertEquals(List.of("Tasks are missing for an actionable intent"), response.issues());
    }

    @Test
    void rejectsMeetingIntentWithoutAvailableSlots() {
        SupervisorResponse response = service.supervise(new SupervisorRequest(
                "Let's meet tomorrow.",
                EmailIntent.MEETING,
                List.of(),
                List.of(),
                "Hello,\n\nThank you for your email.\n\nRegards,\nAI Assistant"));

        assertEquals(SupervisionStatus.REJECTED, response.status());
        assertEquals(List.of("Meeting slots are missing for a meeting intent"), response.issues());
    }

    @Test
    void rejectsDateNotSupportedByEmailTasksOrSlots() {
        SupervisorResponse response = service.supervise(new SupervisorRequest(
                "Please finalize the proposal by Friday.",
                EmailIntent.TASK,
                List.of(new ExtractedTask("Finalize proposal", "Current User",
                        LocalDate.of(2026, 10, 16), TaskPriority.MEDIUM)),
                List.of(),
                "Hello,\n\nThank you for your email. I will finish this by 2026-10-20.\n\nRegards,\nAI Assistant"));

        assertEquals(SupervisionStatus.REJECTED, response.status());
        assertEquals(List.of("Reply contains an unsupported date: 2026-10-20"), response.issues());
    }

    @Test
    void rejectsReplyWithInvalidProfessionalStructure() {
        SupervisorResponse response = service.supervise(new SupervisorRequest(
                "Thanks for the update.",
                EmailIntent.GENERAL,
                List.of(),
                List.of(),
                "Done."));

        assertEquals(SupervisionStatus.REJECTED, response.status());
        assertEquals(List.of("Generated reply must begin with a professional greeting",
                "Generated reply must end with Regards, AI Assistant"), response.issues());
    }
}
