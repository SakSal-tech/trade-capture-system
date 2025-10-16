package com.technicalchallenge.validation;

import com.technicalchallenge.dto.TradeDTO;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class TradeDateValidator {
    // Accepts TradeDTO, entire trade object being validated and
    // TradeValidationResult, the shared result object that stores all validation
    // messages
    public void validate(TradeDTO trade, TradeValidationResult result) {

        // Business rule 1: Maturity date cannot be before start date or trade date

        if ((trade.getTradeMaturityDate().isBefore(trade.getTradeStartDate())
                || (trade.getTradeStartDate().isBefore(trade.getTradeDate())))) {
            result.setError("Maturity date cannot be before start date");
        }
        // Business rule 2: Start date cannot be before trade date

        if (trade.getTradeStartDate().isBefore(trade.getTradeDate())) {
            result.setError("Start date cannot be before trade date");

        }
        // Business rule 3:Trade date cannot be more than 30 days in the past
        // long because because the number of days between two points in time can be
        // very large.
        // ChronoUnit, tells Java what unit of time want to measure between two
        // temporal objects
        long daysBetween = ChronoUnit.DAYS.between(trade.getTradeDate(), LocalDate.now());

        if (daysBetween > 30) {
            result.setError("Trade date cannot be more than 30 days in the past");

        }

    }

}
