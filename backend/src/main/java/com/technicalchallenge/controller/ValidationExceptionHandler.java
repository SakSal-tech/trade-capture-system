package com.technicalchallenge.controller;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.ResponseEntity;

/**
 * Global exception handler for validation errors in REST controllers.
 * Reason for creation:
 * - Fixes the error where validation failures (e.g., negative notional in
 * TradeLegDTO) result in a 400 Bad Request with an empty response body.
 * - Cause: By default, when bean validation fails (such as @NotNull
 * or @Positive), Spring throws a MethodArgumentNotValidException and returns a
 * 400 status, but does not include the validation error message in the response
 * body.
 * - This handler intercepts those exceptions and ensures the error message
 * (e.g., "Notional must be positive") is returned in the response body, so
 * tests and clients can see the reason for the failure.
 *
 * This class is automatically detected by Spring due to
 * the @RestControllerAdvice annotation.
 * It intercepts validation exceptions (such as when a DTO fails bean
 * validation) and returns
 * a user-friendly error message in the HTTP response body.
 */

// Tells Spring that this class should automatically watch for exceptions
// (errors) happening in REST controllers.
@RestControllerAdvice
public class ValidationExceptionHandler {
    /**
     * Handles validation errors thrown when a @Valid DTO fails bean validation. If
     * there's a validation error (like when a @Valid DTO fails bean validation),
     * call this method to handle it.
     * 
     * @param ex the exception containing validation error details
     * @return a 400 Bad Request response with the validation error message as the
     *         body
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    // Gets the error details from the exception.Extracts the first validation error
    // message (e.g., "field must not be null"). Returns a 400 Bad Request response
    // with that message in the body.
    public ResponseEntity<String> handleValidationException(MethodArgumentNotValidException ex) {
        // Extract the default error message from the first field error in the
        // validation result
        String message = "Validation failed";
        if (ex.getBindingResult().getFieldError() != null) {
            var fieldError = ex.getBindingResult().getFieldError();
            String fieldErrorMessage = (fieldError != null) ? fieldError.getDefaultMessage() : null;// Added validation
                                                                                                    // to check in null
                                                                                                    // to avoid possible
                                                                                                    // null.
            message = (fieldErrorMessage != null) ? fieldErrorMessage : message;
        }
        // Return a 400 Bad Request response with the error message in the body
        return ResponseEntity.badRequest().body(message);
    }
}
