package com.technicalchallenge.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.technicalchallenge.dto.DailySummaryDTO;
import com.technicalchallenge.dto.SearchCriteriaDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeSummaryDTO;
import com.technicalchallenge.service.TradeDashboardService;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/*This controller has been created for Separation of concerns: Keeps trade CRUD operations (TradeController) separate from dashboard/analytics endpoints.
Scalability: Easier to expand dashboard features without cluttering trade logic. */

@Validated // To make sure API calls are clean and users can't crash the endpoint with bad
           // parameters.
@RestController
@RequestMapping("/api/dashboard")

public class TradeDashboardController {
    @Autowired
    private TradeDashboardService tradeDashboardService;

    @GetMapping("/filter")
    // Page<TradeDTO> is a page (a paginated list) of TradeDTO objects
    // ResponseEntity<Page<TradeDTO>> is the HTTP response wrapper that contains
    // that page and extra HTTP info (status code, headers)
    public ResponseEntity<Page<TradeDTO>> filterTrades(@ModelAttribute SearchCriteriaDTO criteriaDTO,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

        Page<TradeDTO> pagedResult = tradeDashboardService.filterTrades(criteriaDTO, page, size);
        return ResponseEntity.ok(pagedResult);

    }

    @GetMapping("/search")
    // This method uses that criteriaDTO to find matching trades in the database and
    // returns them as a list of TradeDTO objects. criteriaDTO tells the service
    // what to search for and List<TradeDTO> is the
    // search result returned.
    public ResponseEntity<List<TradeDTO>> searchTrades(SearchCriteriaDTO criteriaDTO) {
        List<TradeDTO> results = tradeDashboardService.searchTrades(criteriaDTO);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/rsql")
    public ResponseEntity<List<TradeDTO>> searchTradesRsql(@RequestParam String query) {

        List<TradeDTO> trades = tradeDashboardService.searchTradesRsql(query);

        return ResponseEntity.ok(trades);
    }

    // Trader's personal trades
    @GetMapping("/my-trades")
    public List<TradeDTO> getMyTrades(@RequestParam @NotBlank String traderId) {
        return tradeDashboardService.getTradesByTrader(traderId);
    }

    // Book-level trade aggregation
    @GetMapping("/book/{bookId}/trades")
    public List<TradeDTO> getTradesByBook(@PathVariable @Positive Long bookId) {
        return tradeDashboardService.getTradesByBook(bookId);
    }

    // Portfolio summary — aggregate totals and exposure by trader
    @GetMapping("/summary")
    public TradeSummaryDTO getTradeSummary(@RequestParam @NotBlank String traderId) {
        return tradeDashboardService.getTradeSummary(traderId);
    }

    // Daily trading statistics — today's activity snapshot
    @GetMapping("/daily-summary")
    public DailySummaryDTO getDailySummary(@RequestParam @NotBlank String traderId) {
        return tradeDashboardService.getDailySummary(traderId);
    }

}
