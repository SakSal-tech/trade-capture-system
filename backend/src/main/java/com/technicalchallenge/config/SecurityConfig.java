package com.technicalchallenge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.context.annotation.Bean;
// removed unused imports (we wire a DatabaseUserDetailsService via DaoAuthenticationProvider)
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
// Authentication provider classes
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import com.technicalchallenge.security.DatabaseUserDetailsService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

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

    // Create a DaoAuthenticationProvider that wires DatabaseUserDetailsService
    // and the application's PasswordEncoder. This tells Spring Security how to
    // load users (from DB) and how to check/encode passwords.
    @Bean
    public DaoAuthenticationProvider daoAuthenticationProvider(DatabaseUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        // Set the custom UserDetailsService that loads users from the DB
        provider.setUserDetailsService(userDetailsService);
        // Set the PasswordEncoder bean so presented passwords are checked
        // using the same encoding strategy as stored passwords
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
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
     * - `.formLogin(form -> form.disable())` → disables Spring Security's default
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
            org.springframework.security.config.annotation.web.builders.HttpSecurity http,
            DaoAuthenticationProvider authProvider) throws Exception {
        http
                // Globally allow all requests (temporary for dev/testing)
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll())
                // Disable CSRF for non-browser API access
                .csrf(csrf -> csrf.disable())
                // Register the DaoAuthenticationProvider so authentication attempts
                // (for example from Basic auth) will use the DB-backed service.
                .authenticationProvider(authProvider)
                // Enable HTTP Basic for Swagger/curl testing; this makes it easy to
                // provide credentials (Authorization header) from the client/UI.
                .httpBasic(org.springframework.security.config.Customizer.withDefaults())
                // Disable form-based login to avoid browser redirect loops to /login
                .formLogin(form -> form.disable())
                // Keep H2 console working in a frame by disabling frameOptions
                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable()));
        // Build and return the configured security chain
        return http.build();
    }

    /**
     * Expose the AuthenticationManager from the configuration so other beans
     * (for example controllers performing programmatic login) can authenticate
     * credentials using the same providers we registered
     * (DaoAuthenticationProvider).
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
