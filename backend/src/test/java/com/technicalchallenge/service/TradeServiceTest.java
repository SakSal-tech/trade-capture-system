package com.technicalchallenge.service;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.dto.TradeLegDTO;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.model.TradeLeg;
import com.technicalchallenge.model.TradeStatus;
import com.technicalchallenge.repository.CashflowRepository;
import com.technicalchallenge.repository.TradeLegRepository;
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.repository.TradeStatusRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
// Added: enable lenient mode to avoid UnnecessaryStubbingException while you iterate
import org.mockito.junit.jupiter.MockitoExtension;
// Added: import MockitoSettings + Strictness
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import com.technicalchallenge.model.Book;
import com.technicalchallenge.model.Cashflow;
import com.technicalchallenge.model.Counterparty;
import com.technicalchallenge.model.Currency;
//import java.util.Currency;
// Added import for Schedule
import com.technicalchallenge.model.Schedule;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// Added: Make Mockito lenient so shared stubs in setUp() don’t fail tests that
// don’t use them.
@MockitoSettings(strictness = Strictness.LENIENT)
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
    private AdditionalInfoService additionalInfoService;

    // Added so mockito creates a mock version of the BookRepository for your tests.
    // This allows you to control its behaviour
    @Mock
    private com.technicalchallenge.repository.BookRepository bookRepository;

    @Mock
    private com.technicalchallenge.repository.CounterpartyRepository counterpartyRepository;

    // added to fix issue Cannot invoke
    // "com.technicalchallenge.repository.CurrencyRepository.
    @Mock
    private com.technicalchallenge.repository.CurrencyRepository currencyRepository;

    @Mock
    private com.technicalchallenge.repository.ApplicationUserRepository applicationUserRepository;

    @Mock
    private com.technicalchallenge.repository.TradeTypeRepository tradeTypeRepository;

    @InjectMocks
    private TradeService tradeService;

    private TradeDTO tradeDTO;
    private Trade trade;

    @BeforeEach
    void setUp() {
        // Set up test data
        tradeDTO = new TradeDTO();
        tradeDTO.setTradeId(100001L);
        tradeDTO.setTradeDate(LocalDate.of(2025, 1, 15));
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 1, 17));
        tradeDTO.setTradeMaturityDate(LocalDate.of(2026, 1, 17));

        // Create TradeLegDTOs
        TradeLegDTO leg1 = new TradeLegDTO();
        leg1.setNotional(BigDecimal.valueOf(1000000));
        leg1.setRate(0.05);
        leg1.setCurrency("USD");

        TradeLegDTO leg2 = new TradeLegDTO();
        leg2.setNotional(BigDecimal.valueOf(1000000));
        leg2.setRate(0.0);
        leg2.setCurrency("GBP");

        tradeDTO.setTradeLegs(Arrays.asList(leg1, leg2));

        // Ensure legId is set to prevent NullPointerException
        if (tradeDTO.getTradeLegs() != null) {
            for (int i = 0; i < tradeDTO.getTradeLegs().size(); i++) {
                tradeDTO.getTradeLegs().get(i).setLegId((long) (i + 1));
            }
        }

        // Added: stub currency lookups because service resolves Currency via repository
        Currency usd = new Currency();
        usd.setId(1L);
        usd.setCurrency("USD");

        Currency gbp = new Currency();
        gbp.setId(2L);
        gbp.setCurrency("GBP");

        when(currencyRepository.findByCurrency("USD")).thenReturn(Optional.of(usd));
        when(currencyRepository.findByCurrency("GBP")).thenReturn(Optional.of(gbp));

        // Create Trade model object
        trade = new Trade();
        trade.setId(1L); // Simulates existing database ID
        trade.setTradeId(100001L); // Business trade ID
        trade.setVersion(1); // Ensures mock Trade has a valid version

        tradeDTO.setBookName("TestBook"); // Simulate trade associated with a book

        // Set Counterparty info to prevent errors in service validation
        tradeDTO.setCounterpartyName("TestCounterparty");
        tradeDTO.setCounterpartyId(123L);

        // Mock Counterparty repository to prevent NullPointerException
        Counterparty counterparty = new Counterparty();
        counterparty.setId(123L);
        counterparty.setName("TestCounterparty");
        // Mockito statement, when the code calls the code below return the value.
        // Otherwise, it will return empty by default
        when(counterpartyRepository.findByName("TestCounterparty"))
                .thenReturn(Optional.of(counterparty)); // Mock return so it's not null. I changed it from new
                                                        // Counterparty() with no ID or name, so if TradeService later
                                                        // relies on those values being non-null, I get
                                                        // NullPointerException or validation errors

        // Mock TradeStatus repository
        TradeStatus status = new TradeStatus();
        status.setId(1L);
        status.setTradeStatus("NEW");

        // Fixed: return the populated status object (not a blank new TradeStatus())
        when(tradeStatusRepository.findByTradeStatus("NEW"))
                .thenReturn(Optional.of(status));

        // Mock Book repository
        Book book = new Book(); // no-arg constructor
        book.setId(1L); // set the ID
        book.setBookName("TestBook"); // set the name
        when(bookRepository.findByBookName("TestBook"))
                .thenReturn(Optional.of(book)); // Fixed: return fully populated Book object

        // Added: The repository methods filter on findByTradeIdAndActiveTrue Without
        // this, service logic that checks .isActive() could fail
        trade.setActive(true);

        // Added: Ensure tradeLegRepository.save(...) does NOT return null.
        // This was causing NPE in generateCashflows (leg.getLegId()).
        // We return the same leg passed in, assigning a synthetic legId if missing.
        when(tradeLegRepository.save(any(TradeLeg.class))).thenAnswer(invocation -> {
            TradeLeg saved = invocation.getArgument(0);
            if (saved.getLegId() == null) {
                saved.setLegId(999L); // Added: synthetic ID for tests
            }
            return saved;
        });

        // Added: tradeRepository.save should at least return a non-null Trade.
        // You also override this in specific tests where needed.
        when(tradeRepository.save(any(Trade.class))).thenAnswer(invocation -> {
            Trade t = invocation.getArgument(0);
            if (t.getId() == null) {
                t.setId(42L); // Added: synthetic DB id so it's non-null
            }
            return t;
        });
    }

    @Test
    void testCreateTrade_Success() {
        // Given
        // Fixed: explicit stub is fine; base setUp also returns non-null. Keeping
        // yours.
        when(tradeRepository.save(any(Trade.class))).thenReturn(trade);

        // When
        tradeDTO.setCounterpartyName("TestCounterparty");
        Trade result = tradeService.createTrade(tradeDTO);

        // Then
        assertNotNull(result);
        assertEquals(100001L, result.getTradeId());
        verify(tradeRepository).save(any(Trade.class));

    }

    @Test
    void testCreateTrade_InvalidDates_ShouldFail() {
        // Given - This test is intentionally failing for candidates to fix
        tradeDTO.setTradeStartDate(LocalDate.of(2025, 1, 10)); // Before trade date

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            tradeService.createTrade(tradeDTO);
        });

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

        assertTrue(exception.getMessage().contains("exactly 2 legs"));
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
        when(tradeRepository.findByTradeIdAndActiveTrue(100001L)).thenReturn(Optional.of(trade));
        // Fixed: return a populated AMENDED status to avoid any NULL state downstream
        TradeStatus amended = new TradeStatus();
        amended.setId(2L);
        amended.setTradeStatus("AMENDED");
        when(tradeStatusRepository.findByTradeStatus("AMENDED"))
                .thenReturn(Optional.of(amended));
        when(tradeRepository.save(any(Trade.class))).thenReturn(trade);

        // when
        Trade result = tradeService.amendTrade(100001L, tradeDTO);

        // Then
        assertNotNull(result);
        verify(tradeRepository, times(2)).save(any(Trade.class)); // Save old and new
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
        // It creates a new LocalDate object for a specific year, month, and day.
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate maturityDate = LocalDate.of(2026, 1, 1);

        // when
        // When this method is called, it uses Monthly and generates 12 cashflows for 1
        // year period
        tradeService.generateCashflows(leg, startDate, maturityDate);

        // then
        // Using Mockito to verify the expected number of cashflows. This tells Mockito
        // to expect that method is called 12 times to generate cashflow
        verify(cashflowRepository, times(12)).save(any(Cashflow.class));

    }
}
