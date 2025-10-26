package com.technicalchallenge.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * TestSecurityConfig
 *
 * This configuration is only loaded during tests (because
 * of @TestConfiguration).
 * It defines a minimal, isolated security setup for integration and controller
 * tests
 * so that Spring Boot can create a SecurityFilterChain without requiring
 * real authentication or complex user setup.
 *
 * It’s safe for tests but should never be used in production.
 */
@TestConfiguration
public class TestSecurityConfig {

        /**
         * Defines an in-memory user details service for tests.
         *
         * - Uses InMemoryUserDetailsManager, which stores users only in memory (not
         * persistent).
         * - Adds a single test user with:
         * username: "testuser"
         * password: "password"
         * role: "USER"
         *
         * This allows tests that rely on authentication (e.g. @WithMockUser)
         * to pass when a user context is required.
         */
        @Bean
        public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
                var mgr = new org.springframework.security.provisioning.InMemoryUserDetailsManager();
                mgr.createUser(org.springframework.security.core.userdetails.User
                                .withUsername("testuser")
                                .password(passwordEncoder.encode("password"))
                                .roles("USER")
                                .build());
                return mgr;
        }

        /**
         * Password encoder used for the test user.
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder(); // Refactored changed from NoOpPasswordEncoder to secure,
                                                    // recommended for production
        }

        /**
         * Defines a simple SecurityFilterChain for the test environment.
         *
         * - Permits access to endpoints commonly used in tests or dev tools:
         * /h2-console/**, /actuator/**, /api-docs/**, /v3/api-docs/**,
         * /swagger-ui/**, /swagger-ui.html
         *
         * - Requires authentication for all other endpoints.
         * - Disables CSRF protection for the H2 console (to allow browser access).
         * - Sets frame options to SAMEORIGIN so the H2 console can render properly.
         * - Enables both HTTP Basic Auth and form login with default settings,
         * making it easier for tests or Swagger UI to authenticate when needed.
         *
         * This configuration ensures that Spring Security does not block
         * integration tests or H2 console access, while still simulating
         * a minimal authentication layer.
         */
        @Bean
        public org.springframework.security.web.SecurityFilterChain testSecurityFilterChain(
                        org.springframework.security.config.annotation.web.builders.HttpSecurity http)
                        throws Exception {

                http
                                // Configure which requests are allowed without authentication
                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers("/h2-console/**", "/actuator/**", "/api-docs/**",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**", "/swagger-ui.html")
                                                .permitAll() // allow public access to these paths
                                                .anyRequest().authenticated()) // all other endpoints require
                                                                               // authentication

                                // Disable CSRF protection for the H2 console so it works in tests
                                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))

                                // Allow frames from the same origin (needed for H2 console)
                                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                                // Enable HTTP Basic authentication for simplicity
                                .httpBasic(org.springframework.security.config.Customizer.withDefaults())

                                // Enable form login with default settings
                                .formLogin(org.springframework.security.config.Customizer.withDefaults());

                return http.build();
        }
}
