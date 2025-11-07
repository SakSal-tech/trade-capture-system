package com.technicalchallenge.validation;

import com.technicalchallenge.dto.TradeDTO;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class TradeDateValidator {
    // Accepts TradeDTO, entire trade object being validated and
    // TradeValidationResult, the shared result object that stores all validation
    // messages
    public void validate(TradeDTO trade, TradeValidationResult result) {

        // Strict presence validation: treat missing required dates as
        // validation errors rather than allowing NPEs.
        // 1) tradeDate is always required
        if (trade.getTradeDate() == null) {
            result.setError("tradeDate is required");
        }

        // 2) If trade contains legs, start and maturity dates are required to
        // support cashflow generation and scheduling
        boolean hasLegs = (trade.getTradeLegs() != null && !trade.getTradeLegs().isEmpty());
        if (hasLegs) {
            if (trade.getTradeStartDate() == null) {
                result.setError("tradeStartDate is required when trade legs are present");
            }
            if (trade.getTradeMaturityDate() == null) {
                result.setError("tradeMaturityDate is required when trade legs are present");
            }
        }

        // After presence checks, perform logical comparisons only when values
        // exist to avoid NPEs.
        if (trade.getTradeMaturityDate() != null && trade.getTradeStartDate() != null) {
            if (trade.getTradeMaturityDate().isBefore(trade.getTradeStartDate())) {
                result.setError("Maturity date cannot be before start date");
            }
        }

        if (trade.getTradeStartDate() != null && trade.getTradeDate() != null) {
            if (trade.getTradeStartDate().isBefore(trade.getTradeDate())) {
                result.setError("Start date cannot be before trade date");
            }
        }

        // Trade date recency check
        if (trade.getTradeDate() != null) {
            long daysBetween = ChronoUnit.DAYS.between(trade.getTradeDate(), LocalDate.now());
            if (daysBetween > 30) {
                result.setError("Trade date cannot be more than 30 days in the past");
            }
        }

    }

}
