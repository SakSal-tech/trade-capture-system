package com.technicalchallenge.dto;

import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;
import java.util.Map;

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
}
