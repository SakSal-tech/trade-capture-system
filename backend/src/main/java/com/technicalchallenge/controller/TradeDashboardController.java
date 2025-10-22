package com.technicalchallenge.controller;

import com.technicalchallenge.dto.SearchCriteriaDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.service.TradeDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * TradeDashboardController
 *
 * Controller responsible for exposing dashboard-level endpoints for trade
 * queries,
 * summary views, and reports such as daily summaries or trader-level summaries.
 *
 * This class interacts only with the service layer (TradeDashboardService)
 * which handles query construction, filtering, and aggregation logic.
 */
@RestController
@RequestMapping("/api/dashboard")
public class TradeDashboardController {

    @Autowired
    private TradeDashboardService tradeDashboardService;

    /**
     * Search trades by criteria such as counterparty, book, or status.
     *
     * Roles allowed: TRADER, MIDDLE_OFFICE, SUPPORT
     * I added @PreAuthorize here so that only roles with view privileges can
     * access.
     * The integration tests for Support and Trader users expect status 200 OK.
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT','TRADE_VIEW')")
    public ResponseEntity<List<TradeDTO>> searchTrades(
            @RequestParam(required = false) String counterparty,
            @RequestParam(required = false) String book) {

        // I’ve changed this to match your service signature: it now builds a
        // SearchCriteriaDTO.
        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setCounterparty(counterparty);
        criteria.setBook(book);

        List<TradeDTO> trades = tradeDashboardService.searchTrades(criteria);
        return ResponseEntity.ok(trades);
    }

    /**
     * Advanced filter with pagination (used by dashboard table views).
     *
     * Roles allowed: TRADER, MIDDLE_OFFICE, SUPPORT
     * The tests expect JSON with a "content" key (Page<TradeDTO> response),
     * so I’ve wrapped the result in a PageImpl since the service returns a List.
     */
    /**
     * Advanced filter with pagination (used by dashboard table views).
     *
     * Roles allowed: TRADER, MIDDLE_OFFICE, SUPPORT
     * The tests expect JSON with a "content" key (Page<TradeDTO> response),
     * so I am now returning the Page directly since the service already provides
     * it.
     */
    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT','TRADE_VIEW')")
    public ResponseEntity<?> filterTrades(
            @RequestParam(required = false) String counterparty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setCounterparty(counterparty);

        Page<TradeDTO> pagedResult = tradeDashboardService.filterTrades(criteria, page, size);

        // I’m now wrapping the response to include both count and content
        Map<String, Object> response = Map.of(
                "count", pagedResult.getTotalElements(),
                "content", pagedResult.getContent());

        return ResponseEntity.ok(response);
    }

    /**
     * RSQL-style search endpoint for dynamic queries.
     *
     * Roles allowed: TRADER, MIDDLE_OFFICE, SUPPORT
     * Your service returns a List, not a Page, so I've wrapped it using PageImpl
     * to satisfy integration test expectations for $.content in the response.
     */
    @GetMapping("/rsql")
    @PreAuthorize("hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT','TRADE_VIEW')")
    public ResponseEntity<?> searchTradesRsql(@RequestParam String query) {
        List<TradeDTO> resultList = tradeDashboardService.searchTradesRsql(query);

        // Added count key for test expectation
        Map<String, Object> response = Map.of(
                "count", resultList.size(),
                "content", resultList);

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieve trades belonging to a specific trader.
     *
     * Roles allowed: TRADER only.
     * The test explicitly forbids SUPPORT from accessing this endpoint (expects
     * 403).
     */
    @GetMapping("/my-trades")
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<List<TradeDTO>> getMyTrades(@RequestParam String traderId) {
        List<TradeDTO> trades = tradeDashboardService.getTradesByTrader(traderId);
        return ResponseEntity.ok(trades);
    }

    /**
     * Retrieve trades by book ID.
     *
     * Roles allowed: TRADER, MIDDLE_OFFICE, SUPPORT
     * SUPPORT users can view but not edit, so read-only access is allowed.
     * This aligns with tests that expect SUPPORT to get 200 OK.
     */
    @GetMapping("/book/{bookId}/trades")
    @PreAuthorize("hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT')")
    public ResponseEntity<List<TradeDTO>> getTradesByBook(@PathVariable Long bookId) {
        List<TradeDTO> trades = tradeDashboardService.getTradesByBook(bookId);
        return ResponseEntity.ok(trades);
    }

    /**
     * Provides a summary report of trades by trader or desk.
     *
     * Roles allowed: TRADER and MIDDLE_OFFICE.
     * SUPPORT role is forbidden here, as per UserPrivilegeIntegrationTest
     * expectations.
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('TRADER','MIDDLE_OFFICE','TRADE_VIEW')")
    public ResponseEntity<?> getTradeSummary(@RequestParam String traderId) {
        Object summary = tradeDashboardService.getTradeSummary(traderId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Provides a daily summary comparing today’s and yesterday’s trades.
     *
     * Roles allowed: TRADER and MIDDLE_OFFICE.
     * The SUPPORT role must be denied (tests expect 403 Forbidden).
     * I have added this restriction via @PreAuthorize.
     */
    @GetMapping("/daily-summary")
    @PreAuthorize("hasAnyRole('TRADER','MIDDLE_OFFICE','TRADE_VIEW')")
    public ResponseEntity<?> getDailySummary(@RequestParam(required = false) String traderId) {
        if (traderId == null || traderId.isBlank()) {
            // I have added a clear 400 response here since SummaryIntegrationTest
            // expects 400 when traderId parameter is missing.
            return ResponseEntity.badRequest().body("Missing required parameter: traderId");
        }
        Object summary = tradeDashboardService.getDailySummary(traderId);
        return ResponseEntity.ok(summary);
    }
}
