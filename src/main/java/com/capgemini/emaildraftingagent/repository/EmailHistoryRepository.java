package com.capgemini.emaildraftingagent.repository;

import com.capgemini.emaildraftingagent.model.EmailHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailHistoryRepository extends JpaRepository<EmailHistory, Long> {
}
