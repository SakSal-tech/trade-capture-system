package com.technicalchallenge.validation;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.model.UserProfile;

public class UserPrivilegeValidator {

    // Accepts user performing the action, tradeDto which represents the trade and
    // the action being requested and result with validation and errors
    public boolean validateUserPrivilege(UserProfile user, TradeDTO trade, TradeValidationResult result) {

        /*
         * Business Rules. User Privilege Enforcement:
         * TRADER: Can create, amend, terminate, cancel trades
         * SALES: Can create and amend trades only (no terminate/cancel)
         * MIDDLE_OFFICE: Can amend and view trades only
         * SUPPORT: Can view trades only
         * 
         */

        // Which type of user is performing which action
        String userType = user.getUserType();
        String action = trade.getAction();

        /*
         * if (userType.equals("TRADER")) {
         * return true;// Trader can do any action
         * }
         * 
         * // Default not allowed
         * return false;
         */

        // Refactored if statements to switch-case statement for user roles can make
        // code cleaner and easier to read, especially as the number of roles grows
        switch (userType) {
            case "TRADER":
                return true;// Trader can do any action

            case "SALES": // SALES: Can only create and amend trades (cannot terminate or cancel)

                if (!(action.equals("CREATE") || action.equals("AMEND"))) {
                    result.setError(("SALES cannot " + action + " trades"));
                    return false;
                }
                return true;

            case "MIDDLE_OFFICE":// MIDDLE_OFFICE: Can only amend and view trades (cannot create, terminate, or
                // cancel)

                if (!(action.equals("VIEW") || action.equals("AMEND"))) {
                    result.setError(("MIDDLE_OFFICE cannot " + action + " trades"));
                    return false;
                }
                return true;

            case "SUPPORT": // SUPPORT: Can only view trades (cannot create, amend, terminate, or cancel)
                if (!(action.equals("VIEW"))) {
                    result.setError(("SUPPORT cannot " + action + " trades"));
                    return false;
                }
                return true;

            default:
                result.setError("Invalid user type: " + userType);
                return false;

        }

    }

}
