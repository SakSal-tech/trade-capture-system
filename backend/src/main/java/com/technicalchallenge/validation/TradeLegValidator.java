package com.technicalchallenge.validation;

import java.util.List;

import com.technicalchallenge.dto.TradeLegDTO;

public class TradeLegValidator {

    public boolean validateTradeLeg(List<TradeLegDTO> tradeLegs, TradeValidationResult result) {

        // Length check validation to avoid crashes if legs are less than 2. A trade
        // must have 2 legs
        if (tradeLegs == null || tradeLegs.size() < 2) {
            result.setError("Trade must have at least two legs");
            return false;
        }

        // Extract legs
        TradeLegDTO leg1 = tradeLegs.get(0);
        TradeLegDTO leg2 = tradeLegs.get(1);

        // Check null leg maturitydate
        if (leg1.getTradeMaturityDate() == null || leg2.getTradeMaturityDate() == null) {
            result.setError("Both legs must have a maturity date defined");
            return false;
        }

        // Compare maturity dates
        if ((leg1.getTradeMaturityDate()).isBefore(leg2.getTradeMaturityDate())
                || (leg1.getTradeMaturityDate()).isAfter(leg2.getTradeMaturityDate())) {
            result.setError("Both legs must have identical maturity dates");
            return false;

        }
        // If none of the validations fail.
        return true;
    }
}
