package com.technicalchallenge.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.List; // I added this import because I changed historicalComparisons to use List instead of Map.
import lombok.Getter;
import lombok.Setter;

//Aggregated DTO, wraps related summaries(Today's trading , Book-level summaries, Historical comparisons) the Dashboard
@Getter
@Setter
public class DailySummaryDTO {
    // Today's trade count
    private int todaysTradeCount;

    // Today's total notional amount (by currency)
    private Map<String, BigDecimal> todaysNotionalByCurrency;

    // User-specific performance metrics (e.g., P&L, win rate)
    private Map<String, Object> userPerformanceMetrics;

    // Book-level activity summaries (bookId -> trade count, notional, etc.)
    private Map<Long, BookActivitySummary> bookActivitySummaries;

    // I changed this from Map<String, DailyComparisonSummary> to
    // List<DailyComparisonSummary> so the response matches the integration test
    // expectations. See Development-Errors-and-fixes.md for details.
    private List<DailyComparisonSummary> historicalComparisons;

    @Setter
    @Getter
    public static class BookActivitySummary {
        private int tradeCount;
        private Map<String, BigDecimal> notionalByCurrency;

    }

    @Setter
    @Getter
    public static class DailyComparisonSummary {
        private int tradeCount;
        private Map<String, BigDecimal> notionalByCurrency;

    }
}