package com.technicalchallenge.controller;

import com.technicalchallenge.dto.SearchCriteriaDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.service.TradeDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
    // Changed authorization to allow either the listed ROLE_* authorities OR
    // the TRADE_VIEW privilege authority. map profile -> ROLE_* in the DB
    // but individual privileges are stored as plain authorities (e.g. TRADE_VIEW),
    // so this expression ensures both models grant access.
    @PreAuthorize("(hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT')) or hasAuthority('TRADE_VIEW')")
    public ResponseEntity<List<TradeDTO>> searchTrades(
            @RequestParam(required = false) String counterparty,
            @RequestParam(required = false) String book) {

        // I've changed this to match service signature: it now builds a
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
     * so I've wrapped the result in a PageImpl since the service returns a List.
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
    // As above: allow users with ROLE_TRADER / ROLE_MIDDLE_OFFICE / ROLE_SUPPORT
    // OR users who have the TRADE_VIEW privilege authority.
    @PreAuthorize("(hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT')) or hasAuthority('TRADE_VIEW')")
    public ResponseEntity<?> filterTrades(
            @RequestParam(required = false) String counterparty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        SearchCriteriaDTO criteria = new SearchCriteriaDTO();
        criteria.setCounterparty(counterparty);

        Page<TradeDTO> pagedResult = tradeDashboardService.filterTrades(criteria, page, size);

        // I'm now wrapping the response to include both count and content
        Map<String, Object> response = Map.of(
                "count", pagedResult.getTotalElements(),
                "content", pagedResult.getContent());

        return ResponseEntity.ok(response);
    }

    /**
     * RSQL-style search endpoint for dynamic queries.
     *
     * Roles allowed: TRADER, MIDDLE_OFFICE, SUPPORT
     * service returns a List, not a Page, so I've wrapped it using PageImpl
     * to satisfy integration test expectations for $.content in the response.
     */
    @GetMapping("/rsql")
    // RSQL search uses the same authorization model as the other read endpoints.
    // This keeps behavior consistent between role-based and privilege-based users.
    @PreAuthorize("(hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT')) or hasAuthority('TRADE_VIEW')")
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
    // Only allow users with role TRADER to call this endpoint and require that
    // the requested traderId matches the authenticated user. This prevents a
    // trader from reading another trader's list by supplying a different id.
    @PreAuthorize("hasRole('TRADER') and #traderId != null and #traderId.equalsIgnoreCase(authentication.name)")
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
    // Allow MIDDLE_OFFICE or users with TRADE_VIEW to
    // request any trader's summary. A TRADER may only request their own
    // summary (compare request param to authentication.name). Example: if
    // 'joey' is logged in and tries to call /api/dashboard/summary?traderId=simon
    // this PreAuthorize will deny the request because #traderId !=
    // authentication.name. This prevents accidental or malicious access to
    // another trader's data at the controller layer.
    // Conservative rule: only MIDDLE_OFFICE or users with TRADE_VIEW_ALL may view
    // other traders' summaries. A TRADER may view only their own summary.
    @PreAuthorize("hasAnyRole('MIDDLE_OFFICE') or hasAuthority('TRADE_VIEW_ALL') or (#traderId != null and #traderId.equalsIgnoreCase(authentication.name))")
    public ResponseEntity<?> getTradeSummary(@RequestParam String traderId) {
        Object summary = tradeDashboardService.getTradeSummary(traderId);
        return ResponseEntity.ok(summary);
    }

    /**
     * Provides a daily summary comparing today's and yesterday's trades.
     *
     * Roles allowed: TRADER and MIDDLE_OFFICE.
     * The SUPPORT role must be denied (tests expect 403 Forbidden).
     * I have added this restriction via @PreAuthorize.
     */
    @GetMapping("/daily-summary")
    // Same reasoning as /summary: allow role-based access or the TRADE_VIEW
    // privilege authority. Tests expect SUPPORT to be denied here.
    // Similar guard: TRADER only allowed to request their own daily summary;
    // MIDDLE_OFFICE and users with TRADE_VIEW can request other traders.
    // Example: logged-in 'joey' cannot request traderId='simon' unless they
    // have the TRADE_VIEW privilege or are MIDDLE_OFFICE.
    // Same conservative semantics as /summary: require MIDDLE_OFFICE or
    // TRADE_VIEW_ALL to view other traders' daily summaries; traders can view
    // only their own.
    @PreAuthorize("hasAnyRole('MIDDLE_OFFICE') or hasAuthority('TRADE_VIEW_ALL') or (#traderId == null || #traderId.equalsIgnoreCase(authentication.name))")
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
