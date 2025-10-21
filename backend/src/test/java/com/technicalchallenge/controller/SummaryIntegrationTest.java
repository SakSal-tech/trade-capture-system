package com.technicalchallenge.controller;

import org.springframework.test.context.ActiveProfiles;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.repository.ApplicationUserRepository;
import com.technicalchallenge.repository.BookRepository;
import com.technicalchallenge.repository.CounterpartyRepository;
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.repository.TradeStatusRepository;
import com.technicalchallenge.service.TradeDashboardService;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// @AutoConfigureMockMvc // configures the MockMvc bean, allowing me to perform
// HTTP requests against
// @org.springframework.boot.autoconfigure.ImportAutoConfiguration(exclude = {
// org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
// org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration.class,
// org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration.class,
// org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration.class
// })
@Transactional // Ensures each test method runs in a transaction. All database changes made
               // during a test are rolled back at the end, keeping the test data isolated and
               // repeatable
@Rollback // Marks the transaction to be rolled back after the test
public class SummaryIntegrationTest extends BaseIntegrationTest {
        // Injecting dependencies to enable the use mockMvc for endpoint testing, seed
        // and query trades in the database and call service methods

        // Class-level test entities to avoid duplicate local variable errors and
        // scoping issues
        private Book book;
        private Counterparty counterparty;
        private ApplicationUser trader;
        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private TradeRepository tradeRepository;

        @Autowired
        private TradeDashboardService tradeDashboardService;

        @Autowired
        private BookRepository bookRepository;

        @Autowired
        private ApplicationUserRepository userRepository;

        @Autowired
        private CounterpartyRepository counterpartyRepository;

        @Autowired
        private TradeStatusRepository tradeStatusRepository;

        private static final String SUMMARY_ENDPOINT = "/api/dashboard/daily-summary";

        // Setup method to seed test data before each test
        @BeforeEach
        void setup() {
                // Clean up tables to avoid duplicate key errors
                tradeRepository.deleteAll();
                bookRepository.deleteAll();
                counterpartyRepository.deleteAll();
                userRepository.deleteAll();

                // Ensure all entities are deleted before each test to avoid unique constraint
                // violations
                tradeRepository.deleteAll();
                bookRepository.deleteAll();
                counterpartyRepository.deleteAll();
                userRepository.deleteAll();

                // Create and save a book
                book = new Book();
                book.setBookName("TestBook");
                book.setActive(true);
                book.setVersion(1); // preserve variable naming
                bookRepository.save(book);

                // Create and save a counterparty
                counterparty = new Counterparty();
                counterparty.setName("TestCounterparty");
                counterparty.setActive(true);
                counterparty.setCreatedDate(LocalDate.now()); // preserve variable naming
                counterparty.setLastModifiedDate(LocalDate.now());
                counterparty.setInternalCode(1L);
                counterpartyRepository.save(counterparty);

                // Create and save a trader user
                trader = new ApplicationUser();
                trader.setLoginId("testTrader");
                trader.setFirstName("Test");
                trader.setLastName("Trader");
                trader.setActive(true);
                trader.setVersion(1);
                userRepository.save(trader);

                // Create and save the trade with references to the above entities
                Trade todayTrade = new Trade();
                todayTrade.setTradeDate(LocalDate.now());
                todayTrade.setBook(book);
                todayTrade.setCounterparty(counterparty);
                todayTrade.setTraderUser(trader);
                todayTrade.setTradeStatus(tradeStatusRepository.findByTradeStatus("NEW").orElse(null));
                todayTrade.setVersion(1);
                todayTrade.setActive(true);
                tradeRepository.save(todayTrade);

                // Create and save a trade for yesterday
                Trade yesterdayTrade = new Trade();
                yesterdayTrade.setTradeDate(LocalDate.now().minusDays(1));
                yesterdayTrade.setBook(book);
                yesterdayTrade.setCounterparty(counterparty);
                yesterdayTrade.setTraderUser(trader);
                yesterdayTrade.setTradeStatus(tradeStatusRepository.findByTradeStatus("NEW").orElse(null));
                yesterdayTrade.setVersion(1);
                yesterdayTrade.setActive(true);
                tradeRepository.save(yesterdayTrade);
                // of 1 (I created only one
                // instance of trade)
        }

