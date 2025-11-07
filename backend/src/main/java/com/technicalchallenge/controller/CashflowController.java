package com.technicalchallenge.controller;

import com.technicalchallenge.dto.CashflowDTO;
import com.technicalchallenge.dto.CashflowGenerationRequest;
import com.technicalchallenge.mapper.CashflowMapper;
import com.technicalchallenge.service.CashflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/cashflows")
@Validated
@Tag(name = "Cashflows", description = "Cashflow generation and management for trades")
public class CashflowController {
    private static final Logger logger = LoggerFactory.getLogger(CashflowController.class);

    @Autowired
    private CashflowService cashflowService;
    @Autowired
    private CashflowMapper cashflowMapper;

    /*
     * Returns a list of all cashflows in the system. Calls
     * cashflowService.getAllCashflows(), maps each entity to a DTO, and returns the
     * list
     */
    @GetMapping
    @Operation(summary = "Get all cashflows", description = "Retrieves a list of all generated cashflows in the system with payment dates and amounts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all cashflows", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CashflowDTO.class))),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public List<CashflowDTO> getAllCashflows() {
        logger.info("Fetching all cashflows");
        return cashflowService.getAllCashflows().stream()
                .map(cashflowMapper::toDto)
                .toList();
    }

    /*
     * Returns a single cashflow by its ID. How: Calls
     * cashflowService.getCashflowById(id), maps the result to a DTO, and wraps it
     * in a ResponseEntity. Returns 404 if not found.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get cashflow by ID", description = "Retrieves a specific cashflow by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cashflow found and returned successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CashflowDTO.class))),
            @ApiResponse(responseCode = "404", description = "Cashflow not found"),
            @ApiResponse(responseCode = "400", description = "Invalid cashflow ID format")
    })
    public ResponseEntity<CashflowDTO> getCashflowById(
            @Parameter(description = "Unique identifier of the cashflow", required = true) @PathVariable(name = "id") Long id) {
        logger.debug("Fetching cashflow by id: {}", id);
        return cashflowService.getCashflowById(id)
                .map(cashflowMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /*
     * Creates a new cashflow. Validates that the payment value is positive and the
     * value date is present. Maps the DTO to an entity, populates reference data,
     * saves it, and returns the saved DTO
     */
    @PostMapping
    @Operation(summary = "Create new cashflow", description = "Adds a new cashflow to the system with the specified payment details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cashflow created successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CashflowDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data for cashflow creation"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createCashflow(@Valid @RequestBody CashflowDTO cashflowDTO) {
        logger.info("Creating new cashflow: {}", cashflowDTO);
        // Validation: value > 0, valueDate not null
        if (cashflowDTO.getPaymentValue() == null || cashflowDTO.getPaymentValue().signum() <= 0) {
            return ResponseEntity.badRequest().body("Cashflow value must be positive");
        }
        if (cashflowDTO.getValueDate() == null) {
            return ResponseEntity.badRequest().body("Value date is required");
        }
        var entity = cashflowMapper.toEntity(cashflowDTO);
        cashflowService.populateReferenceDataByName(entity, cashflowDTO);
        var saved = cashflowService.saveCashflow(entity);
        return ResponseEntity.ok(cashflowMapper.toDto(saved));
    }

    /*
     * Deletes a cashflow by its ID cashflowService.deleteCashflow(id) and returns a
     * 204 No Content response.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete cashflow", description = "Removes a cashflow from the system by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Cashflow deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Cashflow not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Void> deleteCashflow(@PathVariable(name = "id") Long id) {
        logger.warn("Deleting cashflow with id: {}", id);
        cashflowService.deleteCashflow(id);
        return ResponseEntity.noContent().build();
    }

    /*
     * Generates a list of cashflows based on trade legs and schedule. Validates
     * that legs are present. For each leg, calculates cashflow dates and values
     * based on the schedule (monthly, quarterly, etc.). For "Fixed" legs,
     * calculates payment value using notional, rate, and days. Creates and adds
     * CashflowDTO objects to the result list. Returns the list of generated
     * cashflows.
     */
    @PostMapping("/generate")
    @Operation(summary = "Generate cashflows", description = "Creates a series of cashflows based on trade legs and specified generation parameters")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cashflows generated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CashflowDTO.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data for cashflow generation"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    // Refactored: Historically this endpoint only calculated payment amounts for
    // Fixed
    // legs and left Floating legs at zero. That produced many zero-valued
    // cashflows when the frontend supplied an explicit numeric rate for a
    // Floating leg (for testing or one-off pricing). To make the endpoint
    // more useful during UI-driven testing and to keep behaviour consistent
    // with calculateCashflowValue in TradeService, now calculate payments
    // for Floating legs when the caller provides a concrete `rate` value.
    // If no rate is provided for a Floating leg preserve the old
    // behaviour (paymentValue remains zero) so production behaviour that
    // relies on external market-data is unchanged.
    public ResponseEntity<List<CashflowDTO>> generateCashflows(@RequestBody CashflowGenerationRequest request) {
        List<CashflowDTO> allCashflows = new ArrayList<>();
        if (request.getLegs() == null || request.getLegs().isEmpty()) {
            return ResponseEntity.badRequest().body(allCashflows);
        }
        for (CashflowGenerationRequest.TradeLegDTO leg : request.getLegs()) {
            LocalDate startDate = request.getTradeStartDate();
            LocalDate maturityDate = request.getTradeMaturityDate();
            String schedule = leg.getCalculationPeriodSchedule();
            int months = scheduleToMonths(schedule);
            if (months <= 0) {
                continue;
            }
            LocalDate valueDate = startDate;
            while (valueDate.isBefore(maturityDate)) {
                LocalDate nextValueDate = valueDate.plusMonths(months);
                if (nextValueDate.isAfter(maturityDate)) {
                    nextValueDate = maturityDate;
                }
                BigDecimal paymentValue = BigDecimal.ZERO;
                if ("Fixed".equalsIgnoreCase(leg.getLegType())) {
                    // This line calculates the number of days between two dates, ChronoUnit
                    // representes standard units of time, such as DAYS, MONTHS, YEARS, HOURS.
                    long days = java.time.temporal.ChronoUnit.DAYS.between(valueDate, nextValueDate);
                    double rate = leg.getRate() != null ? leg.getRate() : 0.0;
                    // The principal amount for the trade leg
                    paymentValue = leg.getNotional().multiply(BigDecimal.valueOf(rate))// The interest rate (as a
                                                                                       // decimal, e.g., 0.05 for 5%).
                            .multiply(BigDecimal.valueOf(days))// The number of days in the payment period.
                            .divide(BigDecimal.valueOf(360), 2, java.math.RoundingMode.HALF_UP);// Divides by 360 (the
                                                                                                // standard "banking
                                                                                                // year" for interest
                                                                                                // calculations), rounds
                                                                                                // to 2 decimal places
                                                                                                // using HALF_UP
                                                                                                // rounding.

                } else if ("Floating".equalsIgnoreCase(leg.getLegType())) {
                    // Compute floating-leg payment only when the request includes
                    // an explicit numeric rate. This keeps the endpoint useful
                    // for UI testing and aligns it with TradeService which
                    // computes a floating payment when a rate is present.
                    // If no rate is supplied intentionally leave paymentValue
                    // as zero so that callers relying on market fixings are
                    // unaffected.
                    if (leg.getRate() != null) {
                        long days = java.time.temporal.ChronoUnit.DAYS.between(valueDate, nextValueDate);
                        double rate = leg.getRate();
                        paymentValue = leg.getNotional()
                                .multiply(BigDecimal.valueOf(rate))
                                .multiply(BigDecimal.valueOf(days))
                                .divide(BigDecimal.valueOf(360), 2, java.math.RoundingMode.HALF_UP);
                    }
                }
                // For floating legs without an explicit rate, paymentValue remains 0
                CashflowDTO cf = new CashflowDTO();
                cf.setValueDate(nextValueDate);
                cf.setPaymentValue(paymentValue);
                cf.setPayRec(leg.getPayReceiveFlag());
                cf.setPaymentType(leg.getLegType());
                cf.setPaymentBusinessDayConvention(leg.getPaymentBusinessDayConvention());
                cf.setRate(leg.getRate());
                allCashflows.add(cf);
                valueDate = nextValueDate;
            }
        }
        return ResponseEntity.ok(allCashflows);
    }

    /*
     * Converts a schedule string e.g."Monthly", "Quarterly" to a number of
     * months.Checks the string for keywords and returns the corresponding month
     * interval (1, 3, 6, 12, etc.).
     */
    private int scheduleToMonths(String schedule) {
        if (schedule == null)
            return 0;
        schedule = schedule.toLowerCase();
        if (schedule.contains("month")) {
            if (schedule.contains("3"))
                return 3;
            if (schedule.contains("6"))
                return 6;
            if (schedule.contains("12"))
                return 12;
            return 1;
        }
        if (schedule.contains("quarter"))
            return 3;
        if (schedule.contains("annual") || schedule.contains("year"))
            return 12;
        return 0;
    }

}
