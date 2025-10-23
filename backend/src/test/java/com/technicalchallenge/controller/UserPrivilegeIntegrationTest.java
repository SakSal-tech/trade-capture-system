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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import org.junit.jupiter.api.BeforeEach;

import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.model.Trade;

import jakarta.transaction.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@WithAnonymousUser
@ActiveProfiles("test")
@Transactional
@Rollback
public class UserPrivilegeIntegrationTest extends BaseIntegrationTest {

        @Autowired
        private MockMvc mockMvc;

        /*
         * This test class was refactored from a unit-style test that used @MockBean. I
         * realised that I was mocking.
         * for service/mapper to a true integration test. The goal is to exercise the
         * full stack: controller -> service -> repository, using real data persisted
         * into the test database. That ensures authorization rules and data mapping
         * are validated end-to-end and makes the test more robust to changes in
         * service logic.
         *
         * Key changes:
         * - Removed mocks for TradeService/TradeMapper so controller calls the real
         * service and the service calls repositories.
         * - Persisted minimal reference data (Book, Counterparty) in @BeforeEach so
         * population methods like populateReferenceDataByName() find expected
         * records during create/update flows.
         * - Persisted a Trade and set its business-level tradeId (100001L). The
         * application looks up trades by trade.tradeId (business id) not the DB
         * generated primary key, so tests use savedTradeBusinessId when calling
         * controller endpoints.
         * - Kept @Transactional + @Rollback so each test runs in isolation and leaves
         * no permanent state in the test database.
         */

        @Autowired
        private com.technicalchallenge.repository.BookRepository bookRepository;

        @Autowired
        private com.technicalchallenge.repository.CounterpartyRepository counterpartyRepository;

        @Autowired
        private com.technicalchallenge.repository.TradeRepository tradeRepository;

        private Trade trade;
        // tradeDTO was removed — tests use persisted entities or inline payloads
        private Long savedTradeBusinessId;

        private static final String DAILY_SUMMARY_ENDPOINT = "/api/dashboard/daily-summary?traderId=testTrader";

        @BeforeEach
        void setUp() {
                // clear repositories before each test to ensure a clean state
                bookRepository.deleteAll();
                counterpartyRepository.deleteAll();
                tradeRepository.deleteAll();
                // Create minimal Book and Counterparty entries used by Trade
                var book = new Book();
                book.setBookName("Book-Test");
                book = bookRepository.save(book);

                // Also created a book matching the create-trade test payload. The
                // create-trade controller uses populateReferenceDataByName() which
                // looks up Book by name; creating this record ensures the create
                // flow finds the Book and avoids `Book not found` runtime errors.
                var createBook = new Book();
                createBook.setBookName("TestBook");
                createBook = bookRepository.save(createBook);

                var counterparty = new Counterparty();
                counterparty.setName("CounterOne");
                counterparty = counterpartyRepository.save(counterparty);

                // Create a counterparty the create-trade payload expects so the
                // controller's populateReferenceDataByName() can resolve it.
                var createCounterparty = new Counterparty();
                createCounterparty.setName("BigBank");
                createCounterparty = counterpartyRepository.save(createCounterparty);

                /*
                 * Persist a Trade so tests exercise the full path. Important:
                 * - The application uses a business identifier `trade.tradeId` for
                 * lookups (see TradeService.getTradeById which calls
                 * tradeRepository.findByTradeIdAndActiveTrue). That means controller
                 * endpoints expect the business tradeId in the path (e.g. /api/trades/{id}).
                 * - To avoid 404s we set trade.setTradeId(100001L) so controller calls
                 * in the tests resolve the persisted trade.
                 */
                trade = new Trade();
                trade.setTradeId(100001L);
                trade.setVersion(1);
                trade.setActive(true);
                trade.setBook(book);
                trade.setCounterparty(counterparty);
                trade.setTradeDate(LocalDate.now());
                trade = tradeRepository.save(trade);
                savedTradeBusinessId = trade.getTradeId();

                // no TradeDTO field — tests create payloads inline when needed
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
                mockMvc.perform(patch("/api/trades/" + savedTradeBusinessId)
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
                // When / Then - use the persisted business trade id so we hit the
                // full controller -> service -> repository path and validate mapping
                // and authorization behaviour end-to-end.
                mockMvc.perform(get("/api/trades/" + savedTradeBusinessId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.tradeId", is(savedTradeBusinessId.intValue())))
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
                // When / Then - send a valid payload to create a trade and expect 201.
                // We assert the echoed bookName rather than brittle numeric ids because
                // the application generates or assigns tradeIds and the response id
                // may differ across environments.
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
                                .andExpect(jsonPath("$.bookName", is("TestBook"))); // assert created payload echoed
                                                                                    // correctly
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
                // Minimal valid patch payload that satisfies DTO validation rules.
                // The PATCH is executed against the business trade id we persisted
                // earlier (savedTradeBusinessId) so the controller finds the trade.
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

                mockMvc.perform(patch("/api/trades/" + savedTradeBusinessId)
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

                // This test intentionally hits a trade id that does not belong to
                // the caller (supportUser) — the controller/service should return 403.
                mockMvc.perform(patch("/api/trades/" + savedTradeBusinessId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validPatchJson)
                                .with(csrf()))
                                .andExpect(status().isForbidden()); // will now reach security filter
        }

        @DisplayName("TRADER role should be allowed to delete trade")
        @Test
        @WithMockUser(username = "deleterUser", roles = { "TRADER" })
        void testTradeDeleteRoleAllowed() throws Exception {
                // Deleting the persisted trade via the controller exercises the
                // deleteTrade path and verifies logical-delete or delete flow.
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/trades/" + savedTradeBusinessId)).andExpect(status().isNoContent());
        }

        @DisplayName("SUPPORT role should be denied to delete trade")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedDeleteTrade() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .delete("/api/trades/" + savedTradeBusinessId)).andExpect(status().isForbidden());
        }

        // Privilege validation for POST /api/trades/{id}/terminate

        @DisplayName("TRADER role should be allowed to terminate trade")
        @Test
        @WithMockUser(username = "terminatorUser", roles = { "TRADER" })
        void testTradeTerminateRoleAllowed() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/trades/" + savedTradeBusinessId + "/terminate")).andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to terminate trade")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedTerminateTrade() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/trades/" + savedTradeBusinessId + "/terminate"))
                                .andExpect(status().isForbidden());
        }

        // Privilege validation for POST /api/trades/{id}/cancel

        @DisplayName("TRADER role should be allowed to cancel trade")
        @Test
        @WithMockUser(username = "cancellerUser", roles = { "TRADER" })
        void testTradeCancelRoleAllowed() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/trades/" + savedTradeBusinessId + "/cancel")).andExpect(status().isOk());
        }

        @DisplayName("SUPPORT role should be denied to cancel trade")
        @Test
        @WithMockUser(username = "supportUser", roles = { "SUPPORT" })
        void testSupportRoleDeniedCancelTrade() throws Exception {
                mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/trades/" + savedTradeBusinessId + "/cancel"))
                                .andExpect(status().isForbidden());
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
