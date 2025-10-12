package com.technicalchallenge.validation;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.model.UserProfile;

public class UserPrivilegeValidatorTest {

    @Test
    void traderCanPerformAllTradeActions() {
        // represents the user's role (e.g., TRADER, SALES) and is the main way to
        // identify what actions a user is allowed to perform
        UserProfile trader = new UserProfile();
        // Create a user profile for a trader
        trader.setUserType("TRADER");// sets role to TRADER

        // set up tradedto object with required field
        TradeDTO tradeDTO = new TradeDTO();

        // Instantiate the UserPrivilegeValidator
        UserPrivilegeValidator validator = new UserPrivilegeValidator();

        // sets the trade action to "CREATE". Runs the validation engine to check if the
        // TRADER is allowed to create trades
        tradeDTO.setAction("CREATE");
        TradeValidationResult resultCr = new TradeValidationResult();
        validator.validateUserPrivilege(trader, tradeDTO, resultCr);

        // Checks that the validation result is valid, and the TRADER has permission for
        // to create
        assertTrue(resultCr.isValid(), "TRADER should be able to CREATE trades");

        tradeDTO.setAction("AMEND");
        TradeValidationResult resultAmend = new TradeValidationResult();
        validator.validateUserPrivilege(trader, tradeDTO, resultAmend);
        assertTrue(resultAmend.isValid(), "TRADER should be able to AMEND trades");

        tradeDTO.setAction("TERMINATE");
        TradeValidationResult resultTerminate = new TradeValidationResult();
        validator.validateUserPrivilege(trader, tradeDTO, resultTerminate);
        assertTrue(resultTerminate.isValid(), "TRADER should be able to TERMINATE trades");

        tradeDTO.setAction("CANCEL");
        TradeValidationResult resultCancel = new TradeValidationResult();
        validator.validateUserPrivilege(trader, tradeDTO, resultCancel);
        assertTrue(resultCancel.isValid(), "TRADER should be able to CANCEL trades");

    }

}
