package com.technicalchallenge.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.Map;
import java.util.List;

@Getter
@Setter
public class TradeSummaryDTO {

    // Total number of trades by status (e.g. NEW, CANCELLED). To be grouped trades
    // by their status
    private Map<String, Long> tradesByStatus;

    // Total notional amounts grouped by currency (e.g. USD, EUR)
    private Map<String, BigDecimal> notionalByCurrency;

    // Breakdown by trade type and counterparty. Grouping by status or currency,
    // using two factors combined
    // Example: {"FX_SPOT:ABC_BANK": 5, "FX_FORWARD:XYZ_CORP": 3}
    private Map<String, Long> tradesByTypeAndCounterparty;

    // Risk exposure summaries (e.g. total delta, vega, etc.)
    // Summary metrics as key-value pairs for flexibility.
    private Map<String, BigDecimal> riskExposureSummary;

    // Weekly comparisons: list of per-day summaries (oldest first)
    // Each entry contains trade count and notional totals for that day.
    private List<DailySummaryDTO.DailyComparisonSummary> weeklyComparisons;

    // All-time labeled fields (mirrors top-level fields but explicitly named)
    private Map<String, Long> allTimeTradesByStatus;
    private Map<String, BigDecimal> allTimeNotionalByCurrency;
    private Map<String, Long> allTimeTradesByTypeAndCounterparty;
    private Map<String, BigDecimal> allTimeRiskExposureSummary;

    // Weekly labeled aggregates (summary across the last 7 days)
    private Map<String, Long> weeklyTradesByStatus;
    private Map<String, BigDecimal> weeklyNotionalByCurrency;
    private Map<String, Long> weeklyTradesByTypeAndCounterparty;
    private Map<String, BigDecimal> weeklyRiskExposureSummary;

    // Book-level activity summaries (bookId -> trade count, notional, etc.)
    // Added to support dashboard Active Books count display
    private Map<Long, DailySummaryDTO.BookActivitySummary> bookActivitySummaries;

}
