package com.capgemini.emaildraftingagent.service;

import com.capgemini.emaildraftingagent.dto.SupervisorResponse;

public class SupervisorRejectedException extends RuntimeException {

    private final SupervisorResponse response;

    public SupervisorRejectedException(SupervisorResponse response) {
        super("Generated email output did not pass supervision.");
        this.response = response;
    }

    public SupervisorResponse getResponse() {
        return response;
    }
}
