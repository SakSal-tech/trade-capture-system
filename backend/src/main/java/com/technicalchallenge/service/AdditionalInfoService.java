package com.technicalchallenge.service;

import com.technicalchallenge.dto.AdditionalInfoDTO;
import com.technicalchallenge.dto.AdditionalInfoRequestDTO;
import com.technicalchallenge.mapper.AdditionalInfoMapper;
import com.technicalchallenge.model.AdditionalInfo;
import com.technicalchallenge.model.AdditionalInfoAudit;
import com.technicalchallenge.repository.AdditionalInfoAuditRepository;
import com.technicalchallenge.repository.AdditionalInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// for validations
import com.technicalchallenge.validation.TradeValidationResult;
import com.technicalchallenge.validation.TradeValidationEngine;

//ADDED: let the service read the authenticated principal from Spring Security so the audit record can use the real username (with a sensible fallback).
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.security.UserPrivilegeValidator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This service is responsible for validating, saving, updating,
 * and searching for additional information records.
 */
@Service
@Transactional
public class AdditionalInfoService {

    private final AdditionalInfoRepository additionalInfoRepository;
    private final AdditionalInfoMapper additionalInfoMapper;
    private final AdditionalInfoAuditRepository additionalInfoAuditRepository;// Added: injecting the repository.
    private final TradeRepository tradeRepository;

    // central validation engine (refactor: prefer single entry point for
    // validations)
    private final TradeValidationEngine tradeValidationEngine;

    // Optional: validator injected by Spring at runtime. Tests that construct the
    // service directly without Spring will observe a null validator reference; in
    // that case the service falls back to the original, inline ownership logic
    // to preserve existing unit tests.
    @Autowired(required = false)
    private UserPrivilegeValidator userPrivilegeValidator;

    public AdditionalInfoService(AdditionalInfoRepository additionalInfoRepository,
            AdditionalInfoMapper additionalInfoMapper,
            AdditionalInfoAuditRepository additionalInfoAuditRepository,
            TradeValidationEngine tradeValidationEngine,
            TradeRepository tradeRepository) {
        this.additionalInfoRepository = additionalInfoRepository;
        this.additionalInfoMapper = additionalInfoMapper;
        this.additionalInfoAuditRepository = additionalInfoAuditRepository;
        this.tradeValidationEngine = tradeValidationEngine;
        this.tradeRepository = tradeRepository;
    }

    /**
     * Creates a new AdditionalInfo record.
     *
     * This method validates the incoming data before saving.
     * It ensures optional fields, content rules, and safety checks are met.
     */
    public AdditionalInfoDTO createAdditionalInfo(AdditionalInfoRequestDTO additionalInfoRequestDTO) {
        if (additionalInfoRequestDTO == null) {
            throw new IllegalArgumentException("AdditionalInfo request cannot be null.");
        }

        String fieldName = additionalInfoRequestDTO.getFieldName();
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be empty.");
        }
        fieldName = fieldName.trim();

        String fieldValue = additionalInfoRequestDTO.getFieldValue();
        if (fieldValue != null && !fieldValue.trim().isEmpty()) {
            fieldValue = fieldValue.trim();

            // REFACTOR NOTE: settlement-specific validation has been centralised
            // into `SettlementInstructionValidator`. If this AdditionalInfo record
            // is a settlement instruction, use the validator; otherwise preserve
            // the original lightweight checks for other fields.
            if ("SETTLEMENT_INSTRUCTIONS".equalsIgnoreCase(fieldName)) {
                // Use central engine entry point for settlement validation
                TradeValidationResult validationResult = tradeValidationEngine
                        .validateSettlementInstructions(fieldValue);
                if (!validationResult.isValid()) {
                    throw new IllegalArgumentException(validationResult.getErrors().get(0));
                }
            } else {
                // Non-settlement fields keep the previous quick validations
                if (fieldValue.length() < 10 || fieldValue.length() > 500) {
                    throw new IllegalArgumentException(
                            "Field value must be between 10 and 500 characters when provided.");
                }

                if (fieldValue.contains(";") ||
                        fieldValue.contains("--") ||
                        fieldValue.toLowerCase().contains("drop table") ||
                        fieldValue.toLowerCase().contains("delete from")) {
                    throw new IllegalArgumentException("Field value contains unsafe or invalid characters.");
                }

                if (!fieldValue.matches("^[a-zA-Z0-9 ,.:;/\\-\\n\\r]+$")) {
                    throw new IllegalArgumentException(
                            "Field value format not supported. Only structured text is allowed.");
                }
            }

        } else {
            fieldValue = null;
        }

