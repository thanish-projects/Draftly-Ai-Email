package com.capgemini.emaildraftingagent.repository;

import com.capgemini.emaildraftingagent.model.EmailTaskEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTaskRepository extends JpaRepository<EmailTaskEntity, Long> {

    List<EmailTaskEntity> findAllByOrderByCreatedAtDesc();
}
