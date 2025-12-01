package com.technicalchallenge.service;

import com.technicalchallenge.model.Cashflow;
import com.technicalchallenge.model.LegType;
import com.technicalchallenge.model.PayRec;
import com.technicalchallenge.model.TradeLeg;
import com.technicalchallenge.repository.CashflowRepository;
import com.technicalchallenge.repository.BusinessDayConventionRepository;
import com.technicalchallenge.repository.LegTypeRepository;
import com.technicalchallenge.repository.PayRecRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class CashflowServiceTest {

    @Mock
    private CashflowRepository cashflowRepository;

    @Mock
    private PayRecRepository payRecRepository;

    @Mock
    private LegTypeRepository legTypeRepository;

    @Mock
    private BusinessDayConventionRepository businessDayConventionRepository;

    @InjectMocks
    private CashflowService cashflowService;

    @InjectMocks
    private TradeService tradeService;

    private Cashflow cashflow1;
    private Cashflow cashflow2;
    private List<Cashflow> cashflowList;
    private TradeLeg tradeLeg;
    private PayRec payRec;

    @BeforeEach
    void setUp() {
        // Set up related entities
        tradeLeg = new TradeLeg();
        tradeLeg.setLegId(1L);
        tradeLeg.setNotional(BigDecimal.valueOf(1000000.0));

        payRec = new PayRec();
        payRec.setId(1L);
        payRec.setPayRec("PAY");

        // Set up first Cashflow
        cashflow1 = new Cashflow();
        cashflow1.setId(1L);
        cashflow1.setTradeLeg(tradeLeg); // Fixed: was setLeg
        cashflow1.setPaymentValue(BigDecimal.valueOf(25000.0));
        cashflow1.setValueDate(LocalDate.now().plusMonths(6));
        cashflow1.setPayRec(payRec);
        cashflow1.setRate(0.05);
        cashflow1.setValidityStartDate(LocalDate.now().minusDays(1)); // Fixed: LocalDate instead of LocalDateTime
        cashflow1.setValidityEndDate(null);

        // Set up second Cashflow
        cashflow2 = new Cashflow();
        cashflow2.setId(2L);
        cashflow2.setTradeLeg(tradeLeg); // Fixed: was setLeg
        cashflow2.setPaymentValue(BigDecimal.valueOf(25000.0));
        cashflow2.setValueDate(LocalDate.now().plusYears(1));
        cashflow2.setPayRec(payRec);
        cashflow2.setRate(0.05);
        cashflow2.setValidityStartDate(LocalDate.now().minusDays(1)); // Fixed: LocalDate instead of LocalDateTime
        cashflow2.setValidityEndDate(null);

        // Set up cashflow list
        cashflowList = Arrays.asList(cashflow1, cashflow2);
    }

    @Test
    void testGetAllCashflows() {
        // Given
        when(cashflowRepository.findAll()).thenReturn(cashflowList);

        // When
        List<Cashflow> result = cashflowService.getAllCashflows();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(cashflow1.getId(), result.get(0).getId());
        assertEquals(cashflow2.getId(), result.get(1).getId());
        verify(cashflowRepository).findAll();
    }

    @Test
    void testGetCashflowById() {
        // Given
        when(cashflowRepository.findById(1L)).thenReturn(Optional.of(cashflow1));

        // When
        Optional<Cashflow> result = cashflowService.getCashflowById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals(BigDecimal.valueOf(25000.0), result.get().getPaymentValue());
        assertEquals(payRec, result.get().getPayRec());
        verify(cashflowRepository).findById(1L);
    }

    @Test
    void testGetCashflowByNonExistentId() {
        // Given
        when(cashflowRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<Cashflow> result = cashflowService.getCashflowById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(cashflowRepository).findById(999L);
    }

    @Test
    void testSaveCashflow() {
        // Given
        Cashflow newCashflow = new Cashflow();
        newCashflow.setTradeLeg(tradeLeg); // Fixed: was setLeg
        newCashflow.setPaymentValue(BigDecimal.valueOf(30000.0));
        newCashflow.setValueDate(LocalDate.now().plusMonths(9));
        newCashflow.setPayRec(payRec);
        newCashflow.setRate(0.04);

        when(cashflowRepository.save(any(Cashflow.class))).thenReturn(newCashflow);

        // When
        Cashflow savedCashflow = cashflowService.saveCashflow(newCashflow);

        // Then
        assertNotNull(savedCashflow);
        assertEquals(BigDecimal.valueOf(30000.0), savedCashflow.getPaymentValue());
        assertEquals(0.04, savedCashflow.getRate());
        verify(cashflowRepository).save(newCashflow);
    }

    @Test
    void testSaveCashflowWithInvalidPaymentValue() {
        // Given
        Cashflow invalidCashflow = new Cashflow();
        invalidCashflow.setTradeLeg(tradeLeg); // Fixed: was setLeg
        invalidCashflow.setPaymentValue(BigDecimal.valueOf(-10000.0)); // Negative value
        invalidCashflow.setValueDate(LocalDate.now().plusMonths(3));

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> cashflowService.saveCashflow(invalidCashflow)); // Fixed:
                                                                                                           // expression
                                                                                                           // lambda
        verify(cashflowRepository, never()).save(any(Cashflow.class));
    }

    @Test
    void testSaveCashflowWithMissingValueDate() {
        // Given
        Cashflow invalidCashflow = new Cashflow();
        invalidCashflow.setTradeLeg(tradeLeg); // Fixed: was setLeg
        invalidCashflow.setPaymentValue(BigDecimal.valueOf(15000.0));
        invalidCashflow.setValueDate(null); // Missing value date

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> cashflowService.saveCashflow(invalidCashflow)); // Fixed:
                                                                                                           // expression
                                                                                                           // lambda
        verify(cashflowRepository, never()).save(any(Cashflow.class));
    }

    @Test
    void testDeleteCashflow() {
        // Given
        Long cashflowId = 1L;
        doNothing().when(cashflowRepository).deleteById(cashflowId);

        // When
        cashflowService.deleteCashflow(cashflowId);

        // Then
        verify(cashflowRepository).deleteById(cashflowId);
    }

    @Test
    void testGenerateQuarterlyCashflow() {

        TradeLeg leg = new TradeLeg();// a new leg to use for calcualtion

        leg.setNotional(new BigDecimal("10000000")); // sets the notional to £10000000(it matches the bug description
                                                     // from the task:“$10M trade with 3.5% rate generating ~$875,000
                                                     // quarterly instead of ~$87,500”) using the string constructor to
                                                     // avoid double rounding issues. uses a String as Java parses the
                                                     // digits exactly no rounding at all
        leg.setRate(3.5);// as it is now the service class has this incorrectly as 3.5 instead of 0.035
                         // which produced too large. this line reproduces that bug

        LegType legType = new LegType(); // fixed or floating?
        legType.setType("Fixed");// Use the fixed-rate formula

        leg.setLegRateType(legType); // Attach legType to the tradeLeg so generateCashflows() knows it's a Fixed leg

        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 4, 2);// 3 months a quarter. REFACTORED changed the date to 2nd as I got
                                                     // and error due to this while loop in calculatePaymentDates
        // trigger the calculation.calls the production method, then save a new cashflow
        // in the database
        tradeService.generateCashflows(leg, startDate, endDate);
        ArgumentCaptor<Cashflow> captor = ArgumentCaptor.forClass(Cashflow.class);// Catch the cashflow object's exact
                                                                                  // value calcualted that is saved,
                                                                                  // without a real database.
        verify(cashflowRepository, atLeastOnce()).save(captor.capture());// watch for any call to
                                                                         // cashflowRepository.save(...) and capture the
                                                                         // object that was passed in.

        List<Cashflow> cashflows = captor.getAllValues();// get all captured cashflows in this case only one.

        assertEquals(1, cashflows.size(), "Should generate exactly one quarterly cashflow");

        BigDecimal actualValue = cashflows.get(0).getPaymentValue(); // get payment amount calculated and saved.Should
                                                                     // be 875,000.00 when the bug exists.

        BigDecimal correctValue = new BigDecimal("87500.00");// expected value

        assertEquals(correctValue, actualValue, "Expected £87,500.00 for £10m at 3.5% quarterly");

    }
}