        // Validate entityType and entityId
        String entityType = additionalInfoRequestDTO.getEntityType();
        if (entityType == null || entityType.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity type must be provided (e.g. TRADE).");
        }
        entityType = entityType.trim();

        Long entityId = additionalInfoRequestDTO.getEntityId();
        if (entityId == null || entityId <= 0) {
            throw new IllegalArgumentException("Entity ID must be a valid positive number.");
        }

        // Map validated DTO to entity and save it
        AdditionalInfo additionalInfo = additionalInfoMapper.toEntity(additionalInfoRequestDTO);

        // Defensive fix at entity-level: ensure fieldType is present before
        // saving. This prevents a DB NOT NULL constraint (field_type) from
        // causing a ConstraintViolationException which surfaces as HTTP 500.
        if (additionalInfo.getFieldType() == null || additionalInfo.getFieldType().trim().isEmpty()) {
            additionalInfo.setFieldType("STRING");
        }

        AdditionalInfo savedInfo = additionalInfoRepository.save(additionalInfo);
        return additionalInfoMapper.toDto(savedInfo);
    }

    /**
     * Updates an existing AdditionalInfo record.
     * Similar to create, but first checks if the record exists in the database.
     */
    public AdditionalInfoDTO updateAdditionalInfo(Long id, AdditionalInfoRequestDTO requestDTO) {

        if (requestDTO == null) {
            throw new IllegalArgumentException("Update request cannot be null.");
        }

        AdditionalInfo existingAdditionalInfo = additionalInfoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AdditionalInfo not found with ID: " + id));

        // Validate updated value
        String updatedValue = requestDTO.getFieldValue();
        if (updatedValue != null && !updatedValue.trim().isEmpty()) {
            updatedValue = updatedValue.trim();

            // REFACTOR NOTE: Moved validations to the SettlementInstructionValidator to use
            // central settlement validator for settlement fields
            // If existing record field is "SETTLEMENT_INSTRUCTIONS" run validator
            if ("SETTLEMENT_INSTRUCTIONS".equalsIgnoreCase(existingAdditionalInfo.getFieldName())) {
                // Use central engine entry point for settlement validation
                TradeValidationResult validationResult = tradeValidationEngine
                        .validateSettlementInstructions(updatedValue);
                if (!validationResult.isValid()) {
                    throw new IllegalArgumentException(validationResult.getErrors().get(0));
                }
            } else {
                // Kept the previous non-settlement validations for non-settlement fields and
                // not
                // moving it to the validoator class
                if (updatedValue.length() < 10 || updatedValue.length() > 500) {
                    throw new IllegalArgumentException(
                            "Field value must be between 10 and 500 characters when provided.");
                }
                // Against SQL injection
                if (updatedValue.contains(";") ||
                        updatedValue.contains("--") ||
                        updatedValue.toLowerCase().contains("drop table") ||
                        updatedValue.toLowerCase().contains("delete from")) {
                    throw new IllegalArgumentException("Field value contains unsafe or invalid characters.");
                }

                // Validates structured characters for non-settlement fields
                if (!updatedValue.matches("^[a-zA-Z0-9 ,.:;'\"/\\-\\n\\r]+$")) {
                    throw new IllegalArgumentException(
                            "Field value format not supported. Only structured text is allowed.");
                }
            }
        }

        additionalInfoMapper.updateEntityFromRequest(existingAdditionalInfo, requestDTO);
        AdditionalInfo updatedInfo = additionalInfoRepository.save(existingAdditionalInfo);

        // Refactored ADDED: Audit trail logic
        // Business purpose: Meets the "Audit Trail" requirement —
        // every change to settlement instructions must be recorded for accountability
        // and compliance.

        // Capturing what the old and new values are
        String oldValue = existingAdditionalInfo.getFieldValue(); // The value before change
        String newValue = updatedValue; // The newly submitted value

        // Comparing old vs new only write audit if something actually changed
        if (oldValue != null && !oldValue.equals(newValue)) {

            // Building a new audit record entity
            AdditionalInfoAudit audit = new AdditionalInfoAudit();
            audit.setTradeId(existingAdditionalInfo.getEntityId()); // Links audit to correct trade
            audit.setFieldName(existingAdditionalInfo.getFieldName()); // e.g. "SETTLEMENT_INSTRUCTIONS"
            audit.setOldValue(oldValue); // Store what it was before
            audit.setNewValue(newValue); // Store what it is now
            // Refactored:Added Security measures which reads the current authenticated
            // principal from
            // Spring Security and uses its username if available to show and record who
            // made
            // the change fallback is ANONYMOUS to used when there is no authenticated user

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String changedBy = (auth != null && auth.getName() != null && !auth.getName().isBlank()) ? auth.getName()
                    : "SYSTEM";
            audit.setChangedBy(changedBy);

            // Saving audit record so it appears in the audit trail endpoint
            additionalInfoAuditRepository.save(audit);// saving the audit after the main entity ensures that:The trade
                                                      // update definitely succeeded (no need to record failed
                                                      // attempts).
        }

        return additionalInfoMapper.toDto(updatedInfo);
    }

    /**
     * Searches for AdditionalInfo records containing the provided keyword.
     * Case-insensitive search on fieldValue.
     */
    public List<AdditionalInfoDTO> searchByKey(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Search keyword cannot be empty.");
        }

        String lowerKeyword = keyword.trim().toLowerCase();

        List<AdditionalInfo> allRecords = additionalInfoRepository.findAll();
        List<AdditionalInfo> matchingRecords = new ArrayList<>();
        for (AdditionalInfo record : allRecords) {
            if (record.getFieldValue() != null) {
                String fieldValue = record.getFieldValue().toLowerCase();
                if (fieldValue.contains(lowerKeyword)) {
                    matchingRecords.add(record);
                }
            }
        }

        List<AdditionalInfoDTO> result = new ArrayList<>();
        for (AdditionalInfo record : matchingRecords) {
            result.add(additionalInfoMapper.toDto(record));
        }
        return result;
    }

    // Refactoring and added dedicated service methods to efficiently manage,
    // search, and update trade settlement instructions, fulfilling business
    // requirements for direct integration, faster lookups, and cleaner controller
    // logic.

    /**
     * ADDED:
     * Retrieves settlement instructions for a specific trade.
     * Business Requirement:
     * - Supports “Operations Team Workflow” by making settlement info immediately
     * accessible.
     * - Required to display instructions on trade detail views and audit records.
     */
    public AdditionalInfoDTO getSettlementInstructionsByTradeId(Long tradeId) {
        // Use targeted repository query for efficient lookup (avoids loading all data)
        AdditionalInfo record = additionalInfoRepository.findActiveByEntityTypeAndEntityIdAndFieldName(
                "TRADE", tradeId, "SETTLEMENT_INSTRUCTIONS");

        // If no record exists, that's fine because settlement instructions are optional
        // as requested
        if (record == null) {
            return null;
        }

        /*
         * SECURITY: Ownership check
         * The service enforces that only the trade owner (the trader assigned to
         * the Trade) or users with elevated roles/privileges may view settlement
         * instructions. This defends against the scenario where a logged-in
         * trader (for example 'simon') could view another trader's (e.g. 'joey')
         * settlement instructions by supplying Joey's tradeId.
         *
         * Rationale and example (Simon/Joey scenario):
         * - Incident: Simon (ROLE_TRADER) was able to view and edit Joey's
         * settlement instructions by calling the endpoint with Joey's tradeId.
         * - Risk: This allows unauthorized access/modification of settlement data
         * and violates least-privilege principles.
         * - Fix: perform a server-side ownership check here (do not rely on any
         * client-supplied 'changedBy' or 'loginId').
         */
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        com.technicalchallenge.model.Trade trade = tradeRepository.findLatestActiveVersionByTradeId(tradeId)
                .orElse(null);
        String currentUser = (auth != null && auth.getName() != null && !auth.getName().isBlank())
                ? auth.getName()
                : "SYSTEM";

        // Prefer the centralised validator when available; fall back to the
        // previous inline ownership logic for environments (tests) where the validator
        // was not injected.
        if (userPrivilegeValidator != null) {
            if (!userPrivilegeValidator.canViewTrade(trade, auth)) {
                throw new AccessDeniedException(
                        "Insufficient privileges to view settlement instructions for trade " + tradeId);
            }
        } else {
            // Fallback: original, inline ownership check (keeps unit tests stable)
            boolean canViewOthers = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                    .anyMatch(a -> {
                        String ga = a.getAuthority();
                        return "ROLE_SALES".equalsIgnoreCase(ga) || "ROLE_SUPERUSER".equalsIgnoreCase(ga)
                                || "TRADE_VIEW_ALL".equalsIgnoreCase(ga);
                    });

            String ownerLogin = (trade != null && trade.getTraderUser() != null
                    && trade.getTraderUser().getLoginId() != null)
                            ? trade.getTraderUser().getLoginId()
                            : null;

            if (ownerLogin != null && !ownerLogin.equalsIgnoreCase(currentUser) && !canViewOthers) {
                throw new AccessDeniedException(
                        "Insufficient privileges to view settlement instructions for trade " + tradeId);
            }
        }

        // Convert entity to DTO to safely return to controller
        return additionalInfoMapper.toDto(record);
    }

    /**
     * ADDED:
     * Searches trades whose settlement instructions contain a given keyword.
     * Business Requirement:
     * - Supports “Operations Team Workflow” for quick text-based search.
     * - Enables partial, case-insensitive matching for flexible operational
     * queries.
     */
    public List<AdditionalInfoDTO> searchTradesBySettlementKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Search keyword cannot be empty.");
        }

        // Calls custom repository query that uses LIKE '%keyword%' for partial match
        List<AdditionalInfo> results = additionalInfoRepository.searchTradeSettlementByKeyword(keyword.trim());
        List<AdditionalInfoDTO> dtoList = new ArrayList<>();

        // Convert entity list to DTO list for frontend use
        for (AdditionalInfo result : results) {
            dtoList.add(additionalInfoMapper.toDto(result));
        }

        return dtoList;
    }

    /**
     * ADDED:
     * Creates or updates settlement instructions for a specific trade.
     * Business Requirement:
     * - Handles both creation and amendment of settlement details.
     * - Ensures there is only one active record per trade.
     * - Supports real-time updates visible to the operations team.
     */
    public AdditionalInfoDTO saveOrUpdateSettlementInstructions(Long tradeId, String instructions) {
        // Retrieve any existing active record for this trade
        AdditionalInfo existing = additionalInfoRepository.findActiveByEntityTypeAndEntityIdAndFieldName(
                "TRADE", tradeId, "SETTLEMENT_INSTRUCTIONS");

        // Build request object for mapping
        AdditionalInfoRequestDTO request = new AdditionalInfoRequestDTO();
        request.setEntityType("TRADE"); // Always tied to a trade
        request.setEntityId(tradeId); // Links record to the correct trade
        request.setFieldName("SETTLEMENT_INSTRUCTIONS");
        request.setFieldValue(instructions); // The new or updated instruction text
        // Note: we do NOT set fieldType on the DTO (request object) because
        // AdditionalInfoRequestDTO does not include that property. The mapper
        // and the entity-level defensive check in createAdditionalInfo will
        // ensure fieldType is populated before persistence to avoid 500 errors.

        // If the record already exists, update it else create a new one
        if (existing != null) {
            return updateAdditionalInfo(existing.getAdditionalInfoId(), request);
        } else {
            return createAdditionalInfo(request);
        }
    }

    /**
     * Refactored ADDED:
     * Handles creation or update (upOrinsert) of settlement instructions for a
     * trade.
     * To centralise business rules from the controller into the service layer.
     * - Supports both creation (insert) and modification (update) in one place.
     * - Ensures SQL injection validation, structured text, and audit trail logging.
     * BUSINESS REQUIREMENTS COVERED:
     * - Optional field (may be blank or omitted)
     * - Editable during trade amendments
     * - Full audit trail of all changes
     */
    @Transactional
    public AdditionalInfoDTO upOrInsertTradeSettlementInstructions(Long tradeId, String settlementText,
            String changedBy) {
        final String ENTITY_TYPE = "TRADE";
        final String FIELD_NAME = "SETTLEMENT_INSTRUCTIONS";

        // Try to find an existing active record for this trade
        AdditionalInfo existing = additionalInfoRepository.findActiveByEntityTypeAndEntityIdAndFieldName(
                ENTITY_TYPE, tradeId, FIELD_NAME);

        // Validate the input only if text is provided
        if (settlementText != null && !settlementText.trim().isEmpty()) {
            settlementText = settlementText.trim();

            // REFACTOR: centralised settlement validation via the validation engine
            TradeValidationResult validationResult = tradeValidationEngine
                    .validateSettlementInstructions(settlementText);
            if (!validationResult.isValid()) {
                throw new IllegalArgumentException(validationResult.getErrors().get(0));
            }
        }

        /*
         * SECURITY: Ownership check for edits
         *
         * We MUST enforce ownership server-side. Do not trust the caller-supplied
         * `changedBy` parameter for authorization (it can be spoofed). Derive the
         * caller from the SecurityContext and verify they are either the trade
         * owner or have an elevated role/privilege (for example, ROLE_SALES or
         * TRADE_EDIT_ALL). This prevents a logged-in trader (e.g. 'simon') from
         * updating another trader's (e.g. 'joey') settlement instructions just by
         * supplying Joey's tradeId.
         */
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        com.technicalchallenge.model.Trade trade = tradeRepository.findLatestActiveVersionByTradeId(tradeId)
                .orElse(null);
        String authUser = (auth != null && auth.getName() != null && !auth.getName().isBlank())
                ? auth.getName()
                : "SYSTEM";

        if (userPrivilegeValidator != null) {
            if (!userPrivilegeValidator.canEditTrade(trade, auth)) {
                throw new AccessDeniedException(
                        "Insufficient privileges to modify settlement instructions for trade " + tradeId);
            }
        } else {
            // Fallback: original inline check to preserve existing unit tests.
            boolean canEditOthers = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                    .anyMatch(a -> {
                        String ga = a.getAuthority();
                        return "ROLE_SALES".equalsIgnoreCase(ga) || "ROLE_SUPERUSER".equalsIgnoreCase(ga)
                                || "TRADE_EDIT_ALL".equalsIgnoreCase(ga);
                    });

            String ownerLogin = (trade != null && trade.getTraderUser() != null
                    && trade.getTraderUser().getLoginId() != null)
                            ? trade.getTraderUser().getLoginId()
                            : null;

            if (ownerLogin != null && !ownerLogin.equalsIgnoreCase(authUser) && !canEditOthers) {
                throw new AccessDeniedException(
                        "Insufficient privileges to modify settlement instructions for trade " + tradeId);
            }
        }

        // Handle upsert logic (create or update)
        AdditionalInfo target;
        String oldValue = null;

        if (existing != null) {
            // Update existing record
            oldValue = existing.getFieldValue();
            existing.setFieldValue(settlementText);
            target = additionalInfoRepository.save(existing);
        } else {
            // Create new record if none exists
            AdditionalInfoRequestDTO dto = new AdditionalInfoRequestDTO();
            dto.setEntityType(ENTITY_TYPE);
            dto.setEntityId(tradeId);
            dto.setFieldName(FIELD_NAME);
            dto.setFieldValue(settlementText);
            // Convert DTO to entity via mapper and then defensively ensure
            // fieldType is set on the entity before saving. This prevents the
            // database NOT NULL constraint on field_type from causing a
            // ConstraintViolationException (HTTP 500) if a caller omitted
            // the field type.
            AdditionalInfo entity = additionalInfoMapper.toEntity(dto);
            if (entity.getFieldType() == null || entity.getFieldType().trim().isEmpty()) {
                entity.setFieldType("STRING");
            }
            target = additionalInfoRepository.save(entity);
        }

        // Write to audit trail
        AdditionalInfoAudit audit = new AdditionalInfoAudit();
        audit.setTradeId(tradeId);
        audit.setFieldName(FIELD_NAME);
        audit.setOldValue(oldValue);
        audit.setNewValue(settlementText);
        // SECURITY: Use the authenticated principal as the changedBy actor in the
        // audit trail to prevent clients from spoofing the actor by supplying a
        // different 'changedBy' value. This preserves non-repudiation properties.
        audit.setChangedBy(authUser);
        audit.setChangedAt(java.time.LocalDateTime.now());
        // Save audit trail record separately
        additionalInfoAuditRepository.save(audit);

        // Convert to DTO and return
        return additionalInfoMapper.toDto(target);
    }

    // Refactored ADDED:
    // Supports business requirement “Search capability for finding trades with
    // specific settlement requirements.”
    public List<AdditionalInfoDTO> searchTradesBySettlementText(String keyword) {

        // Defensive check: keyword must not be empty or null
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new IllegalArgumentException("Search keyword cannot be empty.");
        }

        // Trim user input to avoid issues with extra spaces
        String trimmedKeyword = keyword.trim();

        // Uses repository-level query that performs case-insensitive partial search
        // This query runs directly in the database using LIKE %keyword%
        List<AdditionalInfo> matches = additionalInfoRepository.searchTradeSettlementByKeyword(trimmedKeyword);

        // Convert entities to DTOs for controller-safe output
        List<AdditionalInfoDTO> results = new ArrayList<>();
        for (AdditionalInfo match : matches) {
            results.add(additionalInfoMapper.toDto(match));
        }

        return results;
    }

    public Optional<AdditionalInfoDTO> getTradeSettlementInstructions(Long tradeId) {
        return additionalInfoRepository
                .findActiveOne("TRADE", tradeId, "SETTLEMENT_INSTRUCTIONS")
                .map(additionalInfoMapper::toDto);
    }

}
