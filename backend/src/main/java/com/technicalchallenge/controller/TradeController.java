package com.technicalchallenge.controller;

import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/trades")
@Validated
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
    public ResponseEntity<?> updateTrade(@PathVariable Long id, @Valid @RequestBody TradeDTO tradeDTO) {

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

            // FIX: Return 204 NO CONTENT → matches REST convention + test
            return ResponseEntity.noContent().build();

        } catch (Exception e) {
            logger.error("Error deleting trade: {}", e.getMessage(), e);

            // FIX: Changed from returning 200 → 400 to properly reflect error
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
}
