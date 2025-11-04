package com.technicalchallenge.controller;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.model.Trade;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@WithAnonymousUser
@ActiveProfiles("test")
public class UserPrivilegeIntegrationTest extends BaseIntegrationTest {

        @Autowired
        private org.springframework.transaction.PlatformTransactionManager txManager;

        // Autowire MockMvc and repositories used by the tests to resolve compile errors
        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private com.technicalchallenge.repository.BookRepository bookRepository;

        @Autowired
        private com.technicalchallenge.repository.CounterpartyRepository counterpartyRepository;

        @Autowired
        private com.technicalchallenge.repository.ApplicationUserRepository applicationUserRepository;

        @Autowired
        private com.technicalchallenge.repository.TradeRepository tradeRepository;

        @Autowired
        private com.technicalchallenge.repository.TradeStatusRepository tradeStatusRepository;

        // Test fixtures referenced from multiple tests
        protected Long savedTradeBusinessId;
        protected com.technicalchallenge.model.Trade trade;

        // Endpoint constant used in tests
        private static final String DAILY_SUMMARY_ENDPOINT = "/api/dashboard/daily-summary?traderId=testTrader";

        @BeforeEach
        void setUp() {
                // Use a dedicated transaction to persist reference data so it's
                // committed and visible to controller requests executed via MockMvc
                // (MockMvc runs requests in a separate transaction). This avoids
                // the classic "uncommitted test-transaction not visible to web
                // layer" problem when BaseIntegrationTest is transactional.
                org.springframework.transaction.support.TransactionTemplate tt = new org.springframework.transaction.support.TransactionTemplate(
                                txManager);
                tt.execute(status -> {
                        // clear repositories before each test to ensure a clean state
                        bookRepository.deleteAll();
                        counterpartyRepository.deleteAll();
                        applicationUserRepository.deleteAll();
                        tradeRepository.deleteAll();

                        // Create minimal Book and Counterparty entries used by Trade
                        var book = new Book();
                        book.setBookName("Book-Test");
                        book = bookRepository.saveAndFlush(book);

                        // Also create a book matching the create-trade test payload.
                        var createBook = new Book();
                        createBook.setBookName("TestBook");
                        createBook = bookRepository.saveAndFlush(createBook);

                        var counterparty = new Counterparty();
                        counterparty.setName("CounterOne");
                        counterparty = counterpartyRepository.saveAndFlush(counterparty);

                        // Create a counterparty the create-trade payload expects
                        var createCounterparty = new Counterparty();
                        createCounterparty.setName("BigBank");
                        createCounterparty = counterpartyRepository.saveAndFlush(createCounterparty);

                        /*
                         * Persist a Trade so tests exercise the full path. Important:
                         * - The application uses a business identifier `trade.tradeId` for
                         * lookups (see TradeService.getTradeById which calls
                         * tradeRepository.findByTradeIdAndActiveTrue). That means controller
                         * endpoints expect the business tradeId in the path (e.g. /api/trades/{id}).
                         * - To avoid 404s set trade.setTradeId(100001L) so controller calls
                         * in the tests resolve the persisted trade.
                         */
                        trade = new Trade();
                        trade.setTradeId(100001L);
                        trade.setVersion(1);
                        trade.setActive(true);
                        trade.setBook(book);
                        trade.setCounterparty(counterparty);
                        trade.setTradeDate(LocalDate.now());
                        trade = tradeRepository.saveAndFlush(trade);
                        savedTradeBusinessId = trade.getTradeId();

                        // Also seed additional users and a second trade used by some tests.
                        var tradeStatus = tradeStatusRepository.findByTradeStatus("NEW").orElseThrow();

                        // ✅ Create new application users for the tests (unique login IDs)
                        String[] extraUsers = {
                                        "testTrader", "supportUser", "viewerUser",
                                        "creatorUser", "editorUser", "deleterUser",
                                        "terminatorUser", "cancellerUser", "bookViewer"
                        };
                        for (String u : extraUsers) {
                                var user = new com.technicalchallenge.model.ApplicationUser();
                                user.setLoginId(u);
                                user.setFirstName(u);
                                user.setActive(true);
                                applicationUserRepository.saveAndFlush(user);
                        }

                        /*
                         * Create one valid Trade for read/patch/delete tests.
                         * This trade uses real reference data seeded via data.sql.
                         */
                        Trade second = new Trade();
                        second.setTradeId(100999L); // unique business ID, not clashing with data.sql
                        second.setVersion(1);
                        second.setActive(true);
                        second.setBook(book);
                        second.setCounterparty(counterparty);
                        second.setTradeDate(LocalDate.now());
                        second.setTradeStatus(tradeStatus);
                        second = tradeRepository.saveAndFlush(second);

                        // expose the last saved business id as test fixture (where used)
                        savedTradeBusinessId = second.getTradeId();

                        return null;
                });
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
                                "  \"tradeDate\": \"2025-10-15\",\n" +
                                "  \"tradeStartDate\": \"2025-10-15\",\n" +
                                "  \"tradeMaturityDate\": \"2026-10-15\",\n" +
                                "  \"tradeLegs\": [\n" +
                                "    {\"legId\":1,\"notional\":2000000,\"currency\":\"USD\",\"legType\":\"FIXED\",\"payReceiveFlag\":\"PAY\",\"rate\":1.5,\"tradeMaturityDate\":\"2026-10-15\"},\n"
                                +
                                "    {\"legId\":2,\"notional\":2000000,\"currency\":\"USD\",\"legType\":\"FIXED\",\"payReceiveFlag\":\"RECEIVE\",\"rate\":1.5,\"tradeMaturityDate\":\"2026-10-15\"}\n"
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
                // When / Then - use the persisted business trade id so hit the
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
                // assert the echoed bookName rather than brittle numeric ids because
                // the application generates or assigns tradeIds and the response id
                // may differ across environments.
                mockMvc.perform(post("/api/trades")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\n" +
                                                "  \"tradeId\": 200002,\n" +
                                                "  \"bookName\": \"TestBook\",\n" +
                                                "  \"counterpartyName\": \"BigBank\",\n" +
                                                "  \"tradeDate\": \"2025-10-15\",\n" +
                                                "  \"startDate\": \"2025-10-15\",\n" +
                                                "  \"maturityDate\": \"2026-10-15\",\n" +
                                                "  \"tradeType\": \"SWAP\",\n" +
                                                "  \"tradeStatus\": \"NEW\",\n" +
                                                "  \"tradeLegs\": [\n" +
                                                "    {\"legId\": 1, \"notional\": 1000000, \"currency\": \"USD\", \"legType\": \"FIXED\", \"payReceiveFlag\": \"PAY\", \"rate\": 1.5, \"tradeMaturityDate\": \"2026-10-15\"},\n"
                                                +
                                                "    {\"legId\": 2, \"notional\": 1000000, \"currency\": \"USD\", \"legType\": \"FIXED\", \"payReceiveFlag\": \"RECEIVE\", \"rate\": 1.5, \"tradeMaturityDate\": \"2026-10-15\"}\n"
                                                +
                                                "  ]\n" +
                                                "}"))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.bookName", is("TestBook"))); // assert created payload echoed
        }

        @DisplayName("SUPPORT role should be denied to create trade")
        /*
         * I updated this test to send a valid TradeDTO payload so the request fails
         * on authorization (403) instead of validation (400). Using a full payload
         * ensures exercise the controller's privilege check.
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
                                                "    {\"legId\":1,\"notional\":1000000,\"currency\":\"USD\",\"tradeMaturityDate\":\"2026-01-01\"},\n"
                                                +
                                                "    {\"legId\":2,\"notional\":1000000,\"currency\":\"USD\",\"tradeMaturityDate\":\"2026-01-01\"}\n"
                                                +
                                                "  ]\n" +
                                                "}"))
                                .andExpect(status().isForbidden());
        }

        @DisplayName("TRADER role should be allowed to patch trade")
        @Test
        @WithMockUser(username = "editorUser", roles = { "TRADER" })
        void testTradeEditRoleAllowedPatch() throws Exception {

                /*
                 * REFACTORED: Added a POST call to create a valid trade before performing
                 * PATCH.
                 * WHY: The previous version assumed a trade with savedTradeBusinessId already
                 * existed.
                 * However, in integration tests the trade may not be seeded or may be inactive,
                 * causing the PATCH to return 404 (not found).
                 * By creating the trade first, the test becomes self-contained and reliable.
                 */
                String createTradeJson = "{\n" +
                                "  \"bookName\": \"TestBook\",\n" +
                                "  \"counterpartyName\": \"BigBank\",\n" +
                                "  \"tradeDate\": \"2025-10-20\",\n" +
                                "  \"startDate\": \"2025-10-20\",\n" +
                                "  \"maturityDate\": \"2026-10-20\",\n" +
                                "  \"tradeType\": \"Spot\",\n" +
                                "  \"tradeSubType\": \"Vanilla\",\n" +
                                "  \"tradeLegs\": [\n" +
                                "    {\"notional\":1000000,\"currency\":\"USD\",\"legType\":\"Fixed\",\"payReceiveFlag\":\"Pay\",\"rate\":0.05,\"tradeMaturityDate\":\"2026-10-20\"},\n"
                                +
                                "    {\"notional\":1000000,\"currency\":\"USD\",\"legType\":\"Floating\",\"payReceiveFlag\":\"Receive\",\"rate\":0.04,\"tradeMaturityDate\":\"2026-10-20\",\"index\":\"LIBOR\"}\n"
                                +
                                "  ]\n" +
                                "}";

                /*
                 * REFACTORED: Create the trade using the same REST endpoint as production.
                 * WHY: This ensures a real trade record exists with a valid tradeId and all
                 * necessary reference data. The status().isCreated() assertion confirms
                 * successful creation before proceeding.
                 */
                String createResponse = mockMvc.perform(post("/api/trades")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createTradeJson))
                                .andExpect(status().isCreated())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                /*
                 * REFACTORED: Parse the tradeId from the creation response dynamically.
                 * WHY: The test no longer depends on a hardcoded savedTradeBusinessId, which
                 * might not exist or might change if data.sql or seed data evolves.
                 */
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
                TradeDTO createdTrade = mapper.readValue(createResponse, TradeDTO.class);
                Long createdTradeId = createdTrade.getTradeId();

                /*
                 * REFACTORED: Use a valid patch body matching the TradeDTO validation rules.
                 * WHY: Ensures validation passes and that the PATCH call actually tests
                 * authorization and amend logic, not input validation errors.
                 */
                String patchJson = "{\n" +
                                "  \"bookName\": \"TestBook\",\n" +
                                "  \"counterpartyName\": \"BigBank\",\n" +
                                "  \"tradeDate\": \"2025-10-21\",\n" +
                                "  \"startDate\": \"2025-10-21\",\n" +
                                "  \"maturityDate\": \"2026-10-21\",\n" +
                                "  \"tradeLegs\": [\n" +
                                "    {\"notional\":2000000,\"currency\":\"USD\",\"legType\":\"Fixed\",\"payReceiveFlag\":\"Pay\",\"rate\":0.06,\"tradeMaturityDate\":\"2026-10-21\"},\n"
                                +
                                "    {\"notional\":2000000,\"currency\":\"USD\",\"legType\":\"Floating\",\"payReceiveFlag\":\"Receive\",\"rate\":0.05,\"tradeMaturityDate\":\"2026-10-21\",\"index\":\"LIBOR\"}\n"
                                +
                                "  ]\n" +
                                "}";

                /*
                 * REFACTORED: PATCH the trade we just created and verify 200 OK.
                 * WHY: The createdTradeId now references an existing, active trade,
                 * so the service's findByTradeIdAndActiveTrue() will succeed, eliminating the
                 * 404.
                 * The .andDo(print()) is kept for debug visibility during test runs.
                 */
                mockMvc.perform(patch("/api/trades/" + createdTradeId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(patchJson))
                                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())// executes
                                                                                                                 // a
                                                                                                                 // ResultHandler
                                                                                                                 // that
                                                                                                                 // writes
                                                                                                                 // the
                                                                                                                 // HTTP
                                                                                                                 // request
                                                                                                                 // and
                                                                                                                 // response
                                                                                                                 // details
                                                                                                                 // (status,
                                                                                                                 // headers,
                                                                                                                 // body,
                                                                                                                 // etc.)
                                                                                                                 // to
                                                                                                                 // the
                                                                                                                 // test
                                                                                                                 // console
                                                                                                                 // /
                                                                                                                 // test
                                                                                                                 // logs
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
                                                {"legId":1,"notional":1000000,"currency":"USD","tradeMaturityDate":"2026-01-01"},
                                                {"legId":2,"notional":1000000,"currency":"USD","tradeMaturityDate":"2026-01-01"}
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
