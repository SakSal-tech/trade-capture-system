package com.technicalchallenge.service;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.validation.TradeValidationResult;
import com.technicalchallenge.validation.TradeValidationEngine;

public class TradeValidationServiceTest {
    // creates a new instance of the test class for each test method
    private final TradeValidationEngine tradeValidationService = new TradeValidationEngine();

    @DisplayName("Should fail when maturity date is before start date")
    @Test
    // Prove that when majurityDate < startDate,the validator flags the trade as
    // invalid and returns a clearer message.
    void failWhenMajurityBeforeStartDate() {
        // SETUP: building a trade where majurity < start
        TradeDTO trade = new TradeDTO();
        trade.setTradeDate(LocalDate.of(2025, 10, 10));// Create a specific fixed date
        trade.setTradeStartDate(LocalDate.of(02025, 10, 15));
        trade.setTradeMaturityDate(LocalDate.of(2025, 10, 12)); // earlier than start -> invalid

        // calls the business-rule validator (the method not written yet).
        TradeValidationResult result = tradeValidationService.validateTradeBusinessRules(trade);

        // verify that trade is considered invalid.
        assertFalse(result.isValid());
        // verify the right human-readable error message is produced.
        assertTrue(result.getErrors().contains("Maturity date cannot be before start date"));

    }

}
