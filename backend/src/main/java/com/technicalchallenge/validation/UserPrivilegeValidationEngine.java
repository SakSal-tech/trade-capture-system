package com.technicalchallenge.validation;

import org.springframework.stereotype.Component;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.model.UserProfile;

// This engine is responsible for user privilege validation only
@Component
public class UserPrivilegeValidationEngine {

    public TradeValidationResult validateUserPrivilegeBusinessRules(TradeDTO tradeDTO, UserProfile userProfile) {
        TradeValidationResult result = new TradeValidationResult();
        UserPrivilegeValidator privilegeValidator = new UserPrivilegeValidator();
        privilegeValidator.validateUserPrivilege(userProfile, tradeDTO, result);
        return result;
    }
}
