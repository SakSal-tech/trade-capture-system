package com.technicalchallenge.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.technicalchallenge.dto.TradeLegDTO;

/*
Cross-Leg Business Rules:
    Both legs must have identical maturity dates
    Legs must have opposite pay/receive flags
    Floating legs must have an index specified
    Fixed legs must have a valid rate
*/

public class TradeLegValidatorTest {
    private TradeLegDTO leg1;
    private TradeLegDTO leg2;

    // Refactored: Moved similar objects and list for multiple objects to the setup
    // method
    @BeforeEach
    void setUp() {
        leg1 = new TradeLegDTO();
        leg2 = new TradeLegDTO();
    }

    // Refactoring: Added helper method to reduce repeating similar code in test
    // methods (DRY principle).
    // Creates two legs with given maturity dates for quick setup.
    private List<TradeLegDTO> createLegsWithMaturityDates(LocalDate date1, LocalDate date2) {
        leg1.setTradeMaturityDate(date1);
        leg2.setTradeMaturityDate(date2);
        return List.of(leg1, leg2);
    }

    // Refactoring: Added helper method to encapsulate validation logic so it is not
    // repeated in each test.
    private TradeValidationResult validateLegs(List<TradeLegDTO> tradeLegs) {
        TradeValidationResult result = new TradeValidationResult();
        TradeLegValidator validator = new TradeLegValidator();
        validator.validateTradeLeg(tradeLegs, result);
        validator.validateTradeLegPayReceive(tradeLegs, result);
        return result;
    }

    @DisplayName("Should fail when two trade legs have different maturity dates")
    @Test
    // GIVEN two trade legs with different maturity dates
    // WHEN the validator checks the legs
    // THEN the validation should fail and return an appropriate error message
    void shouldFailWhenLegMaturityDatesDiffer() {
        List<TradeLegDTO> tradeLegs = createLegsWithMaturityDates(
                LocalDate.of(2025, 10, 10),
                LocalDate.of(2025, 11, 11));

        TradeValidationResult result = validateLegs(tradeLegs);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Both legs must have identical maturity dates"));
    }

    @DisplayName("Should fail when legs have the same pay/receive flag")
    @Test
    // GIVEN two legs with identical pay/receive flags
    // WHEN the validator checks the legs
    // THEN the validation should fail and return an appropriate error message
    void shouldFailWhenLegsHaveSamePayReceiveFlag() {
        leg1.setPayReceiveFlag("PAY");
        leg2.setPayReceiveFlag("PAY");

        List<TradeLegDTO> tradeLegs = List.of(leg1, leg2);
        TradeValidationResult result = validateLegs(tradeLegs);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Legs must have opposite pay/receive flags"));
    }

    @DisplayName("Floating leg index validation scenarios")
    @Test
    void testFloatingLegRequiresIndex() {
        // Floating leg with index specified (valid)
        TradeLegDTO floatingLegValid = new TradeLegDTO();
        floatingLegValid.setLegType("FLOATING");
        floatingLegValid.setIndexId(1L);
        floatingLegValid.setIndexName("GENERIC_INDEX");

        // Floating leg with missing index (invalid)
        TradeLegDTO floatingLegNoIndex = new TradeLegDTO();
        floatingLegNoIndex.setLegType("FLOATING");
        floatingLegNoIndex.setIndexId(null);
        floatingLegNoIndex.setIndexName(null);

        // Non-floating leg (should not require index)
        TradeLegDTO fixedLeg = new TradeLegDTO();
        fixedLeg.setLegType("FIXED");
        fixedLeg.setIndexId(null);
        fixedLeg.setIndexName(null);

        assertTrue(TradeLegValidator.validateFloatingLegIndex(floatingLegValid),
                "Floating leg with index should be valid");
        assertFalse(TradeLegValidator.validateFloatingLegIndex(floatingLegNoIndex),
                "Floating leg without index should be invalid");
        assertTrue(TradeLegValidator.validateFloatingLegIndex(fixedLeg),
                "Fixed leg should be valid even without index");
    }

    @DisplayName("Should fail when rate is null for fixed leg")
    @Test
    void shouldFailWhenRateIsNullForFixedLeg() {
        // Test: Fixed leg with null rate should be invalid
        TradeLegDTO leg = new TradeLegDTO();
        leg.setLegType("FIXED");
        leg.setRate(null);
        TradeLegValidator validator = new TradeLegValidator();
        boolean isValidRate = validator.validateLegRate(leg);
        assertFalse(isValidRate, "Fixed leg with null rate should be invalid");
    }

    @DisplayName("Should fail when rate is zero or negative for fixed leg")
    @Test
    // Verifies the business rule that a fixed leg in a trade must have a strictly
    // positive interest rate
    void shouldFailWhenRateIsZeroOrNegativeForFixedLeg() {
        TradeLegDTO leg = new TradeLegDTO();
        leg.setLegType("FIXED");
        // Check if zero rate is accepted
        leg.setRate(0.0);
        TradeLegValidator validator = new TradeLegValidator();
        boolean isValidRate = validator.validateLegRate(leg);
        assertFalse(isValidRate, "Fixed leg with zero rate should be invalid");

        // Negative rate should be rejected
        leg.setRate(-1.0);
        isValidRate = validator.validateLegRate(leg);
        assertFalse(isValidRate, "Fixed leg with negative rate should be invalid");
    }

    @DisplayName("Should fail when rate is unrealistically high for fixed leg")
    @Test
    void shouldFailWhenRateIsUnrealisticallyHighForFixedLeg() {
        // Test: Fixed leg with unrealistically high rate should be invalid
        TradeLegDTO leg = new TradeLegDTO();
        leg.setLegType("FIXED");
        leg.setRate(150.0); // Example: 150% is unrealistic
        TradeLegValidator validator = new TradeLegValidator();
        boolean isValidRate = validator.validateLegRate(leg);
        assertFalse(isValidRate, "Fixed leg with unrealistically high rate should be invalid");
    }

    @DisplayName("Should pass when rate is null for floating leg")
    @Test
    void shouldPassWhenRateIsNullForFloatingLeg() {
        // Test: Floating leg with null rate should be valid
        TradeLegDTO leg = new TradeLegDTO();
        leg.setLegType("FLOATING");
        leg.setRate(null);
        TradeLegValidator validator = new TradeLegValidator();
        boolean isValidRate = validator.validateLegRate(leg);
        assertTrue(isValidRate, "Floating leg with null rate should be valid");
    }

    @DisplayName("Should pass when rate is valid for fixed leg")
    @Test
    void shouldPassWhenRateIsValidForFixedLeg() {
        // Test: Fixed leg with a valid rate should be accepted
        TradeLegDTO leg = new TradeLegDTO();
        leg.setLegType("FIXED");
        leg.setRate(2.5); // Example of a valid rate
        TradeLegValidator validator = new TradeLegValidator();
        boolean isValidRate = validator.validateLegRate(leg);
        assertTrue(isValidRate, "Fixed leg with valid rate should be accepted");
    }

    @DisplayName("Should fail when rate has excessive decimal precision for fixed leg")
    @Test
    void shouldFailWhenRateHasExcessiveDecimalPrecisionForFixedLeg() {
        // Test: Fixed leg with more than 4 decimal places in rate should be invalid
        TradeLegDTO leg = new TradeLegDTO();
        leg.setLegType("FIXED");
        leg.setRate(2.123456); // More than 4 decimals
        TradeLegValidator validator = new TradeLegValidator();
        boolean isValidRate = validator.validateLegRate(leg);
        assertFalse(isValidRate, "Fixed leg with excessive decimal precision should be invalid");
    }

}
