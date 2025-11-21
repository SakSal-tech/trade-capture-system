package com.technicalchallenge.service;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.*;
import com.technicalchallenge.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
// FIX: Allow mixed tests to share a class-level setup without failing on unused stubs
import org.mockito.junit.jupiter.MockitoSettings; // FIX: import for lenient strictness
import org.mockito.quality.Strictness; // FIX: import for lenient strictness

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // FIX: Prevent UnnecessaryStubbingException across tests with
                                                  // different paths
class TradeServiceTest {

    @Mock
    private TradeRepository tradeRepository;

    @Mock
    private TradeLegRepository tradeLegRepository;

    @Mock
    private CashflowRepository cashflowRepository;

    @Mock
    private TradeStatusRepository tradeStatusRepository;

    @Mock
    private TradeMapper tradeMapper;

    // FIX: These are present in TradeService; keeping them mocked avoids NPE if
    // future tests touch them.
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
    private AdditionalInfoService additionalInfoService;

    @InjectMocks
    private TradeService tradeService;

    private TradeDTO tradeDTO;
    private Trade trade;

    @BeforeEach
    void setUp() {
        // Set up test data
        tradeDTO = new TradeDTO();
        tradeDTO.setTradeId(100001L);
        tradeDTO.setTradeDate(LocalDate.now());
        tradeDTO.setTradeStartDate(LocalDate.now().plusDays(1));
        tradeDTO.setTradeMaturityDate(LocalDate.now().plusYears(1));

        TradeLegDTO leg1 = new TradeLegDTO();
        leg1.setNotional(BigDecimal.valueOf(1000000));
        leg1.setRate(0.05);

        TradeLegDTO leg2 = new TradeLegDTO();
        leg2.setNotional(BigDecimal.valueOf(1000000));
        leg2.setRate(0.0);

        tradeDTO.setTradeLegs(Arrays.asList(leg1, leg2));

        trade = new Trade();
        trade.setId(1L);
        trade.setTradeId(100001L);
        // version intentionally not set here; tests that need it will set
        // explicitly.
    }

