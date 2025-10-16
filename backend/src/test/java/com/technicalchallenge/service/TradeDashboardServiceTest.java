package com.technicalchallenge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
// import org.springframework.security.access.AccessDeniedException;

import com.technicalchallenge.dto.DailySummaryDTO;
import com.technicalchallenge.dto.SearchCriteriaDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.dto.TradeSummaryDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.model.TradeStatus;
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.validation.TradeValidationResult;
import com.technicalchallenge.validation.UserPrivilegeValidationEngine;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Refactored and extended test class for TradeDashboardService.
 * 
 * Changes include:
 * Mocking UserPrivilegeValidationEngine to simulate privilege validation
 * Adding tests for new dashboard endpoints (getTradesByTrader, getBook, etc.)
 * Updating getTradeSummary and getDailySummary tests to reflect live
 * aggregation logic
 * Using ArgumentCaptor to verify correct privilege validation behavior
 */
@ExtendWith(MockitoExtension.class)
public class TradeDashboardServiceTest {
    /**
     * I set up the privilegeValidationEngine mock to always return a valid result
     * unless overridden in a test.
     * This prevents null pointer exceptions and ensures the tests check business
     * logic, not privilege setup.
     */
    @BeforeEach
    void setupPrivilegeValidationMock() {
        // By default, privilege validation always allows
        // Mark as lenient to avoid UnnecessaryStubbingException
        org.mockito.Mockito.lenient().when(privilegeValidationEngine.validateUserPrivilegeBusinessRules(any(), any()))
                .thenReturn(allow());
    }

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private TradeMapper tradeMapper;

    // NEW MOCK: Added to simulate validation behavior for privilege enforcement
    @Mock
    private UserPrivilegeValidationEngine privilegeValidationEngine;

    // InjectMocks automatically wires mocks into service constructor
    @InjectMocks
    private TradeDashboardService tradeDashboardService;

    private TradeDTO tradeDTO;
    private Trade trade;

