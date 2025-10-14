package com.technicalchallenge.dto;

import java.math.BigDecimal;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

//Aggregated DTO, wraps related summaries(Today’s trading , Book-level summaries, Historical comparisons) the Dashboard
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

    // Comparison to previous trading days (date -> summary)
    private Map<String, DailyComparisonSummary> historicalComparisons;

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