package com.technicalchallenge.validation;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Counterparty;

/**
 * Engine for validating entity status and references for trades.
 * Keeps entity validation separate from business rule validation.
 */
public class EntityStatusValidationEngine {

    private final EntityStatusValidator entityValidator = new EntityStatusValidator();

    /**
     * Validates that user, book, and counterparty are active.
     */
    public boolean validateEntityStatus(ApplicationUser user, Book book, Counterparty counterparty,
            TradeValidationResult result) {
        return entityValidator.validateEntityStatus(user, book, counterparty, result);
    }

    /**
     * Validates that trade references (book, counterparty) exist and are valid.
     */
    public boolean validateEntityReferences(TradeDTO trade, TradeValidationResult result) {
        return entityValidator.validateEntityReferences(trade, result);
    }
}
