package com.technicalchallenge.validation;

import com.technicalchallenge.dto.TradeDTO;

// This class run all validators and return results. Acts as the main entry point, clean, testable orchestration of multiple validations for easy maintanence if validations scale
public class TradeValidationEngine {

    public TradeValidationResult validateTradeBusinessRules(TradeDTO trade) {
        // This method will call each validator:
        // - dateValidator.validate(trade, result);
        TradeValidationResult result = new TradeValidationResult();
        TradeDateValidator dateValidator = new TradeDateValidator();
        dateValidator.validate(trade, result);
        return result;

        // - privilegeValidator.validate(...);
        // - legValidator.validate(...);
        // - entityValidator.validate(...);

    }

}
