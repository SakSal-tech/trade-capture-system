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

        // Refactored: Instead of checking both isBefore and isAfter, I changed to
        // avoids unnecessary comparisons.
        // checking for equalityCompare maturity dates
        if (leg1.getTradeMaturityDate().isBefore(leg2.getTradeMaturityDate())) {
            result.setError("Both legs must have identical maturity dates");
            return false;
        }
        if (!leg1.getTradeMaturityDate().equals(leg2.getTradeMaturityDate())) {
            result.setError("Both legs must have identical maturity dates");
            return false;
        }
        return true;// Always return true if no error was found
    }

    // validate the pay/receive flags of trade legs in a financial trade
    public boolean validateTradeLegPayReceive(List<TradeLegDTO> tradeLegs, TradeValidationResult result) {

        // Length check validation to avoid crashes if legs are less than 2. A trade
        // must have 2 legs
        if (tradeLegs == null || tradeLegs.size() < 2) {
            result.setError("Trade must have at least two legs");
            return false;
        }

        // Extract legs
        // Extract legs
        TradeLegDTO leg1 = tradeLegs.get(0);
        TradeLegDTO leg2 = tradeLegs.get(1);

        leg1 = tradeLegs.get(0);
        leg2 = tradeLegs.get(1);

        // Fix: Null check before calling .equals() in your validation method as test
        // was failing
        if (leg1.getPayReceiveFlag() == null || leg2.getPayReceiveFlag() == null) {
            result.setError("Both legs must have a pay/receive flag defined");
            return false;
        }
        if (leg1.getPayReceiveFlag().equals(leg2.getPayReceiveFlag())) {
            if (leg1.getPayReceiveFlag() == null || leg2.getPayReceiveFlag() == null) {
                result.setError("Both legs must have a pay/receive flag defined");
                return false;
            }
            if (leg1.getPayReceiveFlag().equals(leg2.getPayReceiveFlag())) {
                result.setError("Legs must have opposite pay/receive flags");
                return false;
            }

        }
        return true;// Always return true if no error was found

    }
}