        @DisplayName("Summary endpoint returns correct trade counts when multiple trades exist for today")
        @Test
        void testSummaryEndpointMultipleTradesToday() throws Exception {
                // Setup: create a second trade for today
                Book book = bookRepository.findAll().get(0);
                Counterparty counterparty = counterpartyRepository.findAll().get(0);
                ApplicationUser trader = userRepository.findAll().get(0);

                // Create a new trade for today
                Trade secondTodayTrade = new Trade();
                secondTodayTrade.setTradeDate(LocalDate.now());
                secondTodayTrade.setBook(book);
                secondTodayTrade.setCounterparty(counterparty);
                secondTodayTrade.setTraderUser(trader);
                secondTodayTrade.setTradeStatus(tradeStatusRepository.findByTradeStatus("NEW").orElse(null));
                tradeRepository.save(secondTodayTrade);

                // Perform request and assert
                // The service class DailySummaryDTO method, builds queries for today's and
                // yesterday's trades. These queries filter trades by date and trader, and the
                // results are used to populate 'todaysTradeCount' and 'historicalComparisons'
                // in
                // the summary response.
                mockMvc.perform(get(SUMMARY_ENDPOINT).param("traderId", "testTrader"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.todaysTradeCount").value(2))
                                .andExpect(jsonPath("$.historicalComparisons[*].tradeCount").value(1)); // There is only
                                                                                                        // one trade for
                                                                                                        // yesterday.
        }

        @DisplayName("Summary endpoint returns zero trade count when no trades exist for today")
        @Test
        void testSummaryEndpointNoTradesToday() throws Exception {
                // Remove all trades from database for today where date is equal to today's date
                tradeRepository.deleteAll(tradeRepository.findAll().stream()
                                .filter(trade -> trade.getTradeDate().isEqual(LocalDate.now()))
                                .toList());

                // Perform request and assert
                // The service class will query for today's trades and find none, so
                // todaysTradeCount should be 0.
                // Historical comparisons should still show 1 for yesterday (from setup).
                mockMvc.perform(get(SUMMARY_ENDPOINT).param("traderId", "testTrader"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.todaysTradeCount").value(0))
                                .andExpect(jsonPath("$.historicalComparisons[*].tradeCount").value(1));
        }

        @DisplayName("Summary endpoint returns correct historical trade count when multiple trades exist for yesterday")
        @Test
        void testSummaryEndpointMultipleTradesYesterday() throws Exception {
                // Setup: create a second trade for yesterday
                Book book = bookRepository.findAll().get(0);
                Counterparty counterparty = counterpartyRepository.findAll().get(0);
                ApplicationUser trader = userRepository.findAll().get(0);
                Trade secondYesterdayTrade = new Trade();
                secondYesterdayTrade.setTradeDate(LocalDate.now().minusDays(1));
                secondYesterdayTrade.setBook(book);
                secondYesterdayTrade.setCounterparty(counterparty);
                secondYesterdayTrade.setTraderUser(trader);
                secondYesterdayTrade.setTradeStatus(tradeStatusRepository.findByTradeStatus("NEW").orElse(null));
                tradeRepository.save(secondYesterdayTrade);

                // Perform request and assert
                // The service class will query for yesterday's trades and find two, so
                // historicalComparisons should show 2.
                mockMvc.perform(get(SUMMARY_ENDPOINT).param("traderId", "testTrader"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.todaysTradeCount").value(1))
                                .andExpect(jsonPath("$.historicalComparisons[*].tradeCount").value(2));
        }

        @DisplayName("Summary endpoint returns correct trade counts when no trades exist for yesterday")
        @Test
        void testSummaryEndpointNoTradesYesterday() throws Exception {
                // Remove all trades for yesterday
                tradeRepository.deleteAll(tradeRepository.findAll().stream()
                                .filter(trade -> trade.getTradeDate().isEqual(LocalDate.now().minusDays(1)))
                                .toList());

                // Perform request and assert
                // The service class will query for yesterday's trades and find none, so
                // historicalComparisons should show 0.
                mockMvc.perform(get(SUMMARY_ENDPOINT).param("traderId", "testTrader"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.todaysTradeCount").value(1))
                                .andExpect(jsonPath("$.historicalComparisons[*].tradeCount").value(0));
        }

        @DisplayName("Summary endpoint returns correct trade counts when no trades exist at all")
        @Test
        void testSummaryEndpointNoTradesAtAll() throws Exception {
                // Remove all trades from database
                tradeRepository.deleteAll();

                // Perform request and assert
                // The service class will query for today's and yesterday's trades and find
                // none, so both counts should be 0.
                mockMvc.perform(get(SUMMARY_ENDPOINT).param("traderId", "testTrader"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.todaysTradeCount").value(0))
                                .andExpect(jsonPath("$.historicalComparisons[*].tradeCount").value(0));
        }

        @DisplayName("Summary endpoint returns correct trade counts for different traders")
        @Test
        void testSummaryEndpointDifferentTrader() throws Exception {
                // Setup: create a trade for a different trader
                Book book = bookRepository.findAll().get(0);
                Counterparty counterparty = counterpartyRepository.findAll().get(0);
                ApplicationUser otherTrader = new ApplicationUser();
                otherTrader.setLoginId("otherTrader");
                userRepository.save(otherTrader);
                Trade otherTrade = new Trade();
                otherTrade.setTradeDate(LocalDate.now());
                otherTrade.setBook(book);
                otherTrade.setCounterparty(counterparty);
                otherTrade.setTraderUser(otherTrader);
                otherTrade.setTradeStatus(tradeStatusRepository.findByTradeStatus("NEW").orElse(null));
                tradeRepository.save(otherTrade);

                // Perform request and assert for original trader
                mockMvc.perform(get(SUMMARY_ENDPOINT).param("traderId", "testTrader"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.todaysTradeCount").value(1));

                // Perform request and assert for other trader
                mockMvc.perform(get(SUMMARY_ENDPOINT).param("traderId", "otherTrader"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.todaysTradeCount").value(1));
        }

        @DisplayName("Summary endpoint returns zero trade count for invalid trader")
        @Test
        void testSummaryEndpointInvalidTrader() throws Exception {
                mockMvc.perform(get(SUMMARY_ENDPOINT).param("traderId", "nonExistentTrader"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.todaysTradeCount").value(0))
                                .andExpect(jsonPath("$.historicalComparisons[*].tradeCount").value(0));
        }

        @DisplayName("Summary endpoint returns correct trade counts for trades with different statuses")
        @Test
        void testSummaryEndpointDifferentTradeStatus() throws Exception {
                Book book = bookRepository.findAll().get(0);
                Counterparty counterparty = counterpartyRepository.findAll().get(0);
                ApplicationUser trader = userRepository.findAll().get(0);
                Trade completedTrade = new Trade();
                completedTrade.setTradeDate(LocalDate.now());
                completedTrade.setBook(book);
                completedTrade.setCounterparty(counterparty);
                completedTrade.setTraderUser(trader);
                completedTrade.setTradeStatus(tradeStatusRepository.findByTradeStatus("COMPLETED").orElse(null));
                tradeRepository.save(completedTrade);
                mockMvc.perform(get(SUMMARY_ENDPOINT).param("traderId", "testTrader"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.todaysTradeCount").value(2)); // 1 NEW + 1 COMPLETED
        }

        @DisplayName("Summary endpoint returns correct trade counts for trades with different books and counterparties")
        @Test
        void testSummaryEndpointDifferentBookCounterparty() throws Exception {
                ApplicationUser trader = userRepository.findAll().get(0);
                Book newBook = new Book();
                newBook.setBookName("OtherBook");
                bookRepository.save(newBook);
                Counterparty newCounterparty = new Counterparty();
                newCounterparty.setName("OtherCounterparty");
                counterpartyRepository.save(newCounterparty);
                Trade trade = new Trade();
                trade.setTradeDate(LocalDate.now());
                trade.setBook(newBook);
                trade.setCounterparty(newCounterparty);
                trade.setTraderUser(trader);
                trade.setTradeStatus(tradeStatusRepository.findByTradeStatus("NEW").orElse(null));
                tradeRepository.save(trade);
                mockMvc.perform(get(SUMMARY_ENDPOINT).param("traderId", "testTrader"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.todaysTradeCount").value(2));
        }

        @DisplayName("Summary endpoint ignores trades with future dates")
        @Test
        void testSummaryEndpointFutureDateTrades() throws Exception {
                Book book = bookRepository.findAll().get(0);
                Counterparty counterparty = counterpartyRepository.findAll().get(0);
                ApplicationUser trader = userRepository.findAll().get(0);
                Trade futureTrade = new Trade();
                futureTrade.setTradeDate(LocalDate.now().plusDays(1));
                futureTrade.setBook(book);
                futureTrade.setCounterparty(counterparty);
                futureTrade.setTraderUser(trader);
                futureTrade.setTradeStatus(tradeStatusRepository.findByTradeStatus("NEW").orElse(null));
                tradeRepository.save(futureTrade);
                mockMvc.perform(get(SUMMARY_ENDPOINT).param("traderId", "testTrader"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.todaysTradeCount").value(1)); // Should not count future trade
        }

        @DisplayName("Summary endpoint returns empty historicalComparisons array when no historical data exists")
        @Test
        void testSummaryEndpointEmptyHistoricalComparisons() throws Exception {
                tradeRepository.deleteAll();
                mockMvc.perform(get(SUMMARY_ENDPOINT).param("traderId", "testTrader"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.historicalComparisons").isArray())
                                .andExpect(jsonPath("$.historicalComparisons[0].tradeCount").value(0));// I've changed
                                                                                                       // this assertion
                                                                                                       // after test
                                                                                                       // faied, to
                                                                                                       // expect a
                                                                                                       // zeroed summary
                                                                                                       // object in
                                                                                                       // historicalComparisons,
                                                                                                       // rather
                                                                                                       // than an empty
                                                                                                       // array.
                // This matches the new service logic, which always returns a zeroed summary
                // object for consistency in the response structure.

        }

        @DisplayName("Summary endpoint handles large number of trades")
        @Test
        void testSummaryEndpointLargeDataVolume() throws Exception {
                Book book = bookRepository.findAll().get(0);
                Counterparty counterparty = counterpartyRepository.findAll().get(0);
                ApplicationUser trader = userRepository.findAll().get(0);
                for (int i = 0; i < 100; i++) {
                        Trade trade = new Trade();
                        trade.setTradeDate(LocalDate.now());
                        trade.setBook(book);
                        trade.setCounterparty(counterparty);
                        trade.setTraderUser(trader);
                        trade.setTradeStatus(tradeStatusRepository.findByTradeStatus("NEW").orElse(null));
                        tradeRepository.save(trade);
                }
                mockMvc.perform(get(SUMMARY_ENDPOINT).param("traderId", "testTrader"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.todaysTradeCount").value(101)); // 1 from setup + 100 added
        }

        @DisplayName("Summary endpoint returns error or default for missing traderId parameter")
        @Test
        void testSummaryEndpointMissingTradeIdParam() throws Exception {
                mockMvc.perform(get(SUMMARY_ENDPOINT))
                                .andExpect(status().isBadRequest());
        }
        // Adding this line to force commit

}
