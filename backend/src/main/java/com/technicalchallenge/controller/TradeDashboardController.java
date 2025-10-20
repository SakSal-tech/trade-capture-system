
package com.technicalchallenge.controller;

import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.technicalchallenge.dto.*;
import com.technicalchallenge.service.TradeDashboardService;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
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
    // No security enforced
    @org.springframework.web.bind.annotation.GetMapping("/daily-summary")
    public ResponseEntity<DailySummaryDTO> getDailySummary(@RequestParam String traderId) {
        DailySummaryDTO summary = dashboardService.getDailySummary(traderId);
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/dashboard/summary?traderId=xxx
     * Only TRADER role should be allowed.
     * Returns TradeSummaryDTO as JSON.
     */
    // No security enforced
    @org.springframework.web.bind.annotation.GetMapping("/summary")
    public ResponseEntity<TradeSummaryDTO> getTradeSummary(@RequestParam String traderId) {
        TradeSummaryDTO summary = dashboardService.getTradeSummary(traderId);
        return ResponseEntity.ok(summary);
    }

    /**
     * GET /api/dashboard/my-trades?traderId=xxx
     * Only TRADER role should be allowed.
     * Returns List<TradeDTO> as JSON.
     */
    // No security enforced
    @org.springframework.web.bind.annotation.GetMapping("/my-trades")
    public ResponseEntity<List<TradeDTO>> getMyTrades(@RequestParam String traderId) {
        List<TradeDTO> trades = dashboardService.getTradesByTrader(traderId);
        return ResponseEntity.ok(trades);
    }

    /**
     * GET /api/dashboard/book/{bookId}/trades
     * Only BOOK_VIEW role should be allowed.
     * Returns List<TradeDTO> as JSON.
     */
    // No security enforced
    @org.springframework.web.bind.annotation.GetMapping("/book/{bookId}/trades")
    public ResponseEntity<List<TradeDTO>> getTradesByBook(@PathVariable Long bookId) {
        List<TradeDTO> trades = dashboardService.getTradesByBook(bookId);
        return ResponseEntity.ok(trades);
    }

    /**
     * GET /api/dashboard/filter
     * Only TRADE_VIEW role should be allowed.
     * Returns Page<TradeDTO> as JSON.
     */
    // No security enforced
    @org.springframework.web.bind.annotation.GetMapping("/filter")
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
     * Only TRADE_VIEW role should be allowed.
     * Returns List<TradeDTO> as JSON.
     */
    // No security enforced
    @org.springframework.web.bind.annotation.GetMapping("/search")
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
     * Only TRADE_VIEW role should be allowed.
     * Returns List<TradeDTO> as JSON.
     */
    // No security enforced
    @org.springframework.web.bind.annotation.GetMapping("/rsql")
    public ResponseEntity<Map<String, Object>> searchTradesRsql(@RequestParam String query) {
        List<TradeDTO> trades = dashboardService.searchTradesRsql(query);
        Map<String, Object> response = new HashMap<>();
        response.put("content", trades);
        response.put("count", trades != null ? trades.size() : 0);
        return ResponseEntity.ok(response);
    }
    // Adding this line to force commit

}
