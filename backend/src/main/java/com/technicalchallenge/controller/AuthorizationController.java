package com.technicalchallenge.controller;

import com.technicalchallenge.service.AuthorizationService;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

@RestController
@RequestMapping("/api/login")
@Validated
@AllArgsConstructor
public class AuthorizationController {

    private final AuthorizationService authorizationService;
    private final AuthenticationManager authenticationManager;

    /**
     * Programmatic login: authenticate credentials via AuthenticationManager
     * and set the resulting Authentication into the SecurityContext. This
     * allows subsequent requests from the same session/client to be treated
     * as authenticated by Spring Security (session cookie).
     *
     * This endpoint still returns 403 when authentication fails.
     */
    @PostMapping("/{userName}")
    public ResponseEntity<?> login(@PathVariable(name = "userName") String userName,
            @RequestParam(name = "Authorization") String authorization,
            HttpServletRequest request) {

        try {
            // Build an authentication token with the supplied credentials
            UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(userName,
                    authorization);

            // Authenticate using the application's AuthenticationManager
            Authentication authentication = authenticationManager.authenticate(token);

            // On success, set into the security context
            SecurityContextHolder.getContext().setAuthentication(authentication);
            // Persist the SecurityContext into the HTTP session so subsequent
            // requests from the same client (which send the JSESSIONID cookie)
            // are treated as authenticated by Spring Security.
            if (request != null) {
                request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                        SecurityContextHolder.getContext());
            }

            return ResponseEntity.ok("Login successful");
        } catch (Exception e) {
            // Fallback to the existing service validation if required
            boolean ok = authorizationService.authenticateUser(userName, authorization);
            if (ok) {
                // If the fallback authentication succeeded, set a minimal
                // Authentication into the SecurityContext.
                UsernamePasswordAuthenticationToken fallback = new UsernamePasswordAuthenticationToken(
                        userName, null, java.util.Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(fallback);
                if (request != null) {
                    request.getSession(true).setAttribute(
                            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                            SecurityContextHolder.getContext());
                }
                return ResponseEntity.ok("Login successful (fallback)");
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Login failed");
        }
    }
}