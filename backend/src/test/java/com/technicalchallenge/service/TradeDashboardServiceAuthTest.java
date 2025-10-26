package com.technicalchallenge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.technicalchallenge.model.UserPrivilege;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.validation.UserPrivilegeValidationEngine;

/*
 * Focused authentication/authorization unit tests for TradeDashboardService.
 *
 * - This test class isolates the privilege-checking behaviour (the
 *   service-level `hasPrivilege(...)` logic) so can validate short-circuit
 *   rules (SecurityContext authorities) and the DB-backed privilege fallback.
 * - The service now uses a deny-by-default approach and requires a
 *   mocked UserPrivilegeService for DB lookups in unit tests. Tests must
 *   explicitly provide the privilege service behavior instead of relying on
 *   any permissive null-service shortcuts.
 */
public class TradeDashboardServiceAuthTest {

    private TradeDashboardService dashboardService;

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private TradeMapper tradeMapper;

    @Mock
    private UserPrivilegeService userPrivilegeService;

    @Mock
    private UserPrivilegeValidationEngine privilegeValidationEngine;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        // default: inject mocks into service
        dashboardService = new TradeDashboardService(tradeRepository, tradeMapper, userPrivilegeService,
                privilege_validation_engine());
        // clear security context before each test
        SecurityContextHolder.clearContext();
    }

    // helper to satisfy constructor when want a null engine (useful for test)
    private UserPrivilegeValidationEngine privilege_validation_engine() {
        return privilegeValidationEngine;
    }

    @Test
    void whenSecurityContextHasRoleTrader_thenHasPrivilegePermits() throws Exception {
        // Set authentication with ROLE_TRADER authority
        TestingAuthenticationToken auth = new TestingAuthenticationToken("alice", null, "ROLE_TRADER");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // hasPrivilege is private; call public method that uses it: filterTrades will
        // exercise hasPrivilege
        // stub repository to return empty page and mapper
        assertDoesNotThrow(() -> dashboardService.searchTrades(new com.technicalchallenge.dto.SearchCriteriaDTO()));
    }

    @Test
    void whenSecurityContextHasAuthorityTradeView_thenPermits() throws Exception {
        TestingAuthenticationToken auth = new TestingAuthenticationToken("bob", null, "TRADE_VIEW");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertDoesNotThrow(() -> dashboardService.searchTrades(new com.technicalchallenge.dto.SearchCriteriaDTO()));
    }

    @Test
    void whenNoSecurityContext_butDbPrivilegePresent_thenPermits() throws Exception {
        // No authentication
        SecurityContextHolder.clearContext();
        when(userPrivilegeService.findPrivilegesByUserLoginIdAndPrivilegeName(anyString(), anyString()))
                .thenReturn(List.of(new UserPrivilege()));

        // call searchTrades which calls hasPrivilege
        assertDoesNotThrow(() -> dashboardService.searchTrades(new com.technicalchallenge.dto.SearchCriteriaDTO()));
    }
}
