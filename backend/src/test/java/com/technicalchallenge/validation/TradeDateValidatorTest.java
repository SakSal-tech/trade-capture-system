package com.technicalchallenge.validation;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.technicalchallenge.dto.TradeDTO;

public class TradeDateValidatorTest {
    // creates a new instance of the test class for each test method
    private TradeValidationEngine tradeValidationService;
    private TradeDTO trade;

    // Refactoring setting up shared object so it is not repeated in every test
    @BeforeEach
    void setUp() {
        tradeValidationService = new TradeValidationEngine();
        trade = new TradeDTO();
    }

    @DisplayName("Should fail when maturity date is before start date")
    @Test
    // Prove that when majurityDate < startDate,the validator flags the trade as
    // invalid and returns a clearer message.
    void failWhenMajurityBeforeStartDate() {
        // SETUP: building a trade where majurity < start
        trade.setTradeDate(LocalDate.now());// Create a specific fixed date
        trade.setTradeStartDate(LocalDate.of(2025, 10, 15));
        trade.setTradeMaturityDate(LocalDate.of(2025, 10, 12)); // earlier than start which is invalid

        // calls the business-rule validator (the method not written yet).
        TradeValidationResult result = tradeValidationService.validateTradeBusinessRules(trade);

        // verify that trade is considered invalid.
        assertFalse(result.isValid());
        // verify the right human-readable error message is produced.
        assertTrue(result.getErrors().contains("Maturity date cannot be before start date"));

    }

    @DisplayName("Should fail when start date is before trade date")
    @Test
    void failWhenStartBeforeTradeDate() {
        // setup
        trade.setTradeDate(LocalDate.now());
        trade.setTradeStartDate(LocalDate.of(2025, 10, 10)); // invalid
        trade.setTradeMaturityDate(LocalDate.of(2025, 10, 20));

        TradeValidationResult result = tradeValidationService.validateTradeBusinessRules(trade);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Start date cannot be before trade date"));

    }

    @DisplayName("Should fail when trade date is more than 30 days old")
    @Test
    void failWhenTradeDateOlderThan30Days() {
        trade.setTradeDate(LocalDate.now().minusDays(45));// invalid 2025-08-27 45 days ago
        trade.setTradeStartDate(LocalDate.now().minusDays(10));
        trade.setTradeMaturityDate(LocalDate.now().plusDays(30));

        TradeValidationResult result = tradeValidationService.validateTradeBusinessRules(trade);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Trade date cannot be more than 30 days in the past"));

    }

}
