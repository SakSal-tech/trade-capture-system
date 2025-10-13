package com.technicalchallenge.validation;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Counterparty;

public class EntityStatusValidatorTest {

    private EntityStatusValidator validator;
    private TradeValidationResult result;
    private TradeDTO trade;
    private ApplicationUser user;
    private Book book;
    private Counterparty counterparty;

    @BeforeEach
    void setUp() {
        validator = new EntityStatusValidator();
        result = new TradeValidationResult();
        trade = new TradeDTO();
        user = new ApplicationUser();
        book = new Book();
        counterparty = new Counterparty();
    }

    @DisplayName("Should fail when user, book, or counterparty is inactive")
    @Test
    void shouldFailWhenAnyEntityIsInactive() {
        // GIVEN an inactive user, book, and counterparty
        user.setActive(false);
        book.setActive(true);
        counterparty.setActive(true);

        // WHEN validation runs
        boolean isValid = validator.validateEntityStatus(user, book, counterparty, result);

        // THEN the validation should fail and show the correct error
        assertFalse(isValid);
        assertTrue(result.getErrors().contains("ApplicationUser must be active"));
    }

    @DisplayName("Should fail when reference IDs are missing")
    @Test
    void shouldFailWhenEntityReferencesMissing() {
        // GIVEN a trade missing required references
        trade.setBookId(null);
        trade.setCounterpartyId(null);

        // WHEN reference validation runs
        boolean isValid = validator.validateEntityReferences(trade, result);

        // THEN validation should fail due to missing reference data
        assertFalse(isValid);
        assertTrue(result.getErrors().contains("Missing both book and counterparty reference"));
    }

    @DisplayName("Should pass when all references exist and entities are active")
    @Test
    void shouldPassWhenAllEntitiesAreValid() {
        // GIVEN properly linked and active entities
        trade.setBookId(100L);
        trade.setCounterpartyId(200L);
        user.setActive(true);
        book.setActive(true);
        counterparty.setActive(true);

        // WHEN both validations run
        boolean referencesValid = validator.validateEntityReferences(trade, result);
        boolean statusValid = validator.validateEntityStatus(user, book, counterparty, result);

        // THEN all should pass
        assertTrue(referencesValid);
        assertTrue(statusValid);
        assertTrue(result.isValid());
    }
}
