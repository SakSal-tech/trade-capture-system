package com.technicalchallenge.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// I was getting the default BasicErrorController is showing the full stack trace.
// This class is to override it using a @ControllerAdvice to catch exceptions globally and return only the meaningful validation message.
//
// Do not change the handlers' return shapes without coordinating with the frontend team.
// The frontend expects a JSON object containing at least the following keys:
// - timestamp: when the error occurred (LocalDateTime string)
// - status: HTTP status code
// - message: human-readable message suitable for display
//
// Suggested enhancements (non-breaking):
// - add handlers for MethodArgumentNotValidException and ConstraintViolationException to return
//   a structured "errors" array with field-level messages.
// - include the request path in responses for easier client-side logging/tracing.

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle runtime exceptions thrown from controllers or services.
     *
     * Behaviour: returns HTTP 400 (Bad Request) with a minimal JSON body.
     * Keys in the response map:
     * - timestamp: LocalDateTime.now() when the exception was handled
     * - status: integer HTTP status code (400)
     * - message: exception message (ex.getMessage())
     *
     * Important: This is intentionally broad; consider replacing with more specific
     * handlers (e.g. ResponseStatusException) so don't accidentally mask server
     * errors
     * as client errors.
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        // ISO-like timestamp is fine for display; consider switching to Instant/UTC for
        // consistency
        errorResponse.put("timestamp", LocalDateTime.now());
        // Status is the HTTP code the client should interpret
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        // A short human-readable message suitable for UI display
        errorResponse.put("message", ex.getMessage());
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Fallback handler for any Exception not handled elsewhere.
     *
     * Behaviour: returns HTTP 500 with a generic message to avoid leaking internal
     * details.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> errorResponse = new HashMap<>();
        // Keep the payload minimal and safe for displaying to end users
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("message", "Unexpected server error");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
