package com.technicalchallenge.validation;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.repository.BookRepository;
import com.technicalchallenge.repository.CounterpartyRepository;
import com.technicalchallenge.repository.ApplicationUserRepository;

/**
 * Validator that ensures referenced entities (book, counterparty, users)
 * exist and are active. This is implemented as a separate component so
 * it can be injected into the validation engine and easily mocked in tests.
 *
 * Behaviour: null-safe and resilient - if a repository is not wired (e.g.
 * in lightweight unit tests) the validator will skip DB checks rather than
 * throwing. This preserves test ergonomics while enforcing rules in
 * integration/production.
 */
@Component
public class EntityStatusValidator {

    private final BookRepository bookRepository;
    private final CounterpartyRepository counterpartyRepository;
    private final ApplicationUserRepository applicationUserRepository;

    public EntityStatusValidator(BookRepository bookRepository, CounterpartyRepository counterpartyRepository,
            ApplicationUserRepository applicationUserRepository) {
        // Repositories are required for strict validation. Fail fast if a
        // repository is missing because we want validation to run in all
        // environments (unit tests that need to avoid DB access should mock
        // the validator or provide repository-backed test fixtures).
        if (bookRepository == null || counterpartyRepository == null || applicationUserRepository == null) {
            throw new IllegalArgumentException("All repositories are required for EntityStatusValidator");
        }

        this.bookRepository = bookRepository;
        this.counterpartyRepository = counterpartyRepository;
        this.applicationUserRepository = applicationUserRepository;
    }

    /**
     * Strict validation: All repository-backed checks are applied. This method
     * intentionally does not short-circuit or skip checks when repositories are
     * missing — callers (tests) should supply mocks or real repositories.
     */
    public void validate(TradeDTO trade, TradeValidationResult result) {
        if (trade == null) {
            result.setError("Trade is null");
            return;
        }

        // Book checks
        if (trade.getBookId() != null) {
            Optional<Book> b = bookRepository.findById(trade.getBookId());
            if (b.isEmpty()) {
                result.setError("Book not found");
            } else if (!b.get().isActive()) {
                result.setError("Book is not active");
            }
        } else if (trade.getBookName() != null) {
            Optional<Book> b = bookRepository.findByBookName(trade.getBookName());
            if (b.isEmpty()) {
                result.setError("Book not found");
            } else if (!b.get().isActive()) {
                result.setError("Book is not active");
            }
        } else {
            result.setError("Book reference is required");
        }

        // Counterparty checks
        if (trade.getCounterpartyId() != null) {
            Optional<Counterparty> c = counterpartyRepository.findById(trade.getCounterpartyId());
            if (c.isEmpty()) {
                result.setError("Counterparty not found");
            } else if (!c.get().isActive()) {
                result.setError("Counterparty is not active");
            }
        } else if (trade.getCounterpartyName() != null) {
            Optional<Counterparty> c = counterpartyRepository.findByName(trade.getCounterpartyName());
            if (c.isEmpty()) {
                result.setError("Counterparty not found");
            } else if (!c.get().isActive()) {
                result.setError("Counterparty is not active");
            }
        } else {
            result.setError("Counterparty reference is required");
        }

        // Trader user checks (traderUserId or traderUserName/loginId)
        if (trade.getTraderUserId() != null) {
            Optional<ApplicationUser> u = applicationUserRepository.findById(trade.getTraderUserId());
            if (u.isEmpty()) {
                result.setError("Trader user not found");
            } else if (!u.get().isActive()) {
                result.setError("Trader user is not active");
            }
        } else if (trade.getTraderUserName() != null) {
            Optional<ApplicationUser> u = applicationUserRepository.findByLoginId(trade.getTraderUserName());
            if (u.isEmpty()) {
                // Attempt first-name lookup as existing code historically did
                Optional<ApplicationUser> u2 = applicationUserRepository.findByFirstName(trade.getTraderUserName());
                if (u2.isEmpty()) {
                    result.setError("Trader user not found");
                } else if (!u2.get().isActive()) {
                    result.setError("Trader user is not active");
                }
            } else if (!u.get().isActive()) {
                result.setError("Trader user is not active");
            }
        } else {
            result.setError("Trader user reference is required");
        }
    }
}
