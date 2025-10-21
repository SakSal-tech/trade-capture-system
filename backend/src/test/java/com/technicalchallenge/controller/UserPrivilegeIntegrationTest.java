package com.technicalchallenge.controller;

import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

//This ensures that a servlet environment is created and HttpSecurity can be autowired.
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

//@AutoConfigureMockMvc

// disables several Spring Boot security auto-configuration classes for the test
// application context
// removing those auto-configs makes it easier to run integration tests without
// dealing with default authentication, filters, or OAuth2 behaviour
// to disable default login behaviour or avoid authentication interfering with
// controller tests.
// @org.springframework.boot.autoconfigure.ImportAutoConfiguration(exclude = {
// org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
// org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
// org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
// org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
// })
public class UserPrivilegeIntegrationTest extends BaseIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        private static final String DAILY_SUMMARY_ENDPOINT = "/api/dashboard/daily-summary?traderId=testTrader";

        @DisplayName("TRADER role should be allowed to access daily summary")

        @Test
        // I had to add dependency in the Pom file

        @WithMockUser(username = "testTrader", roles = { "TRADER" })
        void testTraderRoleAllowed() throws Exception {
                mockMvc.perform(get(DAILY_SUMMARY_ENDPOINT))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied access to daily summary")

        @Test

        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDenied() throws Exception {
                mockMvc.perform(get(DAILY_SUMMARY_ENDPOINT))
                                .andExpect(status().isForbidden());
        }

        @DisplayName("Unauthenticated user should receive 401 Unauthorized")

        @Test
        void testUnauthenticatedUserDenied() throws Exception {
                mockMvc.perform(get(DAILY_SUMMARY_ENDPOINT))
                                .andExpect(status().isUnauthorized());
        }

        // Privilege validation for GET /api/trades

        @DisplayName("TRADER role should be allowed to access all trades")

        @Test
        @WithMockUser(username = "viewerUser", roles = { "TRADER" })
        void testTradeViewRoleAllowed() throws Exception {
                // Simulating a user with the TRADER role trying to access all trades.
                // TRADERs are allowed according to TradeController.
                mockMvc.perform(get("/api/trades"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be allowed to access all trades")

        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleAllowedAllTrades() throws Exception {
                // SUPPORT users are now allowed since TradeController permits SUPPORT to view
                // trades.
                mockMvc.perform(get("/api/trades"))
                                .andExpect(status().isOk());
        }

        // Privilege validation for GET /api/trades/{id}

        @DisplayName("TRADER role should be allowed to access trade by id")

        @Test
        @WithMockUser(username = "viewerUser", roles = { "TRADER" })
        void testTradeViewRoleAllowedById() throws Exception {
                // Simulating a user with TRADER role accessing a specific trade by id.
                mockMvc.perform(get("/api/trades/1"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be allowed to access trade by id")

        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleAllowedTradeById() throws Exception {
                // SUPPORT role can view trades by id (allowed in TradeController).
                mockMvc.perform(get("/api/trades/1"))
                                .andExpect(status().isOk());
        }

        // Privilege validation for POST /api/trades

        @DisplayName("TRADER role should be allowed to create trade")

        @Test
        @WithMockUser(username = "creatorUser", roles = { "TRADER" })
        void testTradeCreateRoleAllowed() throws Exception {
                // TRADER can create trade according to TradeController.
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/trades")
                                .contentType("application/json")
                                .content("""
                                                    {
                                                        "tradeId": 1,
                                                        "bookName": "TEST-BOOK-1",
                                                        "counterpartyName": "BigBank",
                                                        "tradeType": "Swap",
                                                        "tradeSubType": "Vanilla",
                                                        "tradeDate": "2024-06-01",
                                                        "tradeStartDate": "2024-06-03",
                                                        "tradeMaturityDate": "2029-06-03",
                                                        "tradeExecutionDate": "2024-06-01",
                                                        "traderName": "Simon King"
                                                    }
                                                """))
                                .andExpect(status().isCreated()); // Expect 201 Created
        }

        @DisplayName("SUPPORT role should be denied to create trade")

        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedCreateTrade() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/trades")
                                .contentType("application/json")
                                .content("{\"tradeId\":1}"))
                                .andExpect(status().isForbidden());
        }

        @DisplayName("TRADER role should be allowed to patch trade")
        @Test
        @WithMockUser(username = "editorUser", roles = { "TRADER" })
        void testTradeEditRoleAllowedPatch() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/trades/1")
                                .contentType("application/json")
                                .content("{\"tradeId\":1}"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to patch trade")

        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedPatchTrade() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/trades/1")
                                .contentType("application/json")
                                .content("{\"tradeId\":1}"))
                                .andExpect(status().isForbidden());
        }

        @DisplayName("TRADER role should be allowed to delete trade")
        @Test
        @WithMockUser(username = "deleterUser", roles = { "TRADER" })
        void testTradeDeleteRoleAllowed() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/trades/1"))
                                .andExpect(status().isNoContent());
        }

        @DisplayName("SUPPORT role should be denied to delete trade")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedDeleteTrade() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/trades/1"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for POST /api/trades/{id}/terminate

        @DisplayName("TRADER role should be allowed to terminate trade")
        @Test
        @WithMockUser(username = "terminatorUser", roles = { "TRADER" })
        void testTradeTerminateRoleAllowed() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/trades/1/terminate"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to terminate trade")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedTerminateTrade() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/trades/1/terminate"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for POST /api/trades/{id}/cancel

        @DisplayName("TRADER role should be allowed to cancel trade")
        @Test
        @WithMockUser(username = "cancellerUser", roles = { "TRADER" })
        void testTradeCancelRoleAllowed() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/trades/1/cancel"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to cancel trade")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedCancelTrade() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                "/api/trades/1/cancel"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for GET /api/dashboard/filter

        @DisplayName("TRADER role should be allowed to filter trades")
        @Test
        @WithMockUser(username = "viewerUser", roles = { "TRADER" })
        void testTradeViewRoleAllowedFilter() throws Exception {
                mockMvc.perform(get("/api/dashboard/filter"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be allowed to filter trades")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleAllowedFilterTrades() throws Exception {
                // SUPPORT now allowed as per controller rule.
                mockMvc.perform(get("/api/dashboard/filter"))
                                .andExpect(status().isOk());
        }

        // Privilege validation for GET /api/dashboard/search

        @DisplayName("TRADER role should be allowed to search trades")
        @Test
        @WithMockUser(username = "viewerUser", roles = { "TRADER" })
        void testTradeViewRoleAllowedSearch() throws Exception {
                mockMvc.perform(get("/api/dashboard/search"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be allowed to search trades")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleAllowedSearchTrades() throws Exception {
                mockMvc.perform(get("/api/dashboard/search"))
                                .andExpect(status().isOk());
        }

        // Privilege validation for GET /api/dashboard/rsql

        @DisplayName("TRADER role should be allowed to search trades with RSQL")
        @Test
        @WithMockUser(username = "viewerUser", roles = { "TRADER" })
        void testTradeViewRoleAllowedRsql() throws Exception {
                mockMvc.perform(get("/api/dashboard/rsql?query=book.id==1"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be allowed to search trades with RSQL")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleAllowedRsqlTrades() throws Exception {
                mockMvc.perform(get("/api/dashboard/rsql?query=book.id==1"))
                                .andExpect(status().isOk());
        }

        // Privilege validation for GET /api/dashboard/my-trades

        @DisplayName("TRADER role should be allowed to get my trades")
        @Test
        @WithMockUser(username = "testTrader", roles = { "TRADER" })
        void testTraderRoleAllowedMyTrades() throws Exception {
                mockMvc.perform(get("/api/dashboard/my-trades?traderId=testTrader"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to get my trades")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedMyTrades() throws Exception {
                mockMvc.perform(get("/api/dashboard/my-trades?traderId=testTrader"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for GET /api/dashboard/book/{bookId}/trades

        @DisplayName("TRADER role should be allowed to get trades by book")
        @Test
        @WithMockUser(username = "bookViewer", roles = { "TRADER" })
        void testBookViewRoleAllowedGetTradesByBook() throws Exception {
                mockMvc.perform(get("/api/dashboard/book/1/trades"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be allowed to get trades by book")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleAllowedGetTradesByBook() throws Exception {
                mockMvc.perform(get("/api/dashboard/book/1/trades"))
                                .andExpect(status().isOk());
        }

        // Privilege validation for GET /api/dashboard/summary

        @DisplayName("TRADER role should be allowed to get trade summary")
        @Test
        @WithMockUser(username = "testTrader", roles = { "TRADER" })
        void testTraderRoleAllowedTradeSummary() throws Exception {
                mockMvc.perform(get("/api/dashboard/summary?traderId=testTrader"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to get trade summary")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedTradeSummary() throws Exception {
                mockMvc.perform(get("/api/dashboard/summary?traderId=testTrader"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for GET /api/dashboard/daily-summary

        @DisplayName("TRADER role should be allowed to get daily summary")
        @Test
        @WithMockUser(username = "testTrader", roles = { "TRADER" })
        void testTraderRoleAllowedDailySummary() throws Exception {
                mockMvc.perform(get("/api/dashboard/daily-summary?traderId=testTrader"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to get daily summary")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedDailySummary() throws Exception {
                mockMvc.perform(get("/api/dashboard/daily-summary?traderId=testTrader"))
                                .andExpect(status().isForbidden());
        }
}
