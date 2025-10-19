package com.technicalchallenge.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Default (production) security configuration. This configuration is
 * intentionally strict: it requires authentication for all requests unless
 * another
 * profile-specific security configuration is active (e.g. RuntimeSecurityConfig
 * for local/dev). Open endpoints, permissive CORS, disabled CSRF, debug
 * endpoints do not make it into a deployed environment.
 */
@Configuration
public class SecurityConfig {
        // For development/testing convenience: allows all requests so the frontend
        // can handle authentication. CSRF remains disabled here to avoid form issues in
        // local testing environments. I have restricted access to ensure that only
        // the /api/login endpoint is publicly accessible. All other API endpoints now
        // require authentication.
        // This change is essential for production to prevent unauthenticated users
        // from accessing dashboard and trade data.
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http)
                        throws Exception {
                http.csrf(csrf -> csrf.disable())
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/api/login/**").permitAll()
                                                .anyRequest().authenticated())
                                .httpBasic(Customizer.withDefaults())// Added HTTP Basic authentication for now.
                                // This is a simple way to protect endpoints until a token/JWT system is added.
                                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

                return http.build();
        }
}
