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

    @Test
    void testFloatingLegRequiresIndex() {
        // Floating leg with index specified (valid)
        TradeLegDTO floatingLegValid = new TradeLegDTO();
        floatingLegValid.setLegType("FLOATING");
        floatingLegValid.setIndexId(1L);
        floatingLegValid.setIndexName("LIBOR");

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

        // Assume isFloatingLegIndexValid is your validation method
        assertTrue(TradeLegValidator.ValidateFloatingLegIndex(floatingLegValid),
                "Floating leg with index should be valid");
        assertFalse(TradeLegValidator.ValidateFloatingLegIndex(floatingLegNoIndex),
                "Floating leg without index should be invalid");
        assertTrue(TradeLegValidator.ValidateFloatingLegIndex(fixedLeg),
                "Fixed leg should be valid even without index");
    }
}
