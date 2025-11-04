package com.technicalchallenge.validation;

import org.springframework.stereotype.Service;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;

// This class runs all validators and returns results. Acts as the main entry point, clean, testable orchestration of multiple validations for easy maintenance if validations scale
@Service
public class TradeValidationEngine {

    // This engine now validates trade-related business rules and, when
    // available, repository-backed entity status via EntityStatusValidationEngine.
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

        // Run repository-backed entity status validation when available. The
        // EntityStatusValidationEngine is injected in production/integration
        // tests; the no-arg constructor used in lightweight unit tests keeps
        // this field null to avoid requiring DB wiring.
        if (this.entityStatusValidationEngine != null) {
            TradeValidationResult entityResult = this.entityStatusValidationEngine.validate(tradeDTO);
            if (!entityResult.isValid()) {
                for (String err : entityResult.getErrors()) {
                    result.setError(err);
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
     */
    public TradeValidationResult validateSettlementInstructions(String text) {
        TradeValidationResult result = new TradeValidationResult();
        // Delegate to injected field-level validator (uses Spring DI)
        settlementInstructionValidator.validate(text, result);
        return result;
    }

    // Refactored: Replaced direct instantiation of the validator inside the engine.
    // Before: validateSettlementInstructions created a new instance inline.
    private final SettlementInstructionValidator settlementInstructionValidator;

    // Entity status validation engine - repository-backed, strict checks.
    private final EntityStatusValidationEngine entityStatusValidationEngine;

    // Dependency injection (DI): using constructor injection aligns
    // TradeValidationEngine with Spring DI patterns
    public TradeValidationEngine(SettlementInstructionValidator settlementInstructionValidator,
            EntityStatusValidationEngine entityStatusValidationEngine) {
        this.settlementInstructionValidator = settlementInstructionValidator;
        this.entityStatusValidationEngine = entityStatusValidationEngine;
    }

    /**
     * No-arg constructor kept for backwards compatibility in unit tests and
     * situations where Spring DI is not used. It constructs a default
     * SettlementInstructionValidator. Production Spring wiring will use the
     * constructor that accepts the validator and entity engine beans.
     */
    public TradeValidationEngine() {
        this(new SettlementInstructionValidator(), null);
    }

}
