package com.technicalchallenge.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.model.UserProfile;

public class UserPrivilegeValidatorTest {

    // Helper method to reduce repeated validation logic for different actions.
    // Refactored for clarity and maintainability.
    private void assertTraderCanPerformAction(UserProfile trader, TradeDTO tradeDTO, String action) {
        // sets the trade action and runs the validation engine to check if the TRADER
        // is allowed to perform the action
        tradeDTO.setAction(action);
        TradeValidationResult result = new TradeValidationResult();
        UserPrivilegeValidator validator = new UserPrivilegeValidator();
        validator.validateUserPrivilege(trader, tradeDTO, result);
        // Checks that the validation result is valid, and the TRADER has permission for
        // the action
        assertTrue(result.isValid(), "TRADER should be able to " + action + " trades");
    }

    @DisplayName("Trader can perform all trade actions")
    @Test
    void traderCanPerformAllTradeActions() {
        // represents the user's role (e.g., TRADER, SALES) and is the main way to
        // identify what actions a user is allowed to perform
        UserProfile trader = new UserProfile();
        // Create a user profile for a trader
        trader.setUserType("TRADER"); // sets role to TRADER

        // set up tradedto object with required field
        TradeDTO tradeDTO = new TradeDTO();

        // Refactored: use helper method to reduce repeated code for each action
        assertTraderCanPerformAction(trader, tradeDTO, "CREATE");
        assertTraderCanPerformAction(trader, tradeDTO, "AMEND");
        assertTraderCanPerformAction(trader, tradeDTO, "TERMINATE");
        assertTraderCanPerformAction(trader, tradeDTO, "CANCEL");
    }

}
