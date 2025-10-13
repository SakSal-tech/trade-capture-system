package com.technicalchallenge.validation;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Counterparty;

/**
 * Validates the status and references of entities involved in trade operations.
 * This class provides methods to check whether ApplicationUser, Book, and
 * Counterparty
 * entities are active, and to ensure that TradeDTO objects have valid
 * references(Referential Integrity).
 */
public class EntityStatusValidator {

    // Active status
    public boolean validateEntityStatus(ApplicationUser user, Book book, Counterparty counterparty,
            TradeValidationResult result) {
        if (user != null && !user.isActive()) {
            result.setError("ApplicationUser must be active");
            return false;
        }
        if (book != null && !book.isActive()) {
            result.setError("Book entity must be active");
            return false;
        }
        if (counterparty != null && !counterparty.isActive()) {
            result.setError("Counterparty entity must be active");
            return false;
        }
        return true;
    }

    // Existence & Referential integrity
    public boolean validateEntityReferences(TradeDTO trade, TradeValidationResult result) {
        if (trade.getBookId() == null || trade.getCounterpartyId() == null) {
            if (trade.getBookId() == null && trade.getCounterpartyId() == null) {
                result.setError("Missing both book and counterparty reference");
            } else if (trade.getBookId() == null) {
                result.setError("Missing book reference");
            } else {
                result.setError("Missing counterparty reference");
            }
            return false;
        }
        return true;
    }

}
