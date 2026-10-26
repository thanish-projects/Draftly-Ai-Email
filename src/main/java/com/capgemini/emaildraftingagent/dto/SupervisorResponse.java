package com.capgemini.emaildraftingagent.dto;

import com.capgemini.emaildraftingagent.model.SupervisionStatus;
import java.util.List;

public record SupervisorResponse(SupervisionStatus status, List<String> issues) {
}
