
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)

@AutoConfigureMockMvc

@ActiveProfiles("test")

@org.springframework.boot.autoconfigure.ImportAutoConfiguration(exclude = {
                org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
                org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
})
public class UserPrivilegeIntegrationTest {

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

        @DisplayName("TRADE_VIEW role should be allowed to access all trades")

        @Test

        @WithMockUser(username = "viewerUser", roles = { "TRADE_VIEW" })
        void testTradeViewRoleAllowed() throws Exception {
                // Here Simulating a user with the TRADE_VIEW role trying to access all
                // trades.
                // I expect this user to be allowed, so the status should be 200 OK.
                mockMvc.perform(get("/api/trades"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied access to all trades")

        @Test

        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedAllTrades() throws Exception {
                // Here Simulating a user with the SUPPORT role trying to access all
                // trades.
                // I expect this user to be denied, so the status should be 403 Forbidden.
                mockMvc.perform(get("/api/trades"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for GET /api/trades/{id}

        @DisplayName("TRADE_VIEW role should be allowed to access trade by id")

        @Test

        @WithMockUser(username = "viewerUser", roles = { "TRADE_VIEW" })
        void testTradeViewRoleAllowedById() throws Exception {
                // Simulating a user with TRADE_VIEW role accessing a specific trade by id
                mockMvc.perform(get("/api/trades/1"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied access to trade by id")

        @Test

        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedTradeById() throws Exception {
                // simulating a user with SUPPORT role accessing a specific trade by id.
                mockMvc.perform(get("/api/trades/1"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for POST /api/trades

        @DisplayName("TRADE_CREATE role should be allowed to create trade")

        @Test

        @WithMockUser(username = "creatorUser", roles = { "TRADE_CREATE" })
        void testTradeCreateRoleAllowed() throws Exception {
                // Simulating a user with TRADE_CREATE role creating a trade.
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/trades")
                                .contentType("application/json")
                                .content("{\"tradeId\":1}"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to create trade")

        @Test

        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedCreateTrade() throws Exception {
                // Simulating a user with SUPPORT role trying to create a trade.
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/trades")
                                .contentType("application/json")
                                .content("{\"tradeId\":1}"))
                                .andExpect(status().isForbidden());
        }

        @DisplayName("TRADE_EDIT role should be allowed to patch trade")
        @Test
        @WithMockUser(username = "editorUser", roles = { "TRADE_EDIT" })
        void testTradeEditRoleAllowedPatch() throws Exception {
                // Simulating a user with TRADE_EDIT role patching a trade.
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
                // Simulating a user with SUPPORT role trying to patch a trade.
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .patch("/api/trades/1")
                                .contentType("application/json")
                                .content("{\"tradeId\":1}"))
                                .andExpect(status().isForbidden());
        }

        @DisplayName("TRADE_DELETE role should be allowed to delete trade")
        @Test
        @WithMockUser(username = "deleterUser", roles = { "TRADE_DELETE" })
        void testTradeDeleteRoleAllowed() throws Exception {
                // Simulating a user with TRADE_DELETE role deleting a trade.
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/trades/1"))
                                .andExpect(status().isNoContent());
        }

        @DisplayName("SUPPORT role should be denied to delete trade")

        @Test

        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedDeleteTrade() throws Exception {
                // Simulating a user with SUPPORT role trying to delete a trade.
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/trades/1"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for POST /api/trades/{id}/terminate

        @DisplayName("TRADE_TERMINATE role should be allowed to terminate trade")

        @Test

        @WithMockUser(username = "terminatorUser", roles = { "TRADE_TERMINATE" })
        void testTradeTerminateRoleAllowed() throws Exception {
                // Simulating a user with TRADE_TERMINATE role terminating a trade.
                mockMvc.perform(
                                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                                "/api/trades/1/terminate"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to terminate trade")

        @Test

        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedTerminateTrade() throws Exception {
                // Simulating a user with SUPPORT role trying to terminate a trade.
                mockMvc.perform(
                                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                                "/api/trades/1/terminate"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for POST /api/trades/{id}/cancel

        @DisplayName("TRADE_CANCEL role should be allowed to cancel trade")

        @Test

        @WithMockUser(username = "cancellerUser", roles = { "TRADE_CANCEL" })
        void testTradeCancelRoleAllowed() throws Exception {
                // Simulating a user with TRADE_CANCEL role cancelling a trade.
                mockMvc.perform(
                                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                                "/api/trades/1/cancel"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to cancel trade")

        @Test

        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedCancelTrade() throws Exception {
                // Simulating a user with SUPPORT role trying to cancel a trade.
                mockMvc.perform(
                                org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                                                "/api/trades/1/cancel"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for GET /api/dashboard/filter

        @DisplayName("TRADE_VIEW role should be allowed to filter trades")

        @Test

        @WithMockUser(username = "viewerUser", roles = { "TRADE_VIEW" })
        void testTradeViewRoleAllowedFilter() throws Exception {
                // Simulating a user with TRADE_VIEW role filtering trades.
                mockMvc.perform(get("/api/dashboard/filter"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to filter trades")

        @Test

        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedFilterTrades() throws Exception {
                // Simulating a user with SUPPORT role trying to filter trades.
                mockMvc.perform(get("/api/dashboard/filter"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for GET /api/dashboard/search

        @DisplayName("TRADE_VIEW role should be allowed to search trades")

        @Test

        @WithMockUser(username = "viewerUser", roles = { "TRADE_VIEW" })
        void testTradeViewRoleAllowedSearch() throws Exception {
                // Simulating a user with TRADE_VIEW role searching trades.
                mockMvc.perform(get("/api/dashboard/search"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to search trades")

        @Test

        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedSearchTrades() throws Exception {
                // Simulating a user with SUPPORT role trying to search trades.
                mockMvc.perform(get("/api/dashboard/search"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for GET /api/dashboard/rsql

        @DisplayName("TRADE_VIEW role should be allowed to search trades with RSQL")

        @Test

        @WithMockUser(username = "viewerUser", roles = { "TRADE_VIEW" })
        void testTradeViewRoleAllowedRsql() throws Exception {
                // Simulating a user with TRADE_VIEW role searching trades with RSQL.
                // I have updated the RSQL query to use the nested attribute book.id, as book
                // is // an entity relationship.
                // This should resolve the type mismatch error and is the correct way to filter
                // by book ID in RSQL.
                mockMvc.perform(get("/api/dashboard/rsql?query=book.id==1"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to search trades with RSQL")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedRsqlTrades() throws Exception {
                // Simulating a user with SUPPORT role trying to search trades with RSQL.
                // I have updated the RSQL query to use the nested attribute book.id, as book is
                // an entity relationship.
                // This should resolve the type mismatch error and is the correct way to filter
                // by book ID in RSQL.
                mockMvc.perform(get("/api/dashboard/rsql?query=book.id==1"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for GET /api/dashboard/my-trades

        @DisplayName("TRADER role should be allowed to get my trades")

        @Test

        @WithMockUser(username = "testTrader", roles = { "TRADER" })
        void testTraderRoleAllowedMyTrades() throws Exception {
                // Simulating a user with TRADER role getting their own trades.
                mockMvc.perform(get("/api/dashboard/my-trades?traderId=testTrader"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to get my trades")

        @Test

        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedMyTrades() throws Exception {
                // Simulating a user with SUPPORT role trying to get my trades.
                mockMvc.perform(get("/api/dashboard/my-trades?traderId=testTrader"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for GET /api/dashboard/book/{bookId}/trades

        @DisplayName("BOOK_VIEW role should be allowed to get trades by book")

        @Test

        @WithMockUser(username = "bookViewer", roles = { "BOOK_VIEW" })
        void testBookViewRoleAllowedGetTradesByBook() throws Exception {
                // Simulating a user with BOOK_VIEW role getting trades by book.
                mockMvc.perform(get("/api/dashboard/book/1/trades"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to get trades by book")

        @Test

        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedGetTradesByBook() throws Exception {
                // Simulating a user with SUPPORT role trying to get trades by book.
                mockMvc.perform(get("/api/dashboard/book/1/trades"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for GET /api/dashboard/summary

        @DisplayName("TRADER role should be allowed to get trade summary")

        @Test

        @WithMockUser(username = "testTrader", roles = { "TRADER" })
        void testTraderRoleAllowedTradeSummary() throws Exception {
                // Simulating a user with TRADER role getting trade summary.
                mockMvc.perform(get("/api/dashboard/summary?traderId=testTrader"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to get trade summary")

        @Test

        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedTradeSummary() throws Exception {
                // Simulating a user with SUPPORT role trying to get trade summary.
                mockMvc.perform(get("/api/dashboard/summary?traderId=testTrader"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for GET /api/dashboard/daily-summary

        @DisplayName("TRADER role should be allowed to get daily summary")

        @Test

        @WithMockUser(username = "testTrader", roles = { "TRADER" })
        void testTraderRoleAllowedDailySummary() throws Exception {
                // Simulating a user with TRADER role getting daily summary.
                mockMvc.perform(get("/api/dashboard/daily-summary?traderId=testTrader"))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to get daily summary")

        @Test

        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedDailySummary() throws Exception {
                // Simulating a user with SUPPORT role trying to get daily summary.
                mockMvc.perform(get("/api/dashboard/daily-summary?traderId=testTrader"))
                                .andExpect(status().isForbidden());
        }
        // Adding this line to force commit

}
