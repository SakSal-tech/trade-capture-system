package com.technicalchallenge.controller;

import java.util.Map;
import java.util.HashMap;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import com.technicalchallenge.dto.*;
import com.technicalchallenge.service.TradeDashboardService;

import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Trade Dashboard", description = "Provides endpoints for trade summaries, filtering, and searches")
public class TradeDashboardController {

    private final TradeDashboardService dashboardService;

    public TradeDashboardController(TradeDashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * GET /api/dashboard/daily-summary?traderId=xxx
     * Only TRADER role should be allowed.
     * Returns DailySummaryDTO as JSON.
     */
    // I’ve added @PreAuthorize to make sure that only users with the TRADER role
    // can access the daily summary.
    // This matches the business rule that traders have full visibility over their
    // trades and summaries.
    @GetMapping("/daily-summary")
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<DailySummaryDTO> getDailySummary(@RequestParam String traderId) {
        DailySummaryDTO summary = dashboardService.getDailySummary(traderId);
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/dashboard/summary?traderId=xxx
     * Only TRADER role should be allowed.
     * Returns TradeSummaryDTO as JSON.
     */
    // I’ve added TRADER-only access here as well because trade summaries are part
    // of the trader’s own performance view.
    @GetMapping("/summary")
    // @PreAuthorize("hasRole('TRADER')")
    @PreAuthorize("hasAnyAuthority('TRADE_VIEW','TRADER')")

    public ResponseEntity<TradeSummaryDTO> getTradeSummary(@RequestParam String traderId) {
        TradeSummaryDTO summary = dashboardService.getTradeSummary(traderId);
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/dashboard/my-trades?traderId=xxx
     * Only TRADER role should be allowed.
     * Returns List<TradeDTO> as JSON.
     */
    // I’ve used @PreAuthorize so that only TRADERs can see their own trades.
    // Even though SUPPORT can view all trades, this endpoint is designed for the
    // trader’s own trades, not general visibility.
    @GetMapping("/my-trades")
    @PreAuthorize("hasRole('TRADER')")
    public ResponseEntity<List<TradeDTO>> getMyTrades(@RequestParam String traderId) {
        List<TradeDTO> trades = dashboardService.getTradesByTrader(traderId);
        return ResponseEntity.ok(trades);
    }

    /**
     * GET /api/dashboard/book/{bookId}/trades
     * View trades by book.
     * MIDDLE_OFFICE and SUPPORT can view, TRADER can also view since they have all
     * privileges.
     */
    // I’ve opened this endpoint to TRADER, MIDDLE_OFFICE, and SUPPORT since all of
    // them can view trades.
    // SALES cannot access this as their privileges only allow creation and
    // amendment.
    @GetMapping("/book/{bookId}/trades")
    @PreAuthorize("hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT')")
    public ResponseEntity<List<TradeDTO>> getTradesByBook(@PathVariable Long bookId) {
        List<TradeDTO> trades = dashboardService.getTradesByBook(bookId);
        return ResponseEntity.ok(trades);
    }

    /**
     * GET /api/dashboard/filter
     * Used for filtering trades.
     * TRADER, MIDDLE_OFFICE, and SUPPORT can view filtered results.
     */
    // I’ve allowed TRADER, MIDDLE_OFFICE, and SUPPORT because all three can view
    // trades.
    // SALES is excluded since they do not have trade viewing privileges.
    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT')")
    public ResponseEntity<Map<String, Object>> filterTrades(@RequestParam(required = false) String counterparty,
            @RequestParam(required = false) String book,
            @RequestParam(required = false) String trader,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        SearchCriteriaDTO criteria = new SearchCriteriaDTO(counterparty, book, trader, status, null, null);
        List<TradeDTO> trades = dashboardService.filterTrades(criteria, 0, 100).getContent();
        Map<String, Object> response = new HashMap<>();
        response.put("content", trades);
        response.put("count", trades.size());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/dashboard/search
     * Returns List<TradeDTO> as JSON.
     * TRADER, MIDDLE_OFFICE, and SUPPORT can view trades.
     */
    // I've set this to allow TRADER, MIDDLE_OFFICE, and SUPPORT.
    // This ensures that all roles with view privileges can access the search
    // functionality.
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT')")
    public ResponseEntity<List<TradeDTO>> searchTrades(@RequestParam(required = false) String counterparty,
            @RequestParam(required = false) String book,
            @RequestParam(required = false) String trader,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        SearchCriteriaDTO criteria = new SearchCriteriaDTO(counterparty, book, trader, status, null, null);
        List<TradeDTO> trades = dashboardService.searchTrades(criteria);
        return ResponseEntity.ok(trades);
    }

    /**
     * GET /api/dashboard/rsql?query=...
     * Used for advanced search queries.
     * TRADER, MIDDLE_OFFICE, and SUPPORT can view trades.
     */
    // I've used the same access control as the search endpoint.
    // All roles that can view trades are included here.
    @GetMapping("/rsql")
    @PreAuthorize("hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT')")
    public ResponseEntity<Map<String, Object>> searchTradesRsql(@RequestParam String query) {
        List<TradeDTO> trades = dashboardService.searchTradesRsql(query);
        Map<String, Object> response = new HashMap<>();
        response.put("content", trades);
        response.put("count", trades != null ? trades.size() : 0);
        return ResponseEntity.ok(response);
    }
}
