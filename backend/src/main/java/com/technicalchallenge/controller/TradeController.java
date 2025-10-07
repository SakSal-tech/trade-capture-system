package com.technicalchallenge.controller;

import com.technicalchallenge.dto.SearchCriteriaDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import io.micrometer.core.instrument.search.Search;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/trades")
@Validated
@Tag(name = "Trades", description = "Trade management operations including booking, searching, and lifecycle management")
public class TradeController {
    private static final Logger logger = LoggerFactory.getLogger(TradeController.class);

    @Autowired
    private TradeService tradeService;
    @Autowired
    private TradeMapper tradeMapper;

    @GetMapping
    public List<TradeDTO> getAllTrades() {
        logger.info("Fetching all trades");
        return tradeService.getAllTrades().stream()
                .map(tradeMapper::toDto)
                .toList();
    }

    @GetMapping("/filter")
    // Page<TradeDTO> is a page (a paginated list) of TradeDTO objects
    // ResponseEntity<Page<TradeDTO>> is the HTTP response wrapper that contains
    // that page and extra HTTP info (status code, headers)
    public ResponseEntity<Page<TradeDTO>> filterTrades(@ModelAttribute SearchCriteriaDTO criteriaDTO,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {

        Page<TradeDTO> pagedResult = tradeService.filterTrades(criteriaDTO, page, size);
        return ResponseEntity.ok(pagedResult);

    }

    @GetMapping("/{id}")
    public ResponseEntity<TradeDTO> getTradeById(@PathVariable(name = "id") Long id) {
        logger.debug("Fetching trade by id: {}", id);
        return tradeService.getTradeById(id)
                .map(tradeMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> createTrade(@Valid @RequestBody TradeDTO tradeDTO) {
        logger.info("Creating new trade: {}", tradeDTO);
        try {
            Trade trade = tradeMapper.toEntity(tradeDTO);
            tradeService.populateReferenceDataByName(trade, tradeDTO);
            Trade savedTrade = tradeService.saveTrade(trade, tradeDTO);
            TradeDTO responseDTO = tradeMapper.toDto(savedTrade);

            // FIX: Changed 200 → 201 Created for POST
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDTO);

        } catch (Exception e) {
            logger.error("Error creating trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error creating trade: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update existing trade", description = "Updates an existing trade with new information. Subject to business rule validation and user privileges.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trade updated successfully", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TradeDTO.class))),
            @ApiResponse(responseCode = "404", description = "Trade not found"),
            @ApiResponse(responseCode = "400", description = "Invalid trade data or business rule violation"),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to update trade")
    })
    public ResponseEntity<?> updateTrade(
            @Parameter(description = "Unique identifier of the trade to update", required = true) @PathVariable Long id,
            @Parameter(description = "Updated trade details", required = true) @Valid @RequestBody TradeDTO tradeDTO) {
        // Checking for tradeId mismatch
        if (tradeDTO.getTradeId() != null && !id.equals(tradeDTO.getTradeId())) {
            // FIX: Updated error message to match test expectation
            return ResponseEntity.badRequest().body("Trade ID in path must match Trade ID in request body");
        }

        logger.info("Updating trade with id: {}", id);
        try {
            tradeDTO.setTradeId(id); // Ensure DTO id matches path id
            Trade amendedTrade = tradeService.amendTrade(id, tradeDTO);
            TradeDTO responseDTO = tradeMapper.toDto(amendedTrade);

            // FIX: Ensures JSON includes tradeId
            return ResponseEntity.ok(responseDTO);

        } catch (Exception e) {
            logger.error("Error updating trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error updating trade: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTrade(@PathVariable Long id) {
        logger.info("Deleting trade with id: {}", id);
        try {
            tradeService.deleteTrade(id);

            // FIX: Return 204 NO CONTENT to match REST convention + test
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            logger.error("Error deleting trade: {}", e.getMessage(), e);

            // FIX: Changed from returning 200 to 400 to properly reflect error
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/terminate")
    public ResponseEntity<?> terminateTrade(@PathVariable Long id) {
        logger.info("Terminating trade with id: {}", id);
        try {
            Trade terminatedTrade = tradeService.terminateTrade(id);
            TradeDTO responseDTO = tradeMapper.toDto(terminatedTrade);
            return ResponseEntity.ok(responseDTO); // 200 OK is correct here
        } catch (Exception e) {
            logger.error("Error terminating trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error terminating trade: " + e.getMessage());
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<?> cancelTrade(@PathVariable Long id) {
        logger.info("Cancelling trade with id: {}", id);
        try {
            Trade cancelledTrade = tradeService.cancelTrade(id);
            TradeDTO responseDTO = tradeMapper.toDto(cancelledTrade);
            return ResponseEntity.ok(responseDTO); // 200 OK is correct here
        } catch (Exception e) {
            logger.error("Error cancelling trade: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error cancelling trade: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    // This method uses that criteriaDTO to find matching trades in the database and
    // returns them as a list of TradeDTO objects. criteriaDTO tells the service
    // what to search for and List<TradeDTO> is the
    // search result returned.
    public ResponseEntity<List<TradeDTO>> searchTrades(SearchCriteriaDTO criteriaDTO) {
        List<TradeDTO> results = tradeService.searchTrades(criteriaDTO);
        return ResponseEntity.ok(results);
    }

    // @GetMapping("/rsql")
    // public ResponseEntity<List<TradeDTO>> searchTradesRsql(@RequestParam String
    // query) {

    // List<TradeDTO> trades = tradeService.searchTradesRsql(query);

    // return ResponseEntity.ok(trades);
    // }

}
