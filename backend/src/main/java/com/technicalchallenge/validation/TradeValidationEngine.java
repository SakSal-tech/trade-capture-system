package com.technicalchallenge.validation;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;

// This class runs all validators and returns results. Acts as the main entry point, clean, testable orchestration of multiple validations for easy maintenance if validations scale
public class TradeValidationEngine {

    // Refactored: This engine now only validates trade-related business rules, not
    // user privileges.
    // User privilege validation has been moved to UserPrivilegeValidationEngine for
    // separation of concerns.
    public TradeValidationResult validateTradeBusinessRules(TradeDTO tradeDTO) {
        // This method will call each validator:
        // - dateValidator.validate(trade, result);
        TradeValidationResult result = new TradeValidationResult();
        TradeDateValidator dateValidator = new TradeDateValidator();
        dateValidator.validate(tradeDTO, result);

        // Leg validation setup
        // Only validate legs if tradeLegs are present
        if (tradeDTO.getTradeLegs() != null && !tradeDTO.getTradeLegs().isEmpty()) {
            TradeLegValidator legValidator = new TradeLegValidator();
            // Validate leg maturity dates
            legValidator.validateTradeLeg(tradeDTO.getTradeLegs(), result);
            // Validate pay/receive flags
            legValidator.validateTradeLegPayReceive(tradeDTO.getTradeLegs(), result);

            // For each leg, checks if it is a floating leg and, if so, whether it has a
            // valid index specified
            for (TradeLegDTO leg : tradeDTO.getTradeLegs()) {
                if (!TradeLegValidator.ValidateFloatingLegIndex(leg)) {
                    result.setError("Floating legs must have an index specified");
                }
            }
        }
        // - entityValidator.validate(...);

        return result;

    }

}
