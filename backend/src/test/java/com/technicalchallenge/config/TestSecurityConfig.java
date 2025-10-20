package com.technicalchallenge.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
// import org.springframework.security.crypto.password.PasswordEncoder;
// import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@TestConfiguration
public class TestSecurityConfig {
    // Minimal test-only security so @SpringBootTest contexts have a
    // SecurityFilterChain
    // Allows H2 console frames and ignores CSRF for the console path. Safe for
    // tests only.

    @Bean
    public org.springframework.security.core.userdetails.UserDetailsService userDetailsService() {
        var mgr = new org.springframework.security.provisioning.InMemoryUserDetailsManager();
        mgr.createUser(org.springframework.security.core.userdetails.User
                .withUsername("testuser")
                .password("password")
                .roles("USER")
                .build());
        return mgr;
    }

    @Bean
    public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
        // tests only
        return org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance();
    }

    @Bean
    public org.springframework.security.web.SecurityFilterChain testSecurityFilterChain(
            org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**", "/actuator/**", "/api-docs/**", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html")
                        .permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                .httpBasic(org.springframework.security.config.Customizer.withDefaults())
                .formLogin(org.springframework.security.config.Customizer.withDefaults());

        return http.build();
    }
}
