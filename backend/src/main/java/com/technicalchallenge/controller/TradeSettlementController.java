package com.technicalchallenge.controller;

import com.technicalchallenge.dto.AdditionalInfoAuditDTO;
import com.technicalchallenge.dto.AdditionalInfoDTO;
import com.technicalchallenge.dto.AdditionalInfoRequestDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.mapper.AdditionalInfoAuditMapper;
import com.technicalchallenge.mapper.AdditionalInfoMapper;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.AdditionalInfo;
import com.technicalchallenge.model.AdditionalInfoAudit;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.repository.AdditionalInfoAuditRepository;
import com.technicalchallenge.repository.AdditionalInfoRepository;
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
    private AdditionalInfoMapper additionalInfoMapper;

    @Autowired
    private AdditionalInfoService additionalInfoService;

    @Autowired
    private AdditionalInfoAuditMapper additionalInfoAuditMapper;

    @Autowired
    private AdditionalInfoAuditRepository additionalInfoAuditRepository;

    @Autowired
    private AdditionalInfoRepository additionalInfoRepository;

    /**
     * Refactored ADDED:
     * This endpoint was improved to delegate all searching logic to the service
     * layer,
     * rather than loading all records and filtering in the controller.
     *
     * WHY:
     * - Improves performance (database handles search instead of Java loops)
     * - Keeps controller "thin" — handles only request/response logic
     * - Easier to test and reuse (search logic is now in AdditionalInfoService)
     *
     * Business Requirement:
     * "Search capability for finding trades with specific settlement requirements."
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

        // Refactored: Instead of calling searchByKey() (which loads all records),
        // we now call a dedicated service method that performs a focused database
        // query.
        List<AdditionalInfoDTO> matchingInfos = additionalInfoService.searchTradesBySettlementText(trimmedInput);

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
     * Refactored ADDED:
     * - The controller no longer loops through all AdditionalInfo records to find
     * one match.
     * - Instead, it now calls a single-purpose service method
     * (getTradeSettlementInstructions)
     * that uses a focused JPA query.
     *
     * WHY:
     * - Improves performance (fetches only one matching record, not all)
     * - Keeps controller code cleaner and easier to understand
     * - Moves business logic closer to the service layer, where it belongs
     *
     * Business Requirement:
     * "Settlement instructions visible to all roles but editable by TRADER and
     * SALES only."
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

        // Refactored: Instead of manually searching through all records,
        // the controller now delegates to the service for a single lookup.
        Optional<AdditionalInfoDTO> recordOpt = additionalInfoService.getTradeSettlementInstructions(id);

        // If not found, return a 404 message
        if (recordOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No settlement instructions found for trade ID: " + id);
        }

        // Return the found record as a DTO
        return ResponseEntity.ok(recordOpt.get());
    }

    /**
     * Refactored ADDED:
     * - Previously, the controller handled searching and updating logic directly.
     * - Now, all complex validation, SQL-safety checks, and audit logging are
     * handled
     * by the service layer for cleaner separation of concerns.
     * 
     * BUSINESS REQUIREMENTS AS REQUESTED:
     * - Amendment Handling: Settlement instructions can be updated during trade
     * amendments.
     * - Access Control: Editable by TRADER and SALES roles only.
     * - Optional Field: Field may be blank or null (no forced validation).
     * - Audit Trail: Every change is automatically recorded by the service layer.
     */
    @PutMapping("/{id}/settlement-instructions")
    @PreAuthorize("hasAnyRole('TRADER','SALES')")
    public ResponseEntity<?> updateSettlementInstructions(
            @PathVariable Long id,
            @RequestBody AdditionalInfoRequestDTO infoRequest) {

        // Confirm trade existence
        Optional<Trade> tradeOpt = tradeService.getTradeById(id);
        if (tradeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Trade not found with ID: " + id);
        }

        // Validate request structure
        if (infoRequest == null || infoRequest.getFieldName() == null ||
                !"SETTLEMENT_INSTRUCTIONS".equalsIgnoreCase(infoRequest.getFieldName())) {
            return ResponseEntity.badRequest().body("fieldName must be SETTLEMENT_INSTRUCTIONS");
        }

        // Extract the settlement text (can be null or blank)
        String text = infoRequest.getFieldValue();

        // Record who made the change (placeholder for logged-in user)
        String changedBy = "CURRENT_USER";

        // Delegate logic to service layer (handles both create & update)
        AdditionalInfoDTO result = additionalInfoService.upOrInsertTradeSettlementInstructions(id, text, changedBy);

        return ResponseEntity.ok(result);
    }

    //// Added this endpoint to extend the "Audit Trail" business requirement.
    // Allows ADMIN and MIDDLE_OFFICE users to view a record of all settlement
    // instruction changes.
    @GetMapping("/{id}/audit-trail")
    @PreAuthorize("hasAnyRole('ADMIN','MIDDLE_OFFICE')")
    public ResponseEntity<?> getAuditTrail(@PathVariable Long id) {

        // --- Step 1: Retrieve all audit records for this trade ---
        List<AdditionalInfoAudit> auditRecords = additionalInfoAuditRepository.findByTradeIdOrderByChangedAtDesc(id);

        // If none found, return a helpful message
        if (auditRecords.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No audit history found for trade ID: " + id);
        }

        // Convert entities to DTOs using the mapper
        List<AdditionalInfoAuditDTO> auditDTOs = new ArrayList<>();
        for (AdditionalInfoAudit record : auditRecords) {
            auditDTOs.add(additionalInfoAuditMapper.toDto(record)); // Avoid exposing raw JPA entities
        }

        // Return the DTO list to the frontend
        return ResponseEntity.ok(auditDTOs);
    }

    // Refactored ADDED:
    // Fetches settlement instructions for a single trade directly from DB.
    // Supports business requirement — “Settlement instructions visible to all user
    // types.”
    public Optional<AdditionalInfoDTO> getTradeSettlementInstructions(Long tradeId) {

        if (tradeId == null || tradeId <= 0) {
            throw new IllegalArgumentException("Trade ID must be valid.");
        }

        // Delegate to the service layer which returns Optional<AdditionalInfoDTO>,
        // avoiding direct repository Optional references in this class.
        return additionalInfoService.getTradeSettlementInstructions(tradeId);
    }

}
