package com.technicalchallenge.security;

import com.technicalchallenge.model.Trade;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Encapsulates ownership and privilege checks for trade-related actions.
 *
 * Purpose: centralise the logic that determines whether the authenticated
 * principal may view or modify a trade. This avoids duplicating role/owner
 * checks across services.
 */
@Component("securityUserPrivilegeValidator")
public class UserPrivilegeValidator {

    /**
     * Returns true when the caller is allowed to view the given trade.
     */
    public boolean canViewTrade(Trade trade, Authentication auth) {
        if (trade == null)
            return false;
        String currentUser = (auth != null && auth.getName() != null) ? auth.getName() : "__UNKNOWN__";

        boolean canViewOthers = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                .anyMatch(a -> {
                    String ga = a.getAuthority();
                    return "ROLE_SALES".equalsIgnoreCase(ga) || "ROLE_SUPERUSER".equalsIgnoreCase(ga)
                            || "TRADE_VIEW_ALL".equalsIgnoreCase(ga) || "ROLE_MIDDLE_OFFICE".equalsIgnoreCase(ga)
                            || "ROLE_SUPPORT".equalsIgnoreCase(ga);
                });

        String ownerLogin = (trade.getTraderUser() != null && trade.getTraderUser().getLoginId() != null)
                ? trade.getTraderUser().getLoginId()
                : null;

        if (ownerLogin == null)
            return canViewOthers; // no owner set: only elevated roles allowed
        return canViewOthers || ownerLogin.equalsIgnoreCase(currentUser);
    }

    /**
     * Returns true when the caller is allowed to edit the given trade.
     */
    public boolean canEditTrade(Trade trade, Authentication auth) {
        if (trade == null)
            return false;
        String currentUser = (auth != null && auth.getName() != null) ? auth.getName() : "__UNKNOWN__";

        boolean canEditOthers = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                .anyMatch(a -> {
                    String ga = a.getAuthority();
                    return "ROLE_SALES".equalsIgnoreCase(ga) || "ROLE_SUPERUSER".equalsIgnoreCase(ga)
                            || "TRADE_EDIT_ALL".equalsIgnoreCase(ga);
                });

        String ownerLogin = (trade.getTraderUser() != null && trade.getTraderUser().getLoginId() != null)
                ? trade.getTraderUser().getLoginId()
                : null;

        if (ownerLogin == null)
            return canEditOthers;
        return canEditOthers || ownerLogin.equalsIgnoreCase(currentUser);
    }
}
