package com.technicalchallenge.controller;

import com.technicalchallenge.dto.AdditionalInfoAuditDTO;
import com.technicalchallenge.dto.AdditionalInfoDTO;
import com.technicalchallenge.dto.AdditionalInfoRequestDTO;
import com.technicalchallenge.dto.TradeDTO;
import com.technicalchallenge.mapper.AdditionalInfoAuditMapper;
import com.technicalchallenge.mapper.TradeMapper;
import com.technicalchallenge.model.AdditionalInfoAudit;
import com.technicalchallenge.model.Trade;
import com.technicalchallenge.repository.AdditionalInfoAuditRepository;
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
import java.util.Map;
import java.util.HashMap;

// Added: Swagger imports to ensure developers, auditors, or testers can open /swagger-ui.html and use endpoints
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

// Added. Security imports to implement access rights
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@Tag(name = "Trade Settlement Instructions", description = "Endpoints for managing trade settlement instructions and audit history")
@RequestMapping("/api/trades")
public class TradeSettlementController {

    // Short: logger for diagnostic traces (principal and authorities)
    private static final Logger logger = LoggerFactory.getLogger(TradeSettlementController.class);

    // Developer note: settlement instructions are stored in AdditionalInfo;
    // keep controller thin and delegate business logic/audit to
    // AdditionalInfoService.

    @Autowired
    private TradeService tradeService;

    @Autowired
    private TradeMapper tradeMapper;

    @Autowired
    private AdditionalInfoService additionalInfoService;

    @Autowired
    private AdditionalInfoAuditMapper additionalInfoAuditMapper;

    @Autowired
    private AdditionalInfoAuditRepository additionalInfoAuditRepository;

