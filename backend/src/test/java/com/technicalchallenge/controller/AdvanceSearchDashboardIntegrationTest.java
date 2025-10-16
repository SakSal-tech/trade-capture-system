package com.technicalchallenge.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * These test verify that the controller endpoints for search,
 * filter, and RSQL queries are correctly exposed, wired through Spring Boot,
 * and returning JSON in the expected structure.
 * 
 * These tests are not designed to re-test service logic (that is already
 * covered in unit tests) but to confirm that HTTP routing, parameter binding,
 * and JSON serialisation work end-to-end.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AdvanceSearchDashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/dashboard/search should return filtered trades when counterparty is provided")
    void testSearchTradesEndpoint() throws Exception {
        // I am testing the /api/dashboard/search endpoint to ensure that
        // it accepts a 'counterparty' parameter and returns a list of trades in JSON
        // format.

        mockMvc.perform(get("/api/dashboard/search")
                .param("counterparty", "BigBank")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // The response should be a JSON array (even if empty)
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                // At least confirm that the JSON array exists
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET /api/dashboard/filter should return paginated results with a 'content' field")
    void testFilterTradesEndpoint() throws Exception {
        // This test checks that the /filter endpoint supports pagination and returns
        // a Page-like JSON object with a 'content' array.

        mockMvc.perform(get("/api/dashboard/filter")
                .param("counterparty", "BigBank")
                .param("page", "0")
                .param("size", "10")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // Expecting response with 'content' as a JSON array
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.count").isNumber());
    }

    @Test
    @DisplayName("GET /api/dashboard/rsql should return trades matching the RSQL query expression")
    void testRsqlEndpoint() throws Exception {
        // I am testing the /rsql endpoint to confirm that it correctly parses
        // and handles RSQL queries passed as the 'query' parameter.

        mockMvc.perform(get("/api/dashboard/rsql")
                .param("query", "counterparty.name==BigBank")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                // The result should be a map with 'content' array
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.count").isNumber());
    }

    @Test
    @DisplayName("GET /api/dashboard/rsql should handle invalid RSQL syntax gracefully")
    void testRsqlEndpointInvalidQuery() throws Exception {
        // This test ensures that the controller responds with a 4xx error
        // if an invalid RSQL query is provided, demonstrating basic input validation.

        mockMvc.perform(get("/api/dashboard/rsql")
                .param("query", "counterparty.name===INVALID") // invalid operator
                .accept(MediaType.APPLICATION_JSON))
                // I expect Spring's exception handling or custom advice to return a client
                // error.
                .andExpect(status().is4xxClientError());
    }

}
