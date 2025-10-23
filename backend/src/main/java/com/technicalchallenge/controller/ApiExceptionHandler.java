package com.technicalchallenge.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

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
    // NOTE: This controller advice only formats AccessDeniedException responses.
    // It does not perform authorization checks or change authentication state.
    // Any logic that allows or denies data access lives in security configuration
    // or service/controller guards (e.g., TradeDashboardService).
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex,
            HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", HttpStatus.FORBIDDEN.getReasonPhrase());

        // ADDED: Return a contextual, friendly message depending on the path
        // and method so the UI can show precise guidance to the user.
        String method = request.getMethod();
        String uri = request.getRequestURI() == null ? "" : request.getRequestURI();
        String message = "You do not have the privilege to view other traders' trade details";

        // Create trade (POST /api/trades)
        if ("POST".equalsIgnoreCase(method) && uri.matches(".*/api/trades/?$")) {
            message = "You do not have permission to create trades.";
        }

        // Delete trade (DELETE /api/trades/{id})
        else if ("DELETE".equalsIgnoreCase(method) && uri.matches(".*/api/trades(/.*)?")) {
            message = "You do not have permission to delete trades.";
        }

        // Default for other forbidden access
        body.put("message", message);
        body.put("path", uri);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
}
