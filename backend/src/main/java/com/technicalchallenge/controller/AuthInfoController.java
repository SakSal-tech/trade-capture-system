package com.technicalchallenge.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple debugging endpoint that returns the current authenticated principal
 * and authorities. Useful to verify that login produced a server-side
 * Authentication and which roles/authorities are present.
 */
@RestController
@RequestMapping("/api")
public class AuthInfoController {

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return ResponseEntity.ok("");
        }

        // Return just the username string for simplicity (used by UI/login flows)
        return ResponseEntity.ok(auth.getName());
    }
}
