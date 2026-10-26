package com.capgemini.emaildraftingagent.controller;

import com.capgemini.emaildraftingagent.service.AiAnalysisException;
import com.capgemini.emaildraftingagent.service.EmailNotFoundException;
import com.capgemini.emaildraftingagent.service.SupervisorRejectedException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(EmailNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EmailNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(AiAnalysisException.class)
    public ResponseEntity<Map<String, Object>> handleAiAnalysis(AiAnalysisException exception) {
        return error(HttpStatus.BAD_GATEWAY, exception.getMessage());
    }

    @ExceptionHandler(SupervisorRejectedException.class)
    public ResponseEntity<com.capgemini.emaildraftingagent.dto.SupervisorResponse> handleSupervisorRejection(
            SupervisorRejectedException exception) {
        return ResponseEntity.unprocessableEntity().body(exception.getResponse());
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, Object>> handleProviderError(RestClientException exception) {
        return error(HttpStatus.BAD_GATEWAY, "AI provider request failed: " + exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Request validation failed.");
        return error(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value for parameter: " + exception.getName());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return error(HttpStatus.BAD_REQUEST, "Request body is missing or contains invalid JSON.");
    }

    private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "timestamp", Instant.now(),
                "status", status.value(),
                "error", status.getReasonPhrase(),
                "message", message));
    }
}
