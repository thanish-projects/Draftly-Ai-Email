package com.capgemini.emaildraftingagent.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import com.capgemini.emaildraftingagent.model.EmailTaskEntity;
import com.capgemini.emaildraftingagent.model.TaskPriority;
import com.capgemini.emaildraftingagent.repository.EmailTaskRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskQueryServiceTest {

    @Mock
    private EmailTaskRepository repository;

    @Test
    void returnsStoredTasksInRepositorySortOrder() {
        EmailTaskEntity task = new EmailTaskEntity("Finalize proposal", "Current User",
                LocalDate.of(2026, 10, 16), TaskPriority.HIGH, "email", null);
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(task));

        var result = new TaskQueryService(repository).getTasks();

        assertEquals(1, result.size());
        assertEquals("Finalize proposal", result.getFirst().title());
        assertEquals("Current User", result.getFirst().owner());
        assertEquals(LocalDate.of(2026, 10, 16), result.getFirst().dueDate());
        assertEquals(TaskPriority.HIGH, result.getFirst().priority());
    }
}
