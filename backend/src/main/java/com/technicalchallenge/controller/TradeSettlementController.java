package com.technicalchallenge.controller;

import com.technicalchallenge.dto.AdditionalInfoDTO;
import com.technicalchallenge.dto.AdditionalInfoRequestDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.service.AdditionalInfoService;
import com.technicalchallenge.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Handles Settlement Instructions integration for trades.
 * 
 * This controller interacts with the AdditionalInfo table, which is used as a
 * flexible
 * key-value store for storing settlement instructions related to trades.
 * 
 * Key points:
 * - fieldName = "SETTLEMENT_INSTRUCTIONS"
 * - fieldValue = actual settlement text (optional)
 * - entityType = "TRADE"
 * - entityId = trade ID this info belongs to
 */
@RestController
@RequestMapping("/api/trades")
public class TradeSettlementController {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private TradeMapper tradeMapper;

    @Autowired
    private AdditionalInfoService additionalInfoService;

    /**
     * Search trades by settlement instruction content.
     * 
     * Accessible to TRADER, MIDDLE_OFFICE, and SUPPORT roles.
     */
    @GetMapping("/search/settlement-instructions")
    @PreAuthorize("hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT')")
    public ResponseEntity<?> searchBySettlementInstructions(@RequestParam String instructions) {

        // If user provides nothing or only spaces, return HTTP 400
        if (instructions == null || instructions.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Search text cannot be empty.");
        }

        // Trim removes any extra spaces from both ends of user input.
        String trimmedInput = instructions.trim();

        // This will search all records that contain the given text
        List<AdditionalInfoDTO> matchingInfos = additionalInfoService.searchByKey(trimmedInput);

        // Use a Set to ensure unique trade IDs (avoid duplicates)
        Set<Long> tradeIdSet = new HashSet<>();

        // Loop through each matching record
        // Keep only records that belong to the "TRADE" entity type
        for (AdditionalInfoDTO info : matchingInfos) {
            if (info.getEntityType() != null && info.getEntityType().equalsIgnoreCase("TRADE")) {
                tradeIdSet.add(info.getEntityId());
            }
        }

        // Convert the Set to a List to pass into the service method
        List<Long> tradeIds = new ArrayList<>(tradeIdSet);

        // If no matches were found, return an empty list (not an error)
        if (tradeIds.isEmpty()) {
            return ResponseEntity.ok(new ArrayList<TradeDTO>());
        }

        // Retrieve the trade entities using their IDs
        List<Trade> trades = tradeService.getTradesByIds(tradeIds);

        // Convert the trade entities to DTOs for safe frontend use
        List<TradeDTO> tradeDTOs = new ArrayList<>();
        for (Trade trade : trades) {
            TradeDTO tradeDTO = tradeMapper.toDto(trade);
            tradeDTOs.add(tradeDTO);
        }

        // Return matching trades as JSON
        return ResponseEntity.ok(tradeDTOs);
    }

    /**
     * Create or update settlement instructions for a trade.
     * 
     * Editable by TRADER and SALES roles only.
     * 
     * Assignment requirement: settlement instructions are OPTIONAL.
     * If not provided, no validation errors should be thrown.
     */
    @PutMapping("/{id}/settlement-instructions")
    @PreAuthorize("hasAnyRole('TRADER','SALES')")
    public ResponseEntity<?> updateSettlementInstructions(
            @PathVariable Long id,
            @RequestBody AdditionalInfoRequestDTO infoRequest) {

        // Verify that the trade exists before attempting update
        Optional<Trade> tradeOpt = tradeService.getTradeById(id);
        if (tradeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Trade not found with ID: " + id);
        }

        // Basic request validation
        if (infoRequest == null) {
            return ResponseEntity.badRequest().body("Request body cannot be null.");
        }

        // The field name must always be "SETTLEMENT_INSTRUCTIONS"
        if (!"SETTLEMENT_INSTRUCTIONS".equalsIgnoreCase(infoRequest.getFieldName())) {
            return ResponseEntity.badRequest()
                    .body("Invalid fieldName. Expected 'SETTLEMENT_INSTRUCTIONS'.");
        }

        /*
         * Settlement instructions are OPTIONAL.
         * 
         * If no value is provided (null or blank), do not reject the request.
         * This allows trades that do not yet have settlement details to be saved or
         * updated.
         * 
         * However, if the user provides only spaces, normalise it to null.
         * This avoids storing meaningless whitespace in the database.
         */
        String fieldValue = infoRequest.getFieldValue();
        if (fieldValue != null) {
            String trimmedValue = fieldValue.trim();
            if (trimmedValue.isEmpty()) {
                // Treat blank-only input as "no instructions provided"
                infoRequest.setFieldValue(null);
            } else {
                // Store the cleaned-up (trimmed) text
                infoRequest.setFieldValue(trimmedValue);
            }
        }

        // Define which entity this record belongs to
        infoRequest.setEntityType("TRADE");
        infoRequest.setEntityId(id);

        // --- Check for an existing record for this trade ---
        List<AdditionalInfoDTO> existingInfos = additionalInfoService.searchByKey("SETTLEMENT_INSTRUCTIONS");
        AdditionalInfoDTO existingRecord = null;

        // Loop through and find the matching record by ID
        for (AdditionalInfoDTO info : existingInfos) {
            if (info.getEntityId().equals(id)) {
                existingRecord = info;
                break; // stop once found
            }
        }

        // --- Create or update depending on existence ---
        AdditionalInfoDTO result;
        if (existingRecord != null) {
            // Update the existing record
            result = additionalInfoService.updateAdditionalInfo(existingRecord.getAdditionalInfoId(), infoRequest);
        } else {
            // Create a new record if none exists
            result = additionalInfoService.createAdditionalInfo(infoRequest);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Retrieve settlement instructions for a specific trade.
     * 
     * Accessible to TRADER, MIDDLE_OFFICE, and SUPPORT roles.
     */
    @GetMapping("/{id}/settlement-instructions")
    @PreAuthorize("hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT')")
    public ResponseEntity<?> getSettlementInstructions(@PathVariable Long id) {

        // Confirm trade exists
        Optional<Trade> tradeOpt = tradeService.getTradeById(id);
        if (tradeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Trade not found with ID: " + id);
        }

        // Retrieve all settlement-related additional info records
        List<AdditionalInfoDTO> allInfos = additionalInfoService.searchByKey("SETTLEMENT_INSTRUCTIONS");

        AdditionalInfoDTO record = null;

        // Find the record for this specific trade
        for (AdditionalInfoDTO info : allInfos) {
            if (info.getEntityType().equalsIgnoreCase("TRADE")
                    && info.getEntityId().equals(id)) {
                record = info;
                break;
            }
        }

        // If no record is found, return a 404, but not an error, just "no data yet"
        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No settlement instructions found for trade ID: " + id);
        }

        return ResponseEntity.ok(record);
    }
}
