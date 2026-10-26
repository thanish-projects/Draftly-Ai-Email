package com.capgemini.emaildraftingagent.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "email_history")
public class EmailHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email_content", nullable = false, columnDefinition = "text")
    private String emailContent;

    @Column(name = "sender", length = 500)
    private String sender;

    @Column(name = "subject", length = 1000)
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailIntent intent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmailPriority priority;

    @Column(name = "tasks_json", nullable = false, columnDefinition = "text")
    private String tasksJson;

    @Column(name = "meeting_slots_json", nullable = false, columnDefinition = "text")
    private String meetingSlotsJson;

    @Column(name = "reply_draft", nullable = false, columnDefinition = "text")
    private String replyDraft;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EmailHistory() {
    }

    public EmailHistory(String emailContent, String sender, String subject, EmailIntent intent,
                        EmailPriority priority, String tasksJson, String meetingSlotsJson,
                        String replyDraft) {
        this.emailContent = emailContent;
        this.sender = sender;
        this.subject = subject;
        this.intent = intent;
        this.priority = priority;
        this.tasksJson = tasksJson;
        this.meetingSlotsJson = meetingSlotsJson;
        this.replyDraft = replyDraft;
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

    public String getEmailContent() {
        return emailContent;
    }

    public String getSender() {
        return sender;
    }

    public String getSubject() {
        return subject;
    }

    public EmailIntent getIntent() {
        return intent;
    }

    public EmailPriority getPriority() {
        return priority;
    }

    public String getTasksJson() {
        return tasksJson;
    }

    public String getMeetingSlotsJson() {
        return meetingSlotsJson;
    }

    public String getReplyDraft() {
        return replyDraft;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
