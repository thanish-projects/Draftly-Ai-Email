package com.capgemini.emaildraftingagent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "email_tasks")
public class EmailTaskEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 1000)
    private String title;

    @Column(nullable = false, length = 500)
    private String owner;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority;

    @Column(name = "source_email", nullable = false, columnDefinition = "text")
    private String sourceEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email_history_id")
    private EmailHistory emailHistory;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EmailTaskEntity() {
    }

    public EmailTaskEntity(String title, String owner, LocalDate dueDate, TaskPriority priority,
                           String sourceEmail, EmailHistory emailHistory) {
        this.title = title;
        this.owner = owner;
        this.dueDate = dueDate;
        this.priority = priority;
        this.sourceEmail = sourceEmail;
        this.emailHistory = emailHistory;
    }

    @PrePersist
    void setCreatedAt() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getOwner() {
        return owner;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public String getSourceEmail() {
        return sourceEmail;
    }

    public EmailHistory getEmailHistory() {
        return emailHistory;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
