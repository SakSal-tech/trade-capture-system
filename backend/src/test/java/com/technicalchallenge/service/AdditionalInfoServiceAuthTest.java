package com.technicalchallenge.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.technicalchallenge.mapper.AdditionalInfoMapper;
import com.technicalchallenge.model.AdditionalInfo;
import com.technicalchallenge.model.ApplicationUser;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.repository.AdditionalInfoAuditRepository;
import com.technicalchallenge.repository.AdditionalInfoRepository;
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.validation.TradeValidationEngine;
import com.technicalchallenge.validation.TradeValidationResult;

import java.util.Optional;

/**
 * Authentication/authorization unit tests for AdditionalInfoService.
 *
 * These tests verify that ownership checks prevent a trader from updating
 * another trader's settlement instructions while allowing elevated roles
 * such as SALES to proceed.
 */
public class AdditionalInfoServiceAuthTest {

    private AdditionalInfoService additionalInfoService;

    @Mock
    private AdditionalInfoRepository additionalInfoRepository;

    @Mock
    private AdditionalInfoAuditRepository additionalInfoAuditRepository;

    @Mock
    private AdditionalInfoMapper additionalInfoMapper;

    @Mock
    private TradeValidationEngine tradeValidationEngine;

    @Mock
    private TradeRepository tradeRepository;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        additionalInfoService = new AdditionalInfoService(additionalInfoRepository, additionalInfoMapper,
                additionalInfoAuditRepository, tradeValidationEngine, tradeRepository);
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenTraderEditsAnotherTradersTrade_thenAccessDenied() {
        // Simulate a logged-in trader 'simon'
        TestingAuthenticationToken auth = new TestingAuthenticationToken("simon", null, "ROLE_TRADER");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Prepare trade owned by 'joey'
        Trade trade = new Trade();
        ApplicationUser owner = new ApplicationUser();
        owner.setLoginId("joey");
        trade.setTraderUser(owner);

        when(tradeRepository.findLatestActiveVersionByTradeId(anyLong())).thenReturn(Optional.of(trade));

        // Validation engine must return a non-null result; tests use a
        // mocked engine so ensure it returns a valid TradeValidationResult
        // to avoid a NullPointerException inside the service's validation
        // code. The test's focus is authorization, not validation.
        when(tradeValidationEngine.validateSettlementInstructions(any()))
                .thenReturn(new TradeValidationResult());

        // Attempt to edit: should throw AccessDeniedException
        assertThrows(org.springframework.security.access.AccessDeniedException.class,
                () -> additionalInfoService.upOrInsertTradeSettlementInstructions(200101L, "Some text", "simon"));
    }

    @Test
    void whenSalesEditsAnotherTradersTrade_thenAllowed() {
        // Simulate a logged-in sales user
        TestingAuthenticationToken auth = new TestingAuthenticationToken("alice", null, "ROLE_SALES");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Prepare trade owned by 'joey'
        Trade trade = new Trade();
        ApplicationUser owner = new ApplicationUser();
        owner.setLoginId("joey");
        trade.setTraderUser(owner);

        when(tradeRepository.findLatestActiveVersionByTradeId(anyLong())).thenReturn(Optional.of(trade));

        // Validation passes
        TradeValidationResult ok = new TradeValidationResult();
        when(tradeValidationEngine.validateSettlementInstructions(any())).thenReturn(ok);

        // No existing AdditionalInfo
        when(additionalInfoRepository.findActiveByEntityTypeAndEntityIdAndFieldName(anyString(), anyLong(),
                anyString()))
                .thenReturn(null);

        // Mapper and save stubs
        when(additionalInfoMapper.toEntity(any())).thenReturn(new AdditionalInfo());
        when(additionalInfoRepository.save(any())).thenReturn(new AdditionalInfo());
        when(additionalInfoMapper.toDto(any())).thenReturn(new com.technicalchallenge.dto.AdditionalInfoDTO());

        // Should not throw
        assertDoesNotThrow(() -> additionalInfoService.upOrInsertTradeSettlementInstructions(200101L,
                "Settle via JPM New York", "alice"));
    }
}
