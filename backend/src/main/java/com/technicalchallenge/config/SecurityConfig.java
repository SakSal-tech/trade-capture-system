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

@Configuration
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

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Use DelegatingPasswordEncoder for compatibility, but {noop} for test
        // passwords
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public org.springframework.security.web.SecurityFilterChain securityFilterChain(
            org.springframework.security.config.annotation.web.builders.HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(httpBasic -> httpBasic.disable());
        return http.build();
    }
}
