package com.technicalchallenge.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class SettlementInstructionValidatorTest {

    // Field-level validator under test. It enforces length, escaping and
    // character rules for free-text settlement instructions.
    private final SettlementInstructionValidator validator = new SettlementInstructionValidator();

    @Test
    @DisplayName("Valid settlement instructions should pass validation")
    void validInstructions_pass() {
        // Arrange: a fresh validation result and a well-formed instruction.
        TradeValidationResult result = new TradeValidationResult();
        String text = "Please settle by bank transfer \\\"ASAP\\\" with reference 12345.";

        // Act: run the validator which mutates the result object on errors.
        validator.validate(text, result);

        // Assert: no errors were recorded.
        assertTrue(result.isValid(), "Expected valid settlement instructions to pass validation");
    }

    @Test
    @DisplayName("Too-short instructions should fail with length error")
    void tooShort_fails() {
        // Arrange: validation result and an input that's shorter than minimum.
        TradeValidationResult result = new TradeValidationResult();

        // Act
        validator.validate("Short", result);

        // Assert: validator reports a length-related error.
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("between 10 and 500"));
    }

    @Test
    @DisplayName("Instructions containing semicolons should be rejected")
    void semicolonForbidden_fails() {
        // Semicolons are explicitly forbidden by business rules.
        TradeValidationResult result = new TradeValidationResult();

        validator.validate("Payment; send now", result);

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).toLowerCase().contains("semicolon"));
    }

    @Test
    @DisplayName("Unescaped quotes should be rejected")
    void unescapedQuote_fails() {
        // Unescaped quotes are a common source of injection; the validator
        // requires quotes to be escaped with a backslash.
        TradeValidationResult result = new TradeValidationResult();

        validator.validate("Client said " + '"' + "urgent" + '"', result); // unescaped double quotes

        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).toLowerCase().contains("unescaped quote"));
    }

    @Test
    @DisplayName("Escaped quotes should be accepted")
    void escapedQuote_allowed() {
        // Quotes escaped with a backslash are permitted and should not fail validation.
        TradeValidationResult result = new TradeValidationResult();
        validator.validate("Settle note: client said \\\"urgent\\\".", result);
        assertTrue(result.isValid());
    }
}
