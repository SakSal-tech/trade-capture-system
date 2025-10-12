package com.technicalchallenge.validation;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;

/*Cross-Leg Business Rules:
    Both legs must have identical maturity dates
    Legs must have opposite pay/receive flags
    Floating legs must have an index specified
    Fixed legs must have a valid rate
 */

public class TradeLegValidatorTest {
    @DisplayName("Should fail when two trade legs have different maturity dates")

    @Test
    void shouldFailWhenLegMaturityDatesDiffer() {
        // GIVENGIVEN two trade legs with different maturity dates
        TradeLegDTO leg1 = new TradeLegDTO();
        leg1.setTradeMaturityDate(LocalDate.of(2025, 10, 10));

        TradeLegDTO leg2 = new TradeLegDTO();
        leg2.setTradeMaturityDate(LocalDate.of(2025, 11, 11));

        List<TradeLegDTO> tradeLegs = List.of(leg1, leg2);

        TradeValidationResult result = new TradeValidationResult();

        TradeLegValidator validator = new TradeLegValidator();

        // WHEN the validator checks the legs

        validator.validateTradeLeg(tradeLegs, result);
        // THEN the validation should fail and return an appropriate error message
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Both legs must have identical maturity dates"));

    }

}
