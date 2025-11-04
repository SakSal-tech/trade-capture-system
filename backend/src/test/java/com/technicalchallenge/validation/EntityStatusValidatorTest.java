package com.technicalchallenge.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.repository.ApplicationUserRepository;
import com.technicalchallenge.repository.BookRepository;
import com.technicalchallenge.repository.CounterpartyRepository;

public class EntityStatusValidatorTest {

    private EntityStatusValidator validator;
    private TradeValidationResult result;
    private TradeDTO trade;
    private ApplicationUser user;
    private Book book;
    private Counterparty counterparty;

    private BookRepository bookRepo;
    private CounterpartyRepository counterpartyRepo;
    private ApplicationUserRepository userRepo;

    @BeforeEach
    void setUp() {
        // Create lightweight model objects
        trade = new TradeDTO();
        user = new ApplicationUser();
        book = new Book();
        counterparty = new Counterparty();

        // Mock repositories and inject into strict validator
        bookRepo = mock(BookRepository.class);
        counterpartyRepo = mock(CounterpartyRepository.class);
        userRepo = mock(ApplicationUserRepository.class);

        validator = new EntityStatusValidator(bookRepo, counterpartyRepo, userRepo);
        result = new TradeValidationResult();
    }

    @DisplayName("Should fail when user is inactive")
    @Test
    void shouldFailWhenUserIsInactive() {
        // GIVEN an inactive user referenced by id
        user.setActive(false);
        trade.setTraderUserId(3L);
        when(userRepo.findById(anyLong())).thenReturn(Optional.of(user));

        // WHEN validation runs
        validator.validate(trade, result);

        // THEN the validation should record the trader user inactive error
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Trader user is not active")));
    }

    @DisplayName("Should fail when reference IDs are missing")
    @Test
    void shouldFailWhenEntityReferencesMissing() {
        // GIVEN a trade missing required references
        trade.setBookId(null);
        trade.setCounterpartyId(null);

        // WHEN validation runs
        validator.validate(trade, result);

        // THEN validation should fail due to missing reference data
        assertFalse(result.isValid());
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Book reference is required")));
        assertTrue(result.getErrors().stream().anyMatch(e -> e.contains("Counterparty reference is required")));
    }

    @DisplayName("Should pass when all references exist and entities are active")
    @Test
    void shouldPassWhenAllEntitiesAreValid() {
        // GIVEN properly linked and active entities
        trade.setBookId(100L);
        trade.setCounterpartyId(200L);
        trade.setTraderUserId(300L);
        user.setActive(true);
        book.setActive(true);
        counterparty.setActive(true);

        when(bookRepo.findById(100L)).thenReturn(Optional.of(book));
        when(counterpartyRepo.findById(200L)).thenReturn(Optional.of(counterparty));
        when(userRepo.findById(300L)).thenReturn(Optional.of(user));

        // WHEN validation runs
        validator.validate(trade, result);

        // THEN validation should be valid
        assertTrue(result.isValid());
    }

}
