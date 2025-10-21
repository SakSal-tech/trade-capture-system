package com.technicalchallenge.controller;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;

/**
 * BaseIntegrationTest
 *
 * This is a shared base class for all integration tests.
 * 
 * It automatically:
 * - Starts the full Spring Boot application context (@SpringBootTest)
 * - Configures MockMvc for making HTTP requests to controllers
 * - Authenticates every test request as a mock user ("alice") with the TRADER
 * role
 *
 * By extending this class, individual test classes can focus on testing
 * behaviour rather than configuration.
 *
 * If you need to test a different user role (e.g. SUPPORT or SALES),
 * you can override the @WithMockUser annotation on that test class or method.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test") // Ensures the application-test.properties file is used
@WithMockUser(username = "alice", roles = { "TRADER" })
public abstract class BaseIntegrationTest {
    // Common setup code can go here later if needed
}
