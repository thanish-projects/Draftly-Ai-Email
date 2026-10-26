package com.capgemini.emaildraftingagent.service;

public class EmailNotFoundException extends RuntimeException {

    public EmailNotFoundException(long id) {
        super("Email history entry %d was not found.".formatted(id));
    }
}
