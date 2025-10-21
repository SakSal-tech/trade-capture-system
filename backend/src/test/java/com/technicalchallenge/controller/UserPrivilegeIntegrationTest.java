package com.technicalchallenge.controller;

import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.model.Trade;

import jakarta.transaction.Transactional;

import com.technicalchallenge.dto.TradeDTO;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@WithAnonymousUser
@ActiveProfiles("test")
@Transactional
@Rollback
public class UserPrivilegeIntegrationTest extends BaseIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private com.technicalchallenge.service.TradeService tradeService;

        @MockBean
        private com.technicalchallenge.mapper.TradeMapper tradeMapper;

        @Autowired
        private com.technicalchallenge.repository.BookRepository bookRepository;

        @Autowired
        private com.technicalchallenge.repository.CounterpartyRepository counterpartyRepository;

        @Autowired
        private com.technicalchallenge.repository.TradeRepository tradeRepository;

        private Trade trade;
        private TradeDTO tradeDTO;

        private static final String DAILY_SUMMARY_ENDPOINT = "/api/dashboard/daily-summary?traderId=testTrader";

        @BeforeEach
        void setUp() {
                // clear repositories before each test to ensure a clean state
                bookRepository.deleteAll();
                counterpartyRepository.deleteAll();
                tradeRepository.deleteAll();

                trade = new Trade();
                tradeDTO = new TradeDTO();
        }

        @DisplayName("TRADER role should be allowed to access daily summary")

        @Test
        // I had to add dependency in the Pom file

        @WithMockUser(username = "testTrader", roles = { "TRADER" })
        void testTraderRoleAllowed() throws Exception {
                mockMvc.perform(get(DAILY_SUMMARY_ENDPOINT)).andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to patch trade")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedPatchTrade() throws Exception {
                // Changed payload to include required fields so validation passes
                String validPatchJson = "{\n" +
                                "  \"bookName\": \"UpdatedBook\",\n" +
                                "  \"counterpartyName\": \"BigBank\",\n" +
                                "  \"tradeDate\": \"2025-02-01\",\n" +
                                "  \"tradeLegs\": [\n" +
                                "    {\"legId\":1,\"notional\":2000000,\"currency\":\"USD\",\"startDate\":\"2025-02-01\",\"endDate\":\"2026-02-01\"},\n"
                                +
                                "    {\"legId\":2,\"notional\":2000000,\"currency\":\"USD\",\"startDate\":\"2025-02-01\",\"endDate\":\"2026-02-01\"}\n"
                                +
                                "  ]\n" +
                                "}";

                // Added .with(csrf()) to satisfy Spring Security for write operations
                mockMvc.perform(patch("/api/trades/1")
                                .contentType("application/json")
                                .content(validPatchJson)
                                .with(csrf()))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for GET /api/trades

        @DisplayName("TRADER role should be allowed to access all trades")

        @Test
        @WithMockUser(username = "viewerUser", roles = { "TRADER" })
        void testTradeViewRoleAllowed() throws Exception {
                // Simulating a user with the TRADER role trying to access all trades.
                // TRADERs are allowed according to TradeController.
                mockMvc.perform(get("/api/trades")).andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be allowed to access all trades")

        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleAllowedAllTrades() throws Exception {
                // SUPPORT users are now allowed since TradeController permits SUPPORT to view
                // trades.
                mockMvc.perform(get("/api/trades")).andExpect(status().isOk());
        }

        @DisplayName("TRADER role should be allowed to access trade by id")
        @Test
        @WithMockUser(username = "viewerUser", roles = { "TRADER", "TRADE_VIEW" })
        void testTradeViewRoleAllowedById() throws Exception {
                // Given - a mock Trade and DTO returned by the service/mapper
                Trade trade = new Trade();
                trade.setId(1L);
                trade.setVersion(1);
                trade.setActive(true);

                TradeDTO tradeDTO = new TradeDTO();
                tradeDTO.setTradeId(1L);
                tradeDTO.setBookName("Book-Test");
                tradeDTO.setCounterpartyName("CounterOne");
                tradeDTO.setTradeDate(LocalDate.now());
                // Mock expected service + mapper behavior
                when(tradeService.getTradeById(1L)).thenReturn(Optional.of(trade));// Use Optional.of(...) only when the
                                                                                   // value is non-null;
                when(tradeMapper.toDto(trade)).thenReturn(tradeDTO);
                when(tradeMapper.toDto(trade)).thenReturn(tradeDTO);
                // Ensure it's visible to the controller transaction
                // tradeRepository.flush();

                // When / Then
                mockMvc.perform(get("/api/trades/1"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.tradeId", is(1)))
                                .andExpect(jsonPath("$.bookName", is("Book-Test")))
                                .andExpect(jsonPath("$.counterpartyName", is("CounterOne")));
        }

        // Privilege validation for POST /api/trades

        /*
         * I updated this test to use a valid TradeDTO payload (added tradeDate,
         * bookName,
         * counterpartyName and two tradeLegs) and changed the create tradeId to 200001
         * to avoid colliding with seeded test data. This prevents validation 400 errors
         * so the test can exercise authorization behaviour.
         */
        /*
         * Refactore: test were sending too-small or wrong-field JSON -> DTO validation
         * failed -> HTTP 400.
         * Supplying the required fields (tradeDate, bookName, counterpartyName) and
         * correct property name tradeLegs moves the request past validation so the test
         * can check authorization and service behavior.
         */
        @WithMockUser(username = "creatorUser", roles = { "TRADER", "TRADE_CREATE" })

        @DisplayName("TRADER role should be allowed to create trade")
        @Test
        void testTradeCreateRoleAllowed() throws Exception {
                // Given - ensure the mapper/service mocks return the expected objects
                Trade trade = new Trade();
                TradeDTO tradeDTO = new TradeDTO();
                tradeDTO.setTradeId(200001L); // added: ensures response JSON includes tradeId

                when(tradeMapper.toEntity(any(TradeDTO.class))).thenReturn(trade);
                when(tradeService.saveTrade(any(Trade.class), any(TradeDTO.class))).thenReturn(trade);
                when(tradeMapper.toDto(any(Trade.class))).thenReturn(tradeDTO); // mock now returns tradeId

                // When / Then - send a valid payload
                mockMvc.perform(post("/api/trades")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\n" +
                                                "  \"tradeId\": 200001,\n" +
                                                "  \"bookName\": \"TestBook\",\n" +
                                                "  \"counterpartyName\": \"BigBank\",\n" +
                                                "  \"tradeDate\": \"2025-01-01\",\n" +
                                                "  \"tradeType\": \"SWAP\",\n" +
                                                "  \"tradeStatus\": \"NEW\",\n" +
                                                "  \"tradeLegs\": [\n" +
                                                "    {\"legId\": 1, \"notional\": 1000000, \"currency\": \"USD\", \"startDate\": \"2025-01-01\", \"endDate\": \"2026-01-01\"},\n"
                                                +
                                                "    {\"legId\": 2, \"notional\": 1000000, \"currency\": \"USD\", \"startDate\": \"2025-01-01\", \"endDate\": \"2026-01-01\"}\n"
                                                +
                                                "  ]\n" +
                                                "}"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.tradeId", is(200001))); // now matches
        }

        @DisplayName("SUPPORT role should be denied to create trade")
        /*
         * I updated this test to send a valid TradeDTO payload so the request fails
         * on authorization (403) instead of validation (400). Using a full payload
         * ensures we exercise the controller's privilege check.
         */
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedCreateTrade() throws Exception {
                mockMvc.perform(post("/api/trades")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\n" +
                                                "  \"tradeId\": 200002,\n" +
                                                "  \"bookName\": \"TestBook\",\n" +
                                                "  \"counterpartyName\": \"BigBank\",\n" +
                                                "  \"tradeDate\": \"2025-01-01\",\n" +
                                                "  \"tradeType\": \"SWAP\",\n" +
                                                "  \"tradeStatus\": \"NEW\",\n" +
                                                "  \"tradeLegs\": [\n" +
                                                "    {\"legId\":1,\"notional\":1000000,\"currency\":\"USD\",\"startDate\":\"2025-01-01\",\"endDate\":\"2026-01-01\"},\n"
                                                +
                                                "    {\"legId\":2,\"notional\":1000000,\"currency\":\"USD\",\"startDate\":\"2025-01-01\",\"endDate\":\"2026-01-01\"}\n"
                                                +
                                                "  ]\n" +
                                                "}"))
                                .andExpect(status().isForbidden());
        }

        @DisplayName("TRADER role should be allowed to patch trade")
        /*
         * Small fix: send a valid TradeDTO-like patch body so validation passes
         * and the test exercises authorization and amend logic rather than failing
         * with 400 Bad Request.
         */
        @Test
        @WithMockUser(username = "editorUser", roles = { "TRADER" })
        void testTradeEditRoleAllowedPatch() throws Exception {
                // Minimal valid patch payload that satisfies DTO validation rules
                String patchJson = "{\n" +
                                "  \"bookName\": \"UpdatedBook\",\n" +
                                "  \"counterpartyName\": \"BigBank\",\n" +
                                "  \"tradeDate\": \"2025-02-01\",\n" +
                                "  \"tradeLegs\": [\n" +
                                "    {\"legId\":1,\"notional\":2000000,\"currency\":\"USD\",\"startDate\":\"2025-02-01\",\"endDate\":\"2026-02-01\"},\n"
                                +
                                "    {\"legId\":2,\"notional\":2000000,\"currency\":\"USD\",\"startDate\":\"2025-02-01\",\"endDate\":\"2026-02-01\"}\n"
                                +
                                "  ]\n" +
                                "}";

                mockMvc.perform(patch("/api/trades/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(patchJson))
                                .andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to patch trade")

        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedPatchTrade_Simple() throws Exception {
                String validPatchJson = """
                                {
                                  "bookName": "ValidBook",
                                  "counterpartyName": "ValidCounterparty",
                                  "tradeDate": "2025-01-01",
                                  "tradeLegs": [
                                    {"legId":1,"notional":1000000,"currency":"USD","startDate":"2025-01-01","endDate":"2026-01-01"},
                                    {"legId":2,"notional":1000000,"currency":"USD","startDate":"2025-01-01","endDate":"2026-01-01"}
                                  ]
                                }
                                """;

                mockMvc.perform(patch("/api/trades/1")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validPatchJson)
                                .with(csrf()))
                                .andExpect(status().isForbidden()); // will now reach security filter
        }

        @DisplayName("TRADER role should be allowed to delete trade")
        @Test
        @WithMockUser(username = "deleterUser", roles = { "TRADER" })
        void testTradeDeleteRoleAllowed() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/trades/1")).andExpect(status().isNoContent());
        }

        @DisplayName("SUPPORT role should be denied to delete trade")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedDeleteTrade() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/trades/1")).andExpect(status().isForbidden());
        }

        // Privilege validation for POST /api/trades/{id}/terminate

        @DisplayName("TRADER role should be allowed to terminate trade")
        @Test
        @WithMockUser(username = "terminatorUser", roles = { "TRADER" })
        void testTradeTerminateRoleAllowed() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/trades/1/terminate")).andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to terminate trade")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedTerminateTrade() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/trades/1/terminate")).andExpect(status().isForbidden());
        }

        // Privilege validation for POST /api/trades/{id}/cancel

        @DisplayName("TRADER role should be allowed to cancel trade")
        @Test
        @WithMockUser(username = "cancellerUser", roles = { "TRADER" })
        void testTradeCancelRoleAllowed() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/trades/1/cancel")).andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to cancel trade")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedCancelTrade() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/trades/1/cancel")).andExpect(status().isForbidden());
        }

        // Privilege validation for GET /api/dashboard/filter

        @DisplayName("TRADER role should be allowed to filter trades")
        @Test
        @WithMockUser(username = "viewerUser", roles = { "TRADER" })
        void testTradeViewRoleAllowedFilter() throws Exception {
                mockMvc.perform(get("/api/dashboard/filter")).andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be allowed to filter trades")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleAllowedFilterTrades() throws Exception {
                // SUPPORT now allowed as per controller rule.
                mockMvc.perform(get("/api/dashboard/filter")).andExpect(status().isOk());
        }

        // Privilege validation for GET /api/dashboard/search

        @DisplayName("TRADER role should be allowed to search trades")
        @Test
        @WithMockUser(username = "viewerUser", roles = { "TRADER" })
        void testTradeViewRoleAllowedSearch() throws Exception {
                mockMvc.perform(get("/api/dashboard/search")).andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be allowed to search trades")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleAllowedSearchTrades() throws Exception {
                mockMvc.perform(get("/api/dashboard/search")).andExpect(status().isOk());
        }

        // Privilege validation for GET /api/dashboard/rsql

        @DisplayName("TRADER role should be allowed to search trades with RSQL")
        @Test
        @WithMockUser(username = "viewerUser", roles = { "TRADER" })
        void testTradeViewRoleAllowedRsql() throws Exception {
                mockMvc.perform(get("/api/dashboard/rsql?query=book.id==1")).andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be allowed to search trades with RSQL")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleAllowedRsqlTrades() throws Exception {
                mockMvc.perform(get("/api/dashboard/rsql?query=book.id==1")).andExpect(status().isOk());
        }

        // Privilege validation for GET /api/dashboard/my-trades

        @DisplayName("TRADER role should be allowed to get my trades")
        @Test
        @WithMockUser(username = "testTrader", roles = { "TRADER" })
        void testTraderRoleAllowedMyTrades() throws Exception {
                mockMvc.perform(get("/api/dashboard/my-trades?traderId=testTrader")).andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to get my trades")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedMyTrades() throws Exception {
                mockMvc.perform(get("/api/dashboard/my-trades?traderId=testTrader")).andExpect(status().isForbidden());
        }

        // Privilege validation for GET /api/dashboard/book/{bookId}/trades

        @DisplayName("TRADER role should be allowed to get trades by book")
        @Test
        @WithMockUser(username = "bookViewer", roles = { "TRADER" })
        void testBookViewRoleAllowedGetTradesByBook() throws Exception {
                mockMvc.perform(get("/api/dashboard/book/1/trades")).andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be allowed to get trades by book")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleAllowedGetTradesByBook() throws Exception {
                mockMvc.perform(get("/api/dashboard/book/1/trades")).andExpect(status().isOk());
        }

        // Privilege validation for GET /api/dashboard/summary

        @DisplayName("TRADER role should be allowed to get trade summary")
        @Test
        @WithMockUser(username = "testTrader", roles = { "TRADER" })
        void testTraderRoleAllowedTradeSummary() throws Exception {
                mockMvc.perform(get("/api/dashboard/summary?traderId=testTrader")).andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to get trade summary")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedTradeSummary() throws Exception {
                mockMvc.perform(get("/api/dashboard/summary?traderId=testTrader")).andExpect(status().isForbidden());
        }

        // Privilege validation for GET /api/dashboard/daily-summary

        @DisplayName("TRADER role should be allowed to get daily summary")
        @Test
        @WithMockUser(username = "testTrader", roles = { "TRADER" })
        void testTraderRoleAllowedDailySummary() throws Exception {
                mockMvc.perform(get("/api/dashboard/daily-summary?traderId=testTrader")).andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to get daily summary")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedDailySummary() throws Exception {
                mockMvc.perform(get("/api/dashboard/daily-summary?traderId=testTrader"))
                                .andExpect(status().isForbidden());
        }
}