    @Test
    void testCreateTrade_Success() {
        // Given
        // FIX: createTrade() validates reference data, must provide
        // book/counterparty/status
        tradeDTO.setBookName("TEST-BOOK-1"); // FIX: required by service reference lookup
        tradeDTO.setCounterpartyName("TestCounterparty"); // FIX: required by service reference lookup
        // tradeStatus is set to "NEW" by service if null

        Book book = new Book();
        book.setId(10L);
        book.setBookName("TEST-BOOK-1");

        Counterparty cp = new Counterparty();
        cp.setId(20L);
        cp.setName("TestCounterparty");

        TradeStatus newStatus = new TradeStatus();
        newStatus.setId(30L);
        newStatus.setTradeStatus("NEW");

        when(bookRepository.findByBookName("TEST-BOOK-1")).thenReturn(Optional.of(book)); // FIX: ref data stub
        when(counterpartyRepository.findByName("TestCounterparty")).thenReturn(Optional.of(cp)); // FIX: ref data stub
        when(tradeStatusRepository.findByTradeStatus("NEW")).thenReturn(Optional.of(newStatus)); // FIX: ref data stub

        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0)); // FIX: return saved entity

        // FIX: legs must be saved before cashflows; return self with synthetic id for
        // stability
        when(tradeLegRepository.save(any(TradeLeg.class))).thenAnswer(inv -> {
            TradeLeg saved = inv.getArgument(0);
            if (saved.getLegId() == null)
                saved.setLegId(999L);
            return saved;
        });

        // When
        Trade result = tradeService.createTrade(tradeDTO);

        // Then
        assertNotNull(result);
        assertEquals(100001L, result.getTradeId());
        verify(tradeRepository, atLeastOnce()).save(any(Trade.class)); // saved trade and amended legs/cashflows path
    }

    @Test
    void testCreateTrade_InvalidDates_ShouldFail() {
        // Given - This test is intentionally failing for candidates to fix
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 1, 10)); // Before trade date

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeService.createTrade(tradeDTO);
        });

        // FIX: Assert the real message thrown by validateTradeCreation
        assertEquals("Start date cannot be before trade date", exception.getMessage());
    }

    @Test
    void testCreateTrade_InvalidLegCount_ShouldFail() {
        // Given
        tradeDTO.setTradeLegs(Arrays.asList(new TradeLegDTO())); // Only 1 leg

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeService.createTrade(tradeDTO);
        });

        assertTrue(exception.getMessage().contains("exactly 2 legs")
                || exception.getMessage().contains("must have exactly 2"));
    }

    @Test
    void testGetTradeById_Found() {
        // Given
        when(tradeRepository.findByTradeIdAndActiveTrue(100001L)).thenReturn(Optional.of(trade));

        // When
        Optional<Trade> result = tradeService.getTradeById(100001L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(100001L, result.get().getTradeId());
    }

    @Test
    void testGetTradeById_NotFound() {
        // Given
        when(tradeRepository.findByTradeIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        // When
        Optional<Trade> result = tradeService.getTradeById(999L);

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void testAmendTrade_Success() {
        // Given
        Trade existing = new Trade();
        existing.setId(1L);
        existing.setTradeId(100001L);
        existing.setVersion(1); // FIX: set non-null version to avoid NPE in version increment
        existing.setActive(true);
        // Mocks the repository call so that looking up trade ID 100001, returns
        // Optional.of(existing) instead of hitting the DB.
        when(tradeRepository.findByTradeIdAndActiveTrue(100001L)).thenReturn(Optional.of(existing));
        // Creates a new, empty TradeStatus object to use as a stubbed return value
        TradeStatus amended = new TradeStatus();
        // Sets a fake primary key (long 40) on that status, simulating a persisted row.
        amended.setId(40L);
        // Sets the status text to "AMENDED"
        amended.setTradeStatus("AMENDED");
        // Stubs the status lookup so asking for "AMENDED" returns the amended object
        // (wrapped in Optional) every time. The // FIX: is just a note to future
        // readers.
        when(tradeStatusRepository.findByTradeStatus("AMENDED")).thenReturn(Optional.of(amended));

        // Begins a stub for saving TradeLeg entities using thenAnswer to customise the
        // return value.Return whatever is being saved (deactivate old trade + amended
        // trade
        // save happen
        // here). Grabs the TradeLeg argument passed to save
        when(tradeRepository.save(any(Trade.class))).thenAnswer(inv -> inv.getArgument(0));// 0 because of first
                                                                                           // argument

        // FIX: prevent NPE inside createTradeLegsWithCashflows by stubbing leg save.
        // thenAnswer. Its real type is InvocationOnMock (from Mockito).
        // InvocationOnMock represents the method call that was intercepted by Mockito.
        when(tradeLegRepository.save(any(TradeLeg.class))).thenAnswer(inv -> {
            TradeLeg saved = inv.getArgument(0);
            if (saved.getLegId() == null)
                saved.setLegId(999L);
            return saved;
        });

        // When
        Trade result = tradeService.amendTrade(100001L, tradeDTO);

        // Then
        assertNotNull(result);
        assertEquals(100001L, result.getTradeId());
        assertEquals(2, result.getVersion()); // FIX: ensure version increment works
        verify(tradeRepository, atLeast(2)).save(any(Trade.class)); // Save old and new
    }

    @Test
    void testAmendTrade_TradeNotFound() {
        // Given
        when(tradeRepository.findByTradeIdAndActiveTrue(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeService.amendTrade(999L, tradeDTO);
        });

        assertTrue(exception.getMessage().contains("Trade not found"));
    }

    @Test
    void testCashflowGeneration_MonthlySchedule() {
        // creating a new object
        TradeLeg leg = new TradeLeg();
        leg.setNotional(BigDecimal.valueOf(1000000));// setting up test data by assigning value to the notional field of
                                                     // the TradeLeg object
        leg.setRate(0.5);

        /*
         * Set schedule to monthly. By creating a Schedule object and assigning it to
         * the leg, ensures that the generateCashflows method knows to generate
         * cashflows according to the schedule (in this case, monthly). Otherwise it
         * will use the default quartely 3M
         */
        Schedule schedule = new Schedule();
        schedule.setSchedule("Monthly");
        leg.setCalculationPeriodSchedule(schedule);

        // FIX: typo in variable name (starDate → startDate) to keep code readable
        LocalDate startDate = LocalDate.of(2025, 1, 1); // FIX: renamed from starDate
        LocalDate maturityDate = LocalDate.of(2026, 1, 1);

        // when
        // When this method is called, it uses Monthly and generates 12 cashflows for 1
        // year period
        tradeService.generateCashflows(leg, startDate, maturityDate);

        // then
        // Using Mockito to verify the expected number of cashflows. This tells Mockito
        // to expect that method is called 12 times to generate cashflow
        verify(cashflowRepository, times(12)).save(any(Cashflow.class));// Cashflow.class any instance of the Cashflow
                                                                        // type
    }

}