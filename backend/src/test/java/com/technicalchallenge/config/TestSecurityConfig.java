package com.technicalchallenge.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/*
 * Test-only security configuration.
 *
 * - Integration tests need a predictable auth setup without hitting real user stores.
 * - We provide in-memory users and a SecurityFilterChain that mirrors production-style
 *   constraints (authenticated by default) while remaining test-friendly.
 *
 * Why the bean is renamed:
 * - The main application config (SecurityConfig) also declares a bean named "securityFilterChain".
 * - When tests load the full Spring context, both beans would be present and Spring refuses
 *   duplicate bean names, throwing BeanDefinitionOverrideException.
 * - We fix that by explicitly naming the test bean "testSecurityFilterChain".
 *   This avoids clashing with the app bean named "securityFilterChain".
 *
 * Notes:
 * - @TestConfiguration ensures this class is only considered in test contexts.
 * - Remember If  the 'test' profile and also have GlobalTestSecurityConfig activate
 *   contributing a SecurityFilterChain, make sure its bean name is different too,
 *   or only include one of the two configs in a given test.
 */
@TestConfiguration
@EnableMethodSecurity
public class TestSecurityConfig {

    @Bean
    public UserDetailsService userDetailsService() {
        InMemoryUserDetailsManager manager = new InMemoryUserDetailsManager();
        manager.createUser(User.withUsername("testTrader").password("{noop}pw").roles("TRADER").build());
        manager.createUser(User.withUsername("supportUser").password("{noop}pw").roles("SUPPORT").build());
        manager.createUser(User.withUsername("viewerUser").password("{noop}pw").roles("TRADE_VIEW").build());
        return manager;
    }

    // Bean renamed to avoid clash with the main app's securityFilterChain
    @Bean(name = "testSecurityFilterChain")
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                // Require authentication for meaningful role tests
                .authorizeHttpRequests(authz -> authz.anyRequest().authenticated())
                // Disable CSRF so POST/DELETE in tests never 403
                .csrf(csrf -> csrf.disable())
                // Disable real login mechanisms
                .httpBasic(b -> b.disable())// disables HTTP Basic authentication (the browser popup / Authorization:
                                            // Basic)
                .formLogin(f -> f.disable());// disables the standard form login (the username/password HTML form
                                             // handler).
        return http.build();

        /*
         * Future improvement notes for myself: Remember to use an alternative
         * authentication mechanism (e.g., JWT filters, OAuth2, custom authentication
         * filters) is configured, otherwise the application may become unauthenticated
         * or unreachable
         */
    }
}
