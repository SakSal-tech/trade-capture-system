package com.technicalchallenge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails; // removed
//unused import
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * SecurityConfig
 *
 * Main Spring Security configuration for the application.
 * 
 * This setup currently allows all HTTP requests (permitAll) to simplify testing
 * and integration. However, @EnableMethodSecurity ensures that method-level
 * security annotations like @PreAuthorize and @PostAuthorize in controllers
 * are still enforced.
 *
 * In production, tighten access control by enabling authentication
 * and replacing permitAll() with role-based authorization.
 */
@Configuration
@EnableMethodSecurity // Enables @PreAuthorize / @PostAuthorize annotations for controller methods
public class SecurityConfig {
    // Enable method-level security and provide in-memory users for test/dev

    // @Bean
    // public UserDetailsService userDetailsService() {
    // InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
    // // TRADER: Can create, amend, terminate, cancel trades
    // manager.createUser(User.withUsername("traderUser").password("{noop}password")
    // .roles("TRADE_CREATE", "TRADE_AMEND", "TRADE_TERMINATE",
    // "TRADE_CANCEL").build());
    // // SALES: Can create and amend trades only
    // manager.createUser(User.withUsername("salesUser").password("{noop}password")
    // .roles("TRADE_CREATE", "TRADE_AMEND").build());
    // // MIDDLE_OFFICE: Can amend and view trades only
    // manager.createUser(User.withUsername("middleOfficeUser").password("{noop}password")
    // .roles("TRADE_AMEND", "TRADE_VIEW").build());
    // // SUPPORT: Can view trades only
    // manager.createUser(User.withUsername("supportUser").password("{noop}password")
    // .roles("TRADE_VIEW").build());
    // return manager;
    // }

    /**
     * PasswordEncoder bean.
     *
     * Provides password encoding support for the application.
     * Uses a DelegatingPasswordEncoder, which can handle multiple encoding types.
     * 
     * For development and testing, {noop} can be used for plain-text passwords.
     * 
     * Example: "{noop}password"
     *
     * In production, replace with stronger encoders (e.g., bcrypt).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use DelegatingPasswordEncoder for compatibility, but {noop} for test
        // passwords
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    /**
     * Configures HTTP security for the entire application.
     *
     * - `.anyRequest().permitAll()` → allows all requests without authentication.
     * This is intentional for testing and Swagger access.
     *
     * - `.csrf(csrf -> csrf.disable())` → disables CSRF protection to prevent
     * token errors when using API tools like Postman or Swagger.
     *
     * - `.formLogin(form -> form.disable())` → disables Spring Security’s default
     * form login page, preventing browser redirection to /login.
     *
     * - `.httpBasic(httpBasic -> httpBasic.disable())` → disables browser pop-ups
     * for Basic Auth.
     *
     * This setup is suitable for local development or automated tests,
     * where authentication is handled manually or mocked.
     */
    @Bean
    public org.springframework.security.web.SecurityFilterChain securityFilterChain(
            org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        http
                // Globally allow all requests (temporary for dev/testing)
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll())
                // Disable CSRF for non-browser API access
                .csrf(csrf -> csrf.disable())
                .httpBasic(httpBasic -> httpBasic.disable()) // Disable HTTP Basic authentication prompts
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable())
                // Disable Spring Security’s default form login
                // .formLogin(form -> form.disable())
                );
        // Build and return the configured security chain
        return http.build();
    }
}
