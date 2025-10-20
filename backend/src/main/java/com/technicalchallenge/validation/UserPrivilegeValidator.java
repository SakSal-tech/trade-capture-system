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

        // I am adding basic null/blank guards and normalisation here so that callers
        // don’t get
        // surprising outcomes just because of casing or whitespace. If the action is
        // missing,
        // I fail closed and record a clear error message for the caller to surface as
        // 403.
        if (result == null) {
            // I prefer to fail fast if the result bucket is null, as the engine expects
            // error messages.
            throw new IllegalArgumentException("TradeValidationResult must not be null");
        }
        if (userType == null || userType.isBlank()) {
            result.setError("User type is required");
            return false;
        }
        if (action == null || action.isBlank()) {
            result.setError("Action is required");
            return false;
        }

        // I am normalising both userType and action so comparisons are reliable
        // regardless of input casing.
        String normUserType = userType.trim().toUpperCase();
        String normAction = action.trim().toUpperCase();

        switch (normUserType) {
            case "TRADER":
                // Trader can do any action
                return true;

            case "SALES":
                // SALES: Can only create and amend trades
                if (!(normAction.equals("CREATE") || normAction.equals("AMEND"))) {
                    result.setError(("SALES cannot " + normAction + " trades"));
                    return false;
                }
                return true;

            case "MIDDLE_OFFICE":
                // MIDDLE_OFFICE: Can only amend and view trades
                if (!(normAction.equals("VIEW") || normAction.equals("AMEND"))) {
                    result.setError(("MIDDLE_OFFICE cannot " + normAction + " trades"));
                    return false;
                }
                return true;

            case "SUPPORT":
                // SUPPORT: Can only view trades
                if (!(normAction.equals("VIEW"))) {
                    result.setError(("SUPPORT cannot " + normAction + " trades"));
                    return false;
                }
                return true;

            case "BOOK_VIEW":
                // BOOK_VIEW: Can only view trades (book-level aggregation)
                if (!(normAction.equals("VIEW"))) {
                    result.setError(("BOOK_VIEW cannot " + normAction + " trades"));
                    return false;
                }
                return true;

            case "TRADE_CANCEL":
                // TRADE_CANCEL: Can only cancel trades
                if (!(normAction.equals("CANCEL"))) {
                    result.setError(("TRADE_CANCEL cannot " + normAction + " trades"));
                    return false;
                }
                return true;

            case "TRADE_EDIT":
                // TRADE_EDIT: Can only amend trades
                if (!(normAction.equals("AMEND"))) {
                    result.setError(("TRADE_EDIT cannot " + normAction + " trades"));
                    return false;
                }
                return true;

            case "TRADE_DELETE":
                // TRADE_DELETE: Can only delete trades
                if (!(normAction.equals("DELETE"))) {
                    result.setError(("TRADE_DELETE cannot " + normAction + " trades"));
                    return false;
                }
                return true;

            case "TRADE_CREATE":
                // TRADE_CREATE: Can only create trades
                if (!(normAction.equals("CREATE"))) {
                    result.setError(("TRADE_CREATE cannot " + normAction + " trades"));
                    return false;
                }
                return true;

            case "TRADE_TERMINATE":
                // TRADE_TERMINATE: Can only terminate trades
                if (!(normAction.equals("TERMINATE"))) {
                    result.setError(("TRADE_TERMINATE cannot " + normAction + " trades"));
                    return false;
                }
                return true;

            case "TRADE_VIEW":
                // TRADE_VIEW: Can only view trades
                if (!(normAction.equals("VIEW"))) {
                    result.setError(("TRADE_VIEW cannot " + normAction + " trades"));
                    return false;
                }
                return true;

            default:
                result.setError("Invalid user type: " + userType);
                return false;
        }

    }
    // Adding this line to force commit

}
