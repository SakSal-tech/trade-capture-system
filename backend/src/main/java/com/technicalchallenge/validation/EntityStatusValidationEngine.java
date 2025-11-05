package com.technicalchallenge.validation;

import org.springframework.stereotype.Component;

import com.technicalchallenge.dto.TradeDTO;

/**
 * Adapter/engine that delegates to the repository-backed
 * {@link EntityStatusValidator}. This class expects the validator to be a
 * Spring-managed bean and therefore relies on constructor injection.
 */
@Component
public class EntityStatusValidationEngine {

    private final EntityStatusValidator entityValidator;

    public EntityStatusValidationEngine(EntityStatusValidator entityValidator) {
        this.entityValidator = entityValidator;
    }

    /**
     * Validate a TradeDTO's entity references and return the validation result.
     */
    public TradeValidationResult validate(TradeDTO trade) {
        TradeValidationResult result = new TradeValidationResult();
        entityValidator.validate(trade, result);
        return result;
    }

}
