package com.technicalchallenge.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ApiExceptionHandler
 *
 * ADDED: Centralized exception handler to convert security/authorization
 * exceptions into compact, user-friendly JSON messages suitable for the UI.I
 * was getting a long 403 message when e.g trader Simon is logged in but Simon
 * tries to view Joey's trade dashboard. This can confuse traders
 * Prevents exposing internal stack traces to clients while preserving server
 * logs for debugging. Returns contextual messages for common cases such as
 * create/delete trade attempts by unauthorized users.
 */
@ControllerAdvice
public class ApiExceptionHandler {

    // ADDED: Provide a compact, user-friendly response for AccessDeniedException
    // so clients (UI) see a short message instead of a full stack trace JSON.
    // This controller advice only formats AccessDeniedException responses.
    // It does not perform authorization checks or change authentication state.
    // Any logic that allows or denies data access lives in security configuration
    // or service/controller guards (e.g., TradeDashboardService).
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex,
            HttpServletRequest request) {
        // Business intent: present clear guidance to the user (e.g., "You do
        // not have permission to create trades") while keeping server logs
        // unchanged. Authorization rules remain enforced by @PreAuthorize
        // annotations and SecurityConfig; this handler only formats the
        // resulting 403 response.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", HttpStatus.FORBIDDEN.getReasonPhrase());

        // ADDED: Return a contextual, friendly message depending on the path
        // and method so the UI can show precise guidance to the user.
        String method = request.getMethod();
        String uri = request.getRequestURI() == null ? "" : request.getRequestURI();

        // Default message used for attempts to view or modify other traders'
        // trade details. Keep this message for the common case where a trader
        // is trying to access another trader's data.
        String message = "You do not have the privilege to view other traders' trade details";

        // Create trade (POST /api/trades)
        if ("POST".equalsIgnoreCase(method) && uri.matches(".*/api/trades/?$")) {
            message = "You do not have permission to create trades.";
        }

        // Delete trade (DELETE /api/trades/{id})
        else if ("DELETE".equalsIgnoreCase(method) && uri.matches(".*/api/trades(/.*)?")) {
            message = "You do not have permission to delete trades.";
        }

        // Audit trail endpoint: provide a clearer, more specific message so
        // owners are not confused when they see a 403. Business policy still
        // enforces ADMIN/MIDDLE_OFFICE only here, so the message explains that
        // clearly. We intentionally do NOT replace the default message above
        // for the other case where a trader tries to view another trader's
        // trade details.
        else if ("GET".equalsIgnoreCase(method) && uri.matches(".*/api/trades/\\d+/audit-trail/?$")) {
            message = "Only Admin or Middle Office users may view audit history for trades.";
        }

        body.put("message", message);
        body.put("path", uri);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    /**
     * Handle IllegalArgumentException thrown by validation logic and map to HTTP
     * 400
     * so clients receive a clear Bad Request response instead of a 500.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex,
            HttpServletRequest request) {
        // Validation failures in the service layer (for example, settlement
        // instruction content checks) previously surfaced as uncaught
        // IllegalArgumentException and were returned to clients as HTTP 500
        // Internal Server Error. That response is misleading for client-side
        // validation issues.
        //
        // Business scenario that triggered change: a client attempted to
        // update settlement instructions via
        // PUT /api/trades/{id}/settlement-instructions with a value that
        // contained unsupported characters (for instance unescaped quotes or
        // previously unallowed symbols). The validator raised an
        // IllegalArgumentException which should be communicated to the UI as
        // a Bad Request (HTTP 400) with a clear message so the user can
        // correct their input.
        //
        // Purpose: map validation exceptions to HTTP 400 and provide a
        // concise error message (message = validation text). This improves UX
        // (frontend can show actionable feedback) and prevents confusing 500
        // errors for user-correctable problems.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("message", ex.getMessage() == null ? "Bad request" : ex.getMessage());
        body.put("path", request.getRequestURI() == null ? "" : request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Handle javax validation failures (@Valid) and return field-level messages
     * so clients can show precise validation errors instead of a generic 400.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    // CHANGED: added handler to return field-level validation errors
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        // CHANGED: Added this handler to return field-level validation
        // messages for @Valid failures. Previously the client received a
        // generic 400 with no detail; this makes debugging frontend payload
        // issues (missing fields / invalid values) much easier.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());

        // Collect field -> message
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(err -> err.getField(), err -> err.getDefaultMessage(),
                        (a, b) -> a + "; " + b));

        body.put("message", "Validation failed");
        body.put("errors", fieldErrors);
        body.put("path", request.getRequestURI() == null ? "" : request.getRequestURI());
        return ResponseEntity.badRequest().body(body);
    }
}
