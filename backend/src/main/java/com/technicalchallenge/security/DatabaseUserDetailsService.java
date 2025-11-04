package com.technicalchallenge.security;

import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.model.Privilege;
import com.technicalchallenge.model.UserPrivilege;
import com.technicalchallenge.service.ApplicationUserService;
import com.technicalchallenge.service.UserPrivilegeService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// DatabaseUserDetailsService
//
// Purpose:
// To load application users from the database and convert their profile
// and privileges into Spring Security authorities so @PreAuthorize
// expressions can be evaluated against real, DB-stored users.
//
// Mapping convention:
// - Map the user's profile (userProfile.userType) to a ROLE_ authority.
//   Example: userProfile.userType == "TRADER" -> "ROLE_TRADER"
//
// - Map each database privilege name (for example "TRADE_VIEW") to a
//   plain authority of the same name ("TRADE_VIEW"). This allows the
//   application to use either hasRole('TRADER') or hasAuthority('TRADE_VIEW').
//
// - UsernameNotFoundException is thrown when a loginId is missing or the user
//   is inactive.
// - Null or missing profile/privileges are handled safely by returning an
//   authority set that may be empty.
// - Password handling assumes the stored password matches the configured
//   PasswordEncoder (for development, {noop} may be used).
//
// - Prefer adding a direct query such as getByUserId in UserPrivilegeService
//   for efficient privilege lookup. If only getAllUserPrivileges() exists,
//   the implementation filters the results for this user.

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseUserDetailsService.class);

    private final ApplicationUserService applicationUserService;
    private final UserPrivilegeService userPrivilegeService;

    public DatabaseUserDetailsService(ApplicationUserService applicationUserService,
            UserPrivilegeService userPrivilegeService) {
        this.applicationUserService = applicationUserService;
        this.userPrivilegeService = userPrivilegeService;
    }

    /**
     * Load a user by username (loginId).
     *
     * @param username the login id stored in ApplicationUser.loginId
     * @return UserDetails for Spring Security
     * @throws UsernameNotFoundException when the user is not present or inactive
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Look up the ApplicationUser by loginId using the application service
        Optional<ApplicationUser> maybe = applicationUserService.getUserByLoginId(username);

        // Throw UsernameNotFoundException if the user is not present
        ApplicationUser appUser = maybe.orElseThrow(
                () -> new UsernameNotFoundException("User not found: " + username));

        // If the user record is inactive, treat as not found / disabled
        if (Boolean.FALSE.equals(appUser.isActive())) {
            throw new UsernameNotFoundException("User is not active: " + username);
        }

        // Log basic debug info about the loaded user (mask the password for safety)
        // These debug logs were added to help diagnose 403 / AccessDenied
        // problems during development. They show the stored (masked) password and
        // the resulting GrantedAuthority set created for the user. An empty
        // authority set or an unexpected stored value is a likely reason why
        // method-level security checks refuse access.
        String stored = appUser.getPassword();
        String masked = (stored == null) ? "<null>" : (stored.length() <= 6 ? "***" : stored.substring(0, 4) + "***");
        logger.debug("Loaded user '{}' (id={}) active={} password={}", username, appUser.getId(), appUser.isActive(),
                masked);

        // Create an empty set to collect GrantedAuthority instances
        Set<GrantedAuthority> authorities = new HashSet<>();

        // If a user profile exists, map it to a ROLE_ authority and add it
        if (appUser.getUserProfile() != null && appUser.getUserProfile().getUserType() != null) {
            String roleName = "ROLE_" + appUser.getUserProfile().getUserType().trim().toUpperCase();
            authorities.add(new SimpleGrantedAuthority(roleName));
            // Also add a normalized role when user types encode a more specific
            // role such as TRADER_SALES. Many @PreAuthorize checks expect the
            // more generic ROLE_TRADER or ROLE_MIDDLE_OFFICE. Add the generic
            // forms so both the specific and generic checks succeed.
            String userTypeUpper = appUser.getUserProfile().getUserType().trim().toUpperCase();
            if (userTypeUpper.contains("TRADER")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_TRADER"));
            }
            // Normalize common short codes to the expected role name
            if ("MO".equals(userTypeUpper) || userTypeUpper.contains("MIDDLE")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_MIDDLE_OFFICE"));
            }
            if (userTypeUpper.contains("SUPPORT")) {
                authorities.add(new SimpleGrantedAuthority("ROLE_SUPPORT"));
            }
        }

        // Attempt to collect privilege-based authorities for this user
        try {
            // Retrieve privilege links for this user specifically to avoid
            // loading the entire user_privilege table into memory. Use the
            // helper on UserPrivilegeService which performs a focused query
            // by loginId (backed by a repository finder). This speeds startup
            // and lowers memory pressure in production. // ADDED: Replace broad scan with
            // precise finder
            List<UserPrivilege> userPrivileges = userPrivilegeService
                    .findPrivilegesByUserLoginId(appUser.getLoginId());

            // Map to privilege names; the finder already scopes to this user
            List<String> names = userPrivileges.stream().filter(Objects::nonNull)
                    .map(UserPrivilege::getPrivilege) // get Privilege object
                    .filter(Objects::nonNull) // skip if no Privilege
                    .map(Privilege::getName) // privilege name string
                    .filter(Objects::nonNull) // skip null names
                    .map(String::trim) // trim whitespace
                    .map(String::toUpperCase) // normalize case
                    .collect(Collectors.toList());

            // Convert privilege names into GrantedAuthority objects. Also add
            // commonly expected alias authorities so controller-level
            // expressions that check for TRADE_VIEW (or other canonical names)
            // succeed when the DB stores a slightly different name such as
            // READ_TRADE.
            names.forEach(n -> {
                authorities.add(new SimpleGrantedAuthority(n));
                // Alias READ_TRADE -> TRADE_VIEW to match existing @PreAuthorize
                // expressions that expect TRADE_VIEW
                if ("READ_TRADE".equals(n)) {
                    authorities.add(new SimpleGrantedAuthority("TRADE_VIEW"));
                }
                // Additional aliases may be added here if the DB uses different
                // names for privileges than the security expressions expect.
            });
        } catch (Exception e) {
            // On error, continue with whatever authorities already have
        }

        // Log the computed authorities for debugging authorization failures
        logger.debug("Authorities for user '{}': {}", username,
                authorities.stream().map(Object::toString).collect(Collectors.joining(",")));

        // Build and return a Spring Security UserDetails with username,
        // stored password, authorities, and account flags
        return User.builder()
                // set username for authentication
                .username(appUser.getLoginId())
                // set stored password (must match PasswordEncoder expectations)
                .password(appUser.getPassword() == null ? "" : appUser.getPassword())
                // set computed authorities (roles + privileges)
                .authorities(authorities)
                // account expiration/lock flags
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                // disabled flag mapped from domain isActive
                .disabled(!Boolean.TRUE.equals(appUser.isActive()))
                .build();
    }
}