    /**
     * Refactored ADDED:
     * This endpoint was improved to delegate all searching logic to the service
     * layer,
     * rather than loading all records and filtering in the controller.
     *
     * WHY:
     * - Improves performance (database handles search instead of Java loops)
     * - Keeps controller "thin" handles only request/response logic
     * - Easier to test and reuse (search logic is now in AdditionalInfoService)
     *
     * Business Requirement:
     * "Search capability for finding trades with specific settlement requirements."
     */
    @GetMapping("/search/settlement-instructions")
    @PreAuthorize("hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT')")
    // OpenAPI: @Operation provides a short summary and description displayed
    // in Swagger UI. @ApiResponses lists common HTTP responses for the endpoint.
    @Operation(summary = "Search trades by settlement instruction text", description = "Performs a case-insensitive partial match across settlement instructions and returns matching trades as trade DTOs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of trades matching the search"),
            @ApiResponse(responseCode = "400", description = "Invalid or empty search text provided")
    })
    public ResponseEntity<?> searchBySettlementInstructions(@RequestParam String instructions) {

        // If user provides nothing or only spaces, return HTTP 400
        if (instructions == null || instructions.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Search text cannot be empty.");
        }

        // Trim removes any extra spaces from both ends of user input.
        String trimmedInput = instructions.trim();

        // Refactored: Instead of calling searchByKey() (which loads all records),
        // now call a dedicated service method that performs a focused database
        // query.
        List<AdditionalInfoDTO> matchingInfos = additionalInfoService.searchTradesBySettlementKeyword(trimmedInput);

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
    // OpenAPI: Document this read endpoint for Swagger UI.
    @Operation(summary = "Get settlement instructions for a trade", description = "Returns settlement instructions DTO for the specified trade ID if present.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settlement instructions found and returned"),
            @ApiResponse(responseCode = "404", description = "Trade or settlement instructions not found")
    })
    public ResponseEntity<?> getSettlementInstructions(@PathVariable Long id) {

        // Confirm trade exists
        Optional<Trade> tradeOpt = tradeService.getTradeById(id);
        if (tradeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Trade not found with ID: " + id);
        }

        /*
         * -Moved logic into AdditionalInfoService so authorization,
         * validation, audit and persistence are handled in one place (cleaner
         * code, easier tests, stronger security).
         * 200 = record exists and you can view it.
         * 404 = no settlement instructions saved for that trade (not a
         * permission issue).
         * 403 = you are not the trade owner and lack privileges.
         * - Rows disappearing after restart? That's caused by
         * spring.jpa.hibernate.ddl-auto=create-drop. Change to 'update' or
         */
        try {
            AdditionalInfoDTO dto = additionalInfoService.getSettlementInstructionsByTradeId(id);
            if (dto == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No settlement instructions found for trade ID: " + id);
            }

            // ADDED: run server-side non-standard detection and include result in response
            // (keeps UX consistent in Swagger and enables clients to show the message)
            String keyword = additionalInfoService.alertNonStandardSettlementKeyword(id);
            Map<String, Object> resp = new HashMap<>();
            resp.put("additionalInfo", dto);
            // keep top-level compatibility with existing clients that expect
            // the DTO fields at the root of the response (e.g., fieldValue)
            resp.put("fieldValue", dto.getFieldValue());
            resp.put("nonStandardKeyword", keyword);
            String msg = (keyword != null) ? "Trade contains non-standard settlement instruction: " + keyword
                    : "No non-standard settlement instruction detected.";
            resp.put("message", msg);

            // If a non-standard settlement keyword was found return an
            // additional response header and a message so UIs (or Swagger)
            // can highlight the response (e.g., show in red). Red signals
            // attention: such settlement instructions often require manual
            // handling by operations or risk teams.
            if (keyword != null) {
                return ResponseEntity.ok().header("X-NonStandard-Keyword", keyword).body(resp);
            }
            return ResponseEntity.ok(resp);
        } catch (org.springframework.security.access.AccessDeniedException ade) {
            // Let the global exception handler map this to a 403 with a clear
            // message, but return here for clarity in the controller flow.
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Insufficient privileges to view settlement instructions for trade " + id);
        }
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
    @PreAuthorize("hasAnyRole('TRADER','SALES','MIDDLE_OFFICE','ADMIN')")
    // Refactored. Sales aand middle office and now allowed to create/update
    // settlements. Before it was traders only.
    @Operation(summary = "Create or update settlement instructions", description = "Creates or updates the settlement instructions for a trade and records an audit entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Settlement instructions created/updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or validation failed"),
            @ApiResponse(responseCode = "404", description = "Trade not found")
    })
    public ResponseEntity<?> updateSettlementInstructions(
            @Parameter(name = "id", description = "Trade id", required = true) @PathVariable Long id,
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
        // Read current authenticated principal for audit and diagnostics
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String changedBy = (auth != null && auth.getName() != null && !auth.getName().isBlank()) ? auth.getName()
                : "ANONYMOUS";

        // Short: log principal and granted authorities to aid debugging of 403 cases
        try {
            logger.debug("Settlement PUT principal='{}' authorities='{}' tradeId='{}'",
                    auth != null ? auth.getName() : "ANONYMOUS",
                    auth != null ? auth.getAuthorities() : "NONE",
                    id);
        } catch (Exception logEx) {
            // keep main flow unchanged if logging fails
        }

        // Delegate logic to service layer (handles both create & update)
        AdditionalInfoDTO result = additionalInfoService.upOrInsertTradeSettlementInstructions(id, text, changedBy);

        // ADDED: Run server-side non-standard detection after save and include a
        // short message in the response (keeps clients able to show detection results)
        String keyword = additionalInfoService.alertNonStandardSettlementKeyword(id);
        Map<String, Object> resp = new HashMap<>();
        resp.put("additionalInfo", result);
        // preserve compatibility: include the saved fieldValue at top-level
        resp.put("fieldValue", result.getFieldValue());
        resp.put("nonStandardKeyword", keyword);
        String msg = (keyword != null) ? "Trade contains non-standard settlement instruction: " + keyword
                : "No non-standard settlement instruction detected.";
        resp.put("message", msg);

        // After creating/updating, include a header/message when non-standard
        // text is detected. Clients can use this to emphasise the response in
        // red because non-standard settlement notes are high-risk/manual.
        if (keyword != null) {
            return ResponseEntity.ok().header("X-NonStandard-Keyword", keyword).body(resp);
        }
        return ResponseEntity.ok(resp);
    }

    /**
     * Soft-delete settlement instructions for a trade (marks as inactive).
     * Only users with edit privileges may delete. A deletion is recorded in the
     * audit trail.
     */
    @DeleteMapping("/{id}/settlement-instructions")
    @PreAuthorize("hasAnyRole('TRADER','SALES','MIDDLE_OFFICE','ADMIN')")
    @Operation(summary = "Delete settlement instructions", description = "Soft-delete settlement instructions for a trade and record an audit entry.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Settlement instructions deleted (soft-delete)"),
            @ApiResponse(responseCode = "404", description = "Trade or settlement instructions not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges to delete")
    })
    public ResponseEntity<?> deleteSettlementInstructions(@PathVariable Long id) {

        // Confirm trade exists
        Optional<Trade> tradeOpt = tradeService.getTradeById(id);
        if (tradeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Trade not found with ID: " + id);
        }

        try {
            boolean deleted = additionalInfoService.deleteSettlementInstructions(id);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No settlement instructions found for trade ID: " + id);
            }
            // No content on successful soft-delete
            // Success: the settlement instruction row was deactivated. Returns
            // 204 No Content to match REST conventions for delete-like operations.
            return ResponseEntity.noContent().build();
        } catch (org.springframework.security.access.AccessDeniedException ade) {
            // Authorization failed: the caller is not the trade owner and does
            // not have an elevated edit role. The global exception handler may
            // also convert this to a JSON 403 body; here I return a clear text
            // message for backwards compatibility with older clients.
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Insufficient privileges to delete settlement instructions for trade " + id);
        }
    }

    /**
     * Delete a specific AdditionalInfo row by its primary key.
     * Useful for operators to remove duplicate rows discovered in the
     * additional_info table. This endpoint performs the same security checks
     * as other settlement operations: trade-owned records require edit rights
     * on the trade; non-trade records require admin/superuser authority.
     *
     * Example: DELETE /api/additional-info/123
     */
    @DeleteMapping("/additional-info/{additionalInfoId}")
    @PreAuthorize("hasAnyRole('TRADER','SALES','MIDDLE_OFFICE','ADMIN')")
    @Operation(summary = "Delete AdditionalInfo row by id", description = "Soft-delete a specific AdditionalInfo row (deactivate) and write an audit record.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "AdditionalInfo deactivated"),
            @ApiResponse(responseCode = "404", description = "AdditionalInfo not found"),
            @ApiResponse(responseCode = "403", description = "Insufficient privileges")
    })
    public ResponseEntity<?> deleteAdditionalInfoById(@PathVariable Long additionalInfoId) {
        try {
            boolean deleted = additionalInfoService.deleteAdditionalInfoById(additionalInfoId);
            if (!deleted) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("AdditionalInfo not found with ID: " + additionalInfoId);
            }
            // Success: the AdditionalInfo row was deactivated. This endpoint is
            // typically used by operators to remove duplicate rows discovered in
            // the `additional_info` table. It is purposely precise (accepts the
            // PK) to avoid accidental removal of the wrong record when
            // duplicates exist.
            return ResponseEntity.noContent().build();
        } catch (org.springframework.security.access.AccessDeniedException ade) {
            // Authorization failed: the caller lacks the required privileges to
            // remove this row. For trade-linked rows the caller must be the
            // trade owner or an elevated editor (ROLE_SUPERUSER/ROLE_ADMIN/
            // ROLE_MIDDLE_OFFICE depending on configuration). For non-trade
            // rows an admin/superuser is required.
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Insufficient privileges to delete AdditionalInfo ID: " + additionalInfoId);
        }
    }

    //// Added this endpoint to extend the "Audit Trail" business requirement.
    // Allows ADMIN and MIDDLE_OFFICE users to view a record of all settlement
    // instruction changes.
    @GetMapping("/{id}/audit-trail")
    @PreAuthorize("hasAnyRole('ADMIN','MIDDLE_OFFICE')")
    // OpenAPI: Expose audit trail endpoint details in Swagger UI.
    @Operation(summary = "Get audit trail for settlement instructions", description = "Returns the history of changes to settlement instructions for a trade, ordered by most recent change.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Audit trail records returned"),
            @ApiResponse(responseCode = "404", description = "No audit history found for the trade ID")
    })
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
    // Supports business requirement “Settlement instructions visible to all user
    // types.”
    public Optional<AdditionalInfoDTO> getTradeSettlementInstructions(Long tradeId) {

        if (tradeId == null || tradeId <= 0) {
            throw new IllegalArgumentException("Trade ID must be valid.");
        }

        // Delegate to the service layer which returns Optional<AdditionalInfoDTO>,
        // avoiding direct repository Optional references in this class.
        return additionalInfoService.getTradeSettlementInstructions(tradeId);
    }

    @GetMapping("/{id}/settlement-instructions/identify-nonstandard")
    @PreAuthorize("hasAnyRole('TRADER','MIDDLE_OFFICE','SUPPORT')")
    public ResponseEntity<?> identifyNonStandard(@PathVariable Long id) {

        // call service method to detect nonstandard keyword in the settlement
        String keyword = additionalInfoService.alertNonStandardSettlementKeyword(id);
        // Create a response map to return small JSON. Spring (via Jackson) will
        // automatically convert a Java Map into a small JSON object without you having
        // to create a new DTO class
        Map<String, Object> resp = new HashMap<>();

        // add the trade id into the response so the caller can collect result
        resp.put("tradeId", id);
        // stores the detected keyword (or null)
        resp.put("nonStandardKeyword", keyword);
        // User message to alert nonstandard settlements
        String msg = (keyword != null) ? "Trade contains non-standard settlement instruction: " + keyword
                : "No non-standard settlement instruction detected.";
        resp.put("message", msg);// Add message to the response map under key "message".

        return ResponseEntity.ok(resp);
    }

}
