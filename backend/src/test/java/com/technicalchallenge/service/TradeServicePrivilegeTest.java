package com.technicalchallenge.service;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.model.*;
import com.technicalchallenge.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for user privilege checking in TradeService.
 * Verifies that CRUD operations properly validate user privileges.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TradeServicePrivilegeTest {

    @Mock
    private TradeRepository tradeRepository;
    @Mock
    private TradeLegRepository tradeLegRepository;
    @Mock
    private CashflowRepository cashflowRepository;
    @Mock
    private TradeStatusRepository tradeStatusRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private CounterpartyRepository counterpartyRepository;
    @Mock
    private ApplicationUserRepository applicationUserRepository;
    @Mock
    private TradeTypeRepository tradeTypeRepository;
    @Mock
    private TradeSubTypeRepository tradeSubTypeRepository;
    @Mock
    private CurrencyRepository currencyRepository;
    @Mock
    private LegTypeRepository legTypeRepository;
    @Mock
    private IndexRepository indexRepository;
    @Mock
    private HolidayCalendarRepository holidayCalendarRepository;
    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private BusinessDayConventionRepository businessDayConventionRepository;
    @Mock
    private PayRecRepository payRecRepository;
    @Mock
    private UserPrivilegeService userPrivilegeService;

    @InjectMocks
    private TradeService tradeService;

    private TradeDTO tradeDTO;
    private Trade trade;

    @BeforeEach
    void setUp() {
        // Clear security context before each test
        SecurityContextHolder.clearContext();
        
        // Set up test trade DTO
        tradeDTO = new TradeDTO();
        tradeDTO.setTradeId(100001L);
        tradeDTO.setTradeDate(LocalDate.of(2025, 1, 15));
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 1, 17));
        tradeDTO.setTradeMaturityDate(LocalDate.of(2026, 1, 17));
        tradeDTO.setBookName("TestBook");
        tradeDTO.setCounterpartyName("TestCounterparty");

        TradeLegDTO leg1 = new TradeLegDTO();
        leg1.setNotional(BigDecimal.valueOf(1000000));
        leg1.setRate(0.05);

        TradeLegDTO leg2 = new TradeLegDTO();
        leg2.setNotional(BigDecimal.valueOf(1000000));
        leg2.setRate(0.0);

        tradeDTO.setTradeLegs(Arrays.asList(leg1, leg2));

        // Set up test trade entity
        trade = new Trade();
        trade.setId(1L);
        trade.setTradeId(100001L);
        trade.setVersion(1);
        trade.setActive(true);
    }

    @Test
    void testCreateTrade_WithTraderRole_ShouldSucceed() {
        // Given: User has TRADER role
        TestingAuthenticationToken auth = new TestingAuthenticationToken("trader1", null, "ROLE_TRADER");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Mock required repositories
        setupMocksForSuccessfulCreate();

        // When: Create trade
        Trade result = tradeService.createTrade(tradeDTO);

        // Then: Should succeed without privilege check error
        assertNotNull(result);
        verify(tradeRepository, atLeastOnce()).save(any(Trade.class));
    }

    @Test
    void testCreateTrade_WithoutPrivilege_ShouldFail() {
        // Given: User has SUPPORT role with no TRADE_CREATE privilege
        TestingAuthenticationToken auth = new TestingAuthenticationToken("support1", null, "ROLE_SUPPORT");
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        when(userPrivilegeService.findPrivilegesByUserLoginIdAndPrivilegeName(
                eq("support1"), eq("TRADE_CREATE"))).thenReturn(List.of());

        // When & Then: Should throw AccessDeniedException
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            tradeService.createTrade(tradeDTO);
        });

        assertTrue(exception.getMessage().contains("Insufficient privileges to create trades"));
        verify(tradeRepository, never()).save(any(Trade.class));
    }

    @Test
    void testAmendTrade_WithMiddleOfficeRole_ShouldSucceed() {
        // Given: User has MIDDLE_OFFICE role (authorized for TRADE_AMEND)
        TestingAuthenticationToken auth = new TestingAuthenticationToken("mo1", null, "ROLE_MIDDLE_OFFICE");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Mock existing trade
        when(tradeRepository.findByTradeIdAndActiveTrue(100001L)).thenReturn(Optional.of(trade));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tradeStatusRepository.findByTradeStatus("AMENDED")).thenReturn(
                Optional.of(createTradeStatus("AMENDED")));
        when(tradeLegRepository.save(any(TradeLeg.class))).thenAnswer(inv -> {
            TradeLeg saved = inv.getArgument(0);
            if (saved.getLegId() == null) saved.setLegId(999L);
            return saved;
        });

        // When: Amend trade
        Trade result = tradeService.amendTrade(100001L, tradeDTO);

        // Then: Should succeed
        assertNotNull(result);
        assertEquals(2, result.getVersion());
    }

    @Test
    void testAmendTrade_WithoutPrivilege_ShouldFail() {
        // Given: User has SUPPORT role (no TRADE_AMEND privilege)
        TestingAuthenticationToken auth = new TestingAuthenticationToken("support1", null, "ROLE_SUPPORT");
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        when(userPrivilegeService.findPrivilegesByUserLoginIdAndPrivilegeName(
                eq("support1"), eq("TRADE_AMEND"))).thenReturn(List.of());

        // When & Then: Should throw AccessDeniedException
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            tradeService.amendTrade(100001L, tradeDTO);
        });

        assertTrue(exception.getMessage().contains("Insufficient privileges to amend trades"));
    }

    @Test
    void testCancelTrade_WithTraderRole_ShouldSucceed() {
        // Given: User has TRADER role (authorized for TRADE_CANCEL)
        TestingAuthenticationToken auth = new TestingAuthenticationToken("trader1", null, "ROLE_TRADER");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Mock existing trade
        when(tradeRepository.findByTradeIdAndActiveTrue(100001L)).thenReturn(Optional.of(trade));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tradeStatusRepository.findByTradeStatus("CANCELLED")).thenReturn(
                Optional.of(createTradeStatus("CANCELLED")));

        // When: Cancel trade
        Trade result = tradeService.cancelTrade(100001L);

        // Then: Should succeed
        assertNotNull(result);
        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    void testCancelTrade_WithoutPrivilege_ShouldFail() {
        // Given: User has MIDDLE_OFFICE role (no TRADE_CANCEL privilege)
        TestingAuthenticationToken auth = new TestingAuthenticationToken("mo1", null, "ROLE_MIDDLE_OFFICE");
        SecurityContextHolder.getContext().setAuthentication(auth);
        
        when(userPrivilegeService.findPrivilegesByUserLoginIdAndPrivilegeName(
                eq("mo1"), eq("TRADE_CANCEL"))).thenReturn(List.of());

        // When & Then: Should throw AccessDeniedException
        AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> {
            tradeService.cancelTrade(100001L);
        });

        assertTrue(exception.getMessage().contains("Insufficient privileges to cancel trades"));
    }

    @Test
    void testTerminateTrade_WithTraderRole_ShouldSucceed() {
        // Given: User has TRADER role (authorized for TRADE_TERMINATE)
        TestingAuthenticationToken auth = new TestingAuthenticationToken("trader1", null, "ROLE_TRADER");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Mock existing trade
        when(tradeRepository.findByTradeIdAndActiveTrue(100001L)).thenReturn(Optional.of(trade));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tradeStatusRepository.findByTradeStatus("TERMINATED")).thenReturn(
                Optional.of(createTradeStatus("TERMINATED")));

        // When: Terminate trade
        Trade result = tradeService.terminateTrade(100001L);

        // Then: Should succeed
        assertNotNull(result);
        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    void testDeleteTrade_WithTraderRole_ShouldSucceed() {
        // Given: User has TRADER role (authorized for TRADE_CANCEL via delete)
        TestingAuthenticationToken auth = new TestingAuthenticationToken("trader1", null, "ROLE_TRADER");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Mock existing trade
        when(tradeRepository.findByTradeIdAndActiveTrue(100001L)).thenReturn(Optional.of(trade));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tradeStatusRepository.findByTradeStatus("CANCELLED")).thenReturn(
                Optional.of(createTradeStatus("CANCELLED")));

        // When: Delete trade
        tradeService.deleteTrade(100001L);

        // Then: Should succeed
        verify(tradeRepository).save(any(Trade.class));
    }

    @Test
    void testCreateTrade_WithDatabasePrivilege_ShouldSucceed() {
        // Given: User has privilege in database but not in role
        // Use a custom role that doesn't grant TRADE_CREATE
        TestingAuthenticationToken auth = new TestingAuthenticationToken("customuser", null, "ROLE_CUSTOM");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Mock database privilege
        UserPrivilege userPrivilege = new UserPrivilege();
        when(userPrivilegeService.findPrivilegesByUserLoginIdAndPrivilegeName(
                eq("customuser"), eq("TRADE_CREATE"))).thenReturn(List.of(userPrivilege));

        // Mock required repositories
        setupMocksForSuccessfulCreate();

        // When: Create trade
        Trade result = tradeService.createTrade(tradeDTO);

        // Then: Should succeed based on DB privilege
        assertNotNull(result);
        verify(userPrivilegeService).findPrivilegesByUserLoginIdAndPrivilegeName(
                eq("customuser"), eq("TRADE_CREATE"));
    }

    @Test
    void testCreateTrade_WithSuperuserRole_ShouldSucceed() {
        // Given: User has SUPERUSER role (has all privileges)
        TestingAuthenticationToken auth = new TestingAuthenticationToken("admin", null, "ROLE_SUPERUSER");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Mock required repositories
        setupMocksForSuccessfulCreate();

        // When: Create trade
        Trade result = tradeService.createTrade(tradeDTO);

        // Then: Should succeed
        assertNotNull(result);
        verify(tradeRepository, atLeastOnce()).save(any(Trade.class));
    }

    // Helper methods

    private void setupMocksForSuccessfulCreate() {
        Book book = new Book();
        book.setId(10L);
        book.setBookName("TestBook");

        Counterparty cp = new Counterparty();
        cp.setId(20L);
        cp.setName("TestCounterparty");

        TradeStatus newStatus = createTradeStatus("NEW");

        when(bookRepository.findByBookName("TestBook")).thenReturn(Optional.of(book));
        when(counterpartyRepository.findByName("TestCounterparty")).thenReturn(Optional.of(cp));
        when(tradeStatusRepository.findByTradeStatus("NEW")).thenReturn(Optional.of(newStatus));
        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));
        when(tradeLegRepository.save(any(TradeLeg.class))).thenAnswer(inv -> {
            TradeLeg saved = inv.getArgument(0);
            if (saved.getLegId() == null) saved.setLegId(999L);
            return saved;
        });
    }

    private TradeStatus createTradeStatus(String status) {
        TradeStatus tradeStatus = new TradeStatus();
        tradeStatus.setId(30L);
        tradeStatus.setTradeStatus(status);
        return tradeStatus;
    }
}
