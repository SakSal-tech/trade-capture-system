package com.technicalchallenge.validation;

import org.springframework.stereotype.Component;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.model.UserProfile;

// This engine is responsible for user privilege validation only
@Component
public class UserPrivilegeValidationEngine {
    private final UserPrivilegeValidator privilegeValidator;

    public UserPrivilegeValidationEngine(UserPrivilegeValidator privilegeValidator) {
        this.privilegeValidator = privilegeValidator; // ADDED: inject validator to improve testability and avoid direct
                                                      // new() usage
    }

    public TradeValidationResult validateUserPrivilegeBusinessRules(TradeDTO tradeDTO, UserProfile userProfile) {
        TradeValidationResult result = new TradeValidationResult();
        // Use the injected validator rather than creating a new instance
        privilegeValidator.validateUserPrivilege(userProfile, tradeDTO, result);
        return result;
    }

}