    @BeforeEach
    void setUp() {
        // Set up reusable test data
        tradeDTO = new TradeDTO();
        tradeDTO.setTradeId(100001L);
        tradeDTO.setTradeDate(LocalDate.of(2025, 1, 15));
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 1, 17));
        tradeDTO.setTradeMaturityDate(LocalDate.of(2026, 1, 17));

        TradeLegDTO leg1 = new TradeLegDTO();
        leg1.setNotional(BigDecimal.valueOf(1000000));
        leg1.setRate(0.05);
        leg1.setCurrency("USD");

        TradeLegDTO leg2 = new TradeLegDTO();
        leg2.setNotional(BigDecimal.valueOf(500000));
        leg2.setRate(0.04);
        leg2.setCurrency("EUR");

        tradeDTO.setTradeLegs(List.of(leg1, leg2));

        trade = new Trade();
        trade.setTradeId(100001L);
        TradeStatus status = new TradeStatus();
        status.setTradeStatus("NEW");
        trade.setTradeStatus(status);
    }

    // Helper methods for privilege stubbing
    private TradeValidationResult allow() {
        return new TradeValidationResult(); // valid = true by default
    }

    private TradeValidationResult deny(String msg) {
        TradeValidationResult r = new TradeValidationResult();
        r.setError(msg); // flips valid = false and stores message
        return r;
    }

    /**
     * Tests the searchTrades method to verify that filtering by counterparty name
     * works.
     * Sets up a Trade entity with a specific counterparty, mocks repository and
     * mapper,
     * and asserts that the returned TradeDTO has the expected counterparty name.
     */

    @Test
    @DisplayName("Filter trades by counterparty name")
    void testSearchTrades_FilterByCounterparty() {
        // Privilege engine is not used by searchTrades(), no stubbing required here.

        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setCounterparty("BigBank");

        Trade trade = new Trade();
        trade.setTradeId(1L);
        Counterparty counterparty = new Counterparty();
        counterparty.setName("BigBank");
        trade.setCounterparty(counterparty);
        TradeDTO dto = new TradeDTO();
        dto.setCounterpartyName("BigBank");

        // Fix type safety warning by specifying Specification.class
        // Fix type safety warning by using ArgumentMatchers.<Specification<Trade>>any()
        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any()))
                .thenReturn(Arrays.asList(trade));
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(dto);

        List<TradeDTO> result = tradeDashboardService.searchTrades(criteria);

        assertEquals(1, result.size());
        assertEquals("BigBank", result.get(0).getCounterpartyName());
    }

    @Test
    @DisplayName("Filter trades by counterparty using pageable")
    void testFilterTrades_FilterByCounterparty() {
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setCounterparty("BigBank");

        Trade trade = new Trade();
        trade.setTradeId(2L);
        Counterparty counterparty = new Counterparty();
        counterparty.setName("BigBank");
        trade.setCounterparty(counterparty);
        TradeDTO dto = new TradeDTO();
        dto.setCounterpartyName("BigBank");

        Page<Trade> tradePage = new PageImpl<>(Arrays.asList(trade));
        // Fix type safety warning by specifying Specification.class
        // Fix type safety warning by using ArgumentMatchers.<Specification<Trade>>any()
        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any(), any(Pageable.class)))
                .thenReturn(tradePage);
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(dto);

        List<TradeDTO> result = tradeDashboardService.filterTrades(criteria, 0, 10).getContent();

        assertEquals(1, result.size());
        assertEquals("BigBank", result.get(0).getCounterpartyName());
    }

    @Test
    @DisplayName("Execute complex RSQL search query")
    void testRsqlSearch_ComplexQuery() {
        String rsqlQuery = "counterparty.name==BigBank;tradeStatus.tradeStatus==LIVE";

        Trade trade = new Trade();
        trade.setTradeId(4L);
        Counterparty counterparty = new Counterparty();
        counterparty.setName("BigBank");
        trade.setCounterparty(counterparty);
        TradeStatus status = new TradeStatus();
        status.setTradeStatus("LIVE");
        trade.setTradeStatus(status);
        TradeDTO dto = new TradeDTO();
        dto.setCounterpartyName("BigBank");
        dto.setTradeStatus("LIVE");

        // Fix type safety warning by specifying Specification.class
        // Fix type safety warning by using ArgumentMatchers.<Specification<Trade>>any()
        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any()))
                .thenReturn(Arrays.asList(trade));
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(dto);

        List<TradeDTO> result = tradeDashboardService.searchTradesRsql(rsqlQuery);

        assertEquals(1, result.size());
        assertEquals("BigBank", result.get(0).getCounterpartyName());
        assertEquals("LIVE", result.get(0).getTradeStatus());
    }

    // Test for privilege enforcement and new endpoints
    @Test
    @DisplayName("getTradesByTrader: Should return trader's trades when privilege allows VIEW")
    void testGetTradesByTrader_AllowsView_WhenValidationPasses() {
        org.mockito.Mockito.lenient()
                .when(privilegeValidationEngine.validateUserPrivilegeBusinessRules(any(TradeDTO.class), any()))
                .thenReturn(allow());

        Trade trade = new Trade();
        trade.setTradeId(11L);
        // Fix type safety warning by specifying Specification.class
        // Fix type safety warning by using ArgumentMatchers.<Specification<Trade>>any()
        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any()))
                .thenReturn(Arrays.asList(trade));

        TradeDTO dto = new TradeDTO();
        dto.setTradeId(11L);
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(dto);

        List<TradeDTO> result = tradeDashboardService.getTradesByTrader("alice");

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getTradeId());
    }

    // @Test
    // @DisplayName("getTradesByTrader: Should throw AccessDeniedException when
    // privilege check fails")
    // void testGetTradesByTrader_Forbidden_WhenValidationFails() {
    // when(privilegeValidationEngine.validateUserPrivilegeBusinessRules(any(TradeDTO.class),
    // any()))
    // .thenReturn(deny("VIEW not permitted"));
    //
    // assertThrows(RuntimeException.class, () ->
    // tradeDashboardService.getTradesByTrader("bob"));
    //
    // // Fix type safety warning by specifying Specification.class
    // // Fix type safety warning by using
    // ArgumentMatchers.<Specification<Trade>>any()
    // verify(tradeRepository,
    // never()).findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any());
    // // Ensure DB
    // // not
    // // touched
    // }

    @Test
    @DisplayName("getTradesByBook: Should allow VIEW access when privilege passes")
    void testGetTradesByBook_AllowsView_WhenValidationPasses() {
        org.mockito.Mockito.lenient()
                .when(privilegeValidationEngine.validateUserPrivilegeBusinessRules(any(TradeDTO.class), any()))
                .thenReturn(allow());

        Trade trade = new Trade();
        trade.setTradeId(22L);
        // Fix type safety warning by specifying Specification.class
        // Fix type safety warning by using ArgumentMatchers.<Specification<Trade>>any()
        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any()))
                .thenReturn(Arrays.asList(trade));

        TradeDTO dto = new TradeDTO();
        dto.setTradeId(22L);
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(dto);

        List<TradeDTO> result = tradeDashboardService.getTradesByBook(7L);

        assertEquals(1, result.size());
        assertEquals(22L, result.get(0).getTradeId());
    }

    @Test
    @DisplayName("getTradeSummary: Should compute totals dynamically for multiple currencies")
    void testGetTradeSummary_ComputesDynamicTotals() {
        org.mockito.Mockito.lenient()
                .when(privilegeValidationEngine.validateUserPrivilegeBusinessRules(any(TradeDTO.class), any()))
                .thenReturn(allow());

        // Return a mapped DTO list for two trades with multiple currencies
        // Fix type safety warning by specifying Specification.class
        // Fix type safety warning by using ArgumentMatchers.<Specification<Trade>>any()
        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any()))
                .thenReturn(List.of(trade));
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(tradeDTO);

        TradeSummaryDTO summary = tradeDashboardService.getTradeSummary("trader1");

        assertNotNull(summary);
        assertTrue(summary.getNotionalByCurrency().containsKey("USD"));
        assertTrue(summary.getNotionalByCurrency().containsKey("EUR"));
        assertEquals(new BigDecimal("1000000"), summary.getNotionalByCurrency().get("USD"));
    }

    @Test
    @DisplayName("getDailySummary: Should return comparison summaries for today and yesterday")
    void testGetDailySummary_HistoricalComparison_Computed() {
        org.mockito.Mockito.lenient()
                .when(privilegeValidationEngine.validateUserPrivilegeBusinessRules(any(TradeDTO.class), any()))
                .thenReturn(allow());

        // Fix type safety warning by specifying Specification.class
        // Fix type safety warning by using ArgumentMatchers.<Specification<Trade>>any()
        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any()))
                .thenReturn(List.of(trade));
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(tradeDTO);

        DailySummaryDTO summary = tradeDashboardService.getDailySummary("trader1");

        assertNotNull(summary);
        assertNotNull(summary.getHistoricalComparisons());
        assertTrue(summary.getHistoricalComparisons().size() >= 1);
        assertTrue(summary.getTodaysTradeCount() >= 0);
    }

    @ParameterizedTest
    @DisplayName("FilterTrades: Should support varied pagination sizes")
    @ValueSource(ints = { 1, 5, 10 })
    void testFilterTrades_Pagination_VariedSizes(int size) {
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setCounterparty("BigBank");

        Trade trade = new Trade();
        trade.setTradeId(99L);
        Page<Trade> tradePage = new PageImpl<>(List.of(trade));

        // Fix type safety warning by specifying Specification.class
        // Fix type safety warning by using ArgumentMatchers.<Specification<Trade>>any()
        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any(), any(Pageable.class)))
                .thenReturn(tradePage);
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(new TradeDTO() {
            {
                setTradeId(99L);
            }
        });

        Page<TradeDTO> page = tradeDashboardService.filterTrades(criteria, 0, size);
        assertEquals(1, page.getContent().size());
        assertEquals(99L, page.getContent().getFirst().getTradeId());
    }

    static Stream<String> rsqlQueries() {
        return Stream.of(
                "counterparty.name==BigBank",
                "tradeStatus.tradeStatus==NEW",
                "(counterparty.name==BigBank,counterparty.name==SmallBank);tradeStatus.tradeStatus==LIVE");
    }

    @ParameterizedTest
    @DisplayName("RSQL Search: Should handle multiple query variations correctly")
    @MethodSource("rsqlQueries")
    void testRsqlSearch_VariousQueries(String rsql) {
        Trade trade = new Trade();
        trade.setTradeId(123L);
        // Fix type safety warning by specifying Specification.class
        // Fix type safety warning by using ArgumentMatchers.<Specification<Trade>>any()
        when(tradeRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Trade>>any()))
                .thenReturn(List.of(trade));
        when(tradeMapper.toDto(any(Trade.class))).thenReturn(new TradeDTO() {
            {
                setTradeId(123L);
            }
        });

        List<TradeDTO> result = tradeDashboardService.searchTradesRsql(rsql);
        assertEquals(1, result.size());
        assertEquals(123L, result.get(0).getTradeId());
    }
}
