package com.technicalchallenge.validation;

import java.math.BigDecimal;
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

        // Fix: Null check before calling .equals() in the validation method as test
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

    // --- Floating leg index validation ---
    /**
     * Checks if a floating leg has a valid index specified.
     * Returns true if leg is not floating, or if floating and indexId/indexName are
     * present.
     */
    public static boolean validateFloatingLegIndex(TradeLegDTO leg) {
        if (leg == null)
            return false;
        /*
         * Trade Business Rules:
         * FIXED leg: Pays a fixed interest rate. FLOATING leg: Pays a variable interest
         * rate, which changes over time. Index: For a floating leg, the interest rate
         * is not fixed, it's based on a financial index (like LIBOR, EURIBOR,
         * SONIA,etc.Cannot have a floating leg without specifying which index it uses
         */
        String legType = leg.getLegType();
        // If the leg type is "FLOATING", then the code checks that both an index ID and
        // index name are provided and that the index name is not blank
        if (legType != null && legType.equalsIgnoreCase("FLOATING")) {
            return leg.getIndexId() != null && leg.getIndexName() != null && !leg.getIndexName().isBlank();
        }
        return true;
    }

    public boolean validateLegRate(TradeLegDTO leg) {
        // Null check for leg object
        if (leg == null) {
            return false;
        }

        String legType = leg.getLegType();
        Double rate = leg.getRate();

        // Validation for FIXED leg type
        if (legType != null && legType.equalsIgnoreCase("FIXED")) {
            // Rate must not be null
            if (rate == null) {
                return false;
            }
            // Rate must be greater than zero
            if (rate <= 0) {
                return false;
            }
            // Rate must not be unrealistically high (e.g., > 100%)
            if (rate > 100) {
                return false;
            }
            // Check for excessive decimal precision (e.g., more than 4 decimals)
            BigDecimal rateDecimal = BigDecimal.valueOf(rate);
            if (rateDecimal.scale() > 4) {
                return false;
            }
            return true;
        }

        // Validation for FLOATING leg type
        if (legType != null && legType.equalsIgnoreCase("FLOATING")) {
            // Rate should typically be null for floating legs
            if (rate != null && rate != 0.0) {
                // If business rules require floating leg rate to be null, fail if not null
                // return false;
                // If floating leg rate is allowed to be set, pass
                // For now, allow null or zero
                return true;
            }
            return true;
        }

        // For other leg types, allow null or positive rate
        if (rate == null || rate < 0) {
            return false;
        }
        return true;
    }
    // Adding this line to force commit

}