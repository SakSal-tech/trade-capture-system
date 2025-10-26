package com.technicalchallenge.validation;

import org.springframework.stereotype.Service;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;

// This class runs all validators and returns results. Acts as the main entry point, clean, testable orchestration of multiple validations for easy maintenance if validations scale
@Service
public class TradeValidationEngine {

    // This engine now only validates trade-related business rules, not user
    // privileges or entity status.
    public TradeValidationResult validateTradeBusinessRules(TradeDTO tradeDTO) {
        TradeValidationResult result = new TradeValidationResult();
        TradeDateValidator dateValidator = new TradeDateValidator();
        dateValidator.validate(tradeDTO, result);

        // Leg validation setup
        if (tradeDTO.getTradeLegs() != null && !tradeDTO.getTradeLegs().isEmpty()) {
            TradeLegValidator legValidator = new TradeLegValidator();
            legValidator.validateTradeLeg(tradeDTO.getTradeLegs(), result);
            legValidator.validateTradeLegPayReceive(tradeDTO.getTradeLegs(), result);
            for (TradeLegDTO leg : tradeDTO.getTradeLegs()) {
                if (!TradeLegValidator.validateFloatingLegIndex(leg)) {
                    result.setError("Floating legs must have an index specified");
                }
                if (!legValidator.validateLegRate(leg)) {
                    if (leg.getLegType() != null && leg.getLegType().equalsIgnoreCase("FIXED")) {
                        result.setError("Fixed legs must have a valid rate");
                    } else if (leg.getLegType() != null && leg.getLegType().equalsIgnoreCase("FLOATING")) {
                        result.setError("Floating leg rate is invalid");
                    } else {
                        result.setError("Leg rate is invalid");
                    }
                }
            }
        }
        return result;
    }

    /**
     * Added: Delegates settlement-instruction validation to the field-level
     * validator.
     *
     * Rationale: Settlement instructions are a field-level concern (free text)
     * rather than a full TradeDTO validation. Providing a single entry point on
     * the validation engine keeps validation orchestration consistent across the
     * codebase and makes future additions easier.
     *
     * This method intentionally instantiates the field validator directly to
     * avoid changing bean wiring elsewhere; it adapts the existing validator to
     * the engine's orchestration model.
     */
    public TradeValidationResult validateSettlementInstructions(String text) {
        TradeValidationResult result = new TradeValidationResult();
        // Field-level validator handles trimming, length and character rules
        new SettlementInstructionValidator().validate(text, result);
        return result;
    }
    // Adding this line to force commit

}
