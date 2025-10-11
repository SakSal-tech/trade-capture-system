package com.technicalchallenge.service;

import com.technicalchallenge.dto.TradeDTO;

public class TradeValidationService {

    public TradeValidation validateTradeBusinessRules(TradeDTO trade) {

        return new TradeValidation();
    }

}
