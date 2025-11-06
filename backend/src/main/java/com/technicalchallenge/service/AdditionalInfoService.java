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
import org.springframework.context.ApplicationEventPublisher;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import com.technicalchallenge.repository.TradeRepository;
import com.technicalchallenge.security.UserPrivilegeValidator;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service layer for managing AdditionalInfo records (free-form fields tied to
 * domain entities such as trades).
 *
 * Responsibilities:
 * - Validate incoming AdditionalInfo payloads (special handling for
 * settlement instructions).
 * - Create, update and soft-delete AdditionalInfo rows.
 * - Provide targeted queries used by the operations UI (search and lookup by
 * trade id).
 * - Maintain a separate audit trail for changes to settlement instructions.
 *
 * Behaviour and error modes:
 * - Methods validate inputs and throw IllegalArgumentException for bad input.
 * - Authorization is enforced server-side (AccessDeniedException) where
 * required; the class prefers a central UserPrivilegeValidator when
 * available and falls back to inline checks to preserve testability.
 *
 * Note: this class performs soft-deletes (preserves historical rows) and
 * writes explicit audit records whenever settlement instructions are
 * changed or deactivated.
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
    private final ApplicationEventPublisher applicationEventPublisher;

    // Optional: validator injected by Spring at runtime. Tests that construct the
    // service directly without Spring will observe a null validator reference; in
    // that case the service falls back to the original, inline ownership logic
    // to preserve existing unit tests.
    @Autowired(required = false)
    private UserPrivilegeValidator userPrivilegeValidator;

    /**
     * Refactor after tests failed, I added extra parameter
     * ApplicationEventPublisher — t later after
     * tests writing. Backwards-compatible constructor used by older tests
     * and callers that do
     * not provide an ApplicationEventPublisher. Delegates to the full
     * constructor with a null publisher.
     */
    public AdditionalInfoService(AdditionalInfoRepository additionalInfoRepository,
            AdditionalInfoMapper additionalInfoMapper,
            AdditionalInfoAuditRepository additionalInfoAuditRepository,
            TradeValidationEngine tradeValidationEngine,
            TradeRepository tradeRepository) {
        this(additionalInfoRepository, additionalInfoMapper, additionalInfoAuditRepository, tradeValidationEngine,
                tradeRepository, null);
    }

    @Autowired
    // Added a new arg so Spring Application Events so other components can react to
    // settlement changes without tight coupling
    public AdditionalInfoService(AdditionalInfoRepository additionalInfoRepository,
            AdditionalInfoMapper additionalInfoMapper,
            AdditionalInfoAuditRepository additionalInfoAuditRepository,
            TradeValidationEngine tradeValidationEngine,
            TradeRepository tradeRepository,
            ApplicationEventPublisher applicationEventPublisher) {
        this.additionalInfoRepository = additionalInfoRepository;
        this.additionalInfoMapper = additionalInfoMapper;
        this.additionalInfoAuditRepository = additionalInfoAuditRepository;
        this.tradeValidationEngine = tradeValidationEngine;
        this.tradeRepository = tradeRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Creates a new AdditionalInfo record.
     *
     * Validates the provided DTO and persists a new AdditionalInfo entity.
     *
     * Inputs: an AdditionalInfoRequestDTO
     * Returns: the saved AdditionalInfoDTO
     * Errors: throws IllegalArgumentException for invalid input.
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
     * Validates the new content and updates the entity identified by id.
     *
     * Inputs: id of the AdditionalInfo record and an AdditionalInfoRequestDTO
     * Returns: the updated AdditionalInfoDTO
     * Errors: IllegalArgumentException if the id is not found or the new
     * content is invalid.
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
     * Retrieve the active settlement instructions for a trade.
     *
     * Security: performs ownership/privilege checks and will throw
     * AccessDeniedException if the caller is not allowed to view the trade.
     *
     * Returns: AdditionalInfoDTO or null when no settlement instructions exist.
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
     * Search for trades where the settlement instructions contain the
     * supplied keyword (case-insensitive, partial match).
     *
     * Inputs: search keyword
     * Returns: list of AdditionalInfoDTO (possibly empty)
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
     * Create or update settlement instructions for a trade.
     *
     * Behaviour: if an active record exists it is updated, otherwise a new
     * record is created. Validation and authorization are performed.
     *
     * Returns: saved AdditionalInfoDTO
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
        // Note: do NOT set fieldType on the DTO (request object) because
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
     * Upsert (create or update) settlement instructions with explicit
     * authorization and audit trail.
     *
     * Inputs: tradeId, settlementText and changedBy (informational only;
     * actual actor is derived from the SecurityContext)
     * Returns: saved AdditionalInfoDTO
     * Errors: AccessDeniedException when the caller is not allowed to edit the
     * target trade; IllegalArgumentException for invalid content.
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
                                || "ROLE_MIDDLE_OFFICE".equalsIgnoreCase(ga) || "ROLE_ADMIN".equalsIgnoreCase(ga)
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

        // Publish a domain event so listeners (notifications, SSE, metrics)
        // can react to the settlement instructions change. The event payload is
        // intentionally lightweight and contains the old/new values for
        // convenience.
        try { // there is no tradeid 0
            long longTradeId = (tradeId != null) ? tradeId.longValue() : 0L;
            applicationEventPublisher.publishEvent(
                    new com.technicalchallenge.Events.SettlementInstructionsUpdatedEvent(
                            String.valueOf(tradeId), // trade id as string for UI listeners
                            longTradeId,
                            authUser, // username taken from SecurityContext
                            Instant.now().truncatedTo(ChronoUnit.MILLIS), // event timestamp (truncated to ms)
                            Map.of("oldValue", oldValue, "newValue", settlementText))); // map with previous and new.
                                                                                        // Gives listeners a quick diff:
                                                                                        // they can show "changed from X
                                                                                        // to Y"
                                                                                        // settlement text
        } catch (Exception ex) {
            // Ensure publishing failures do not break the primary DB transaction.Publishing
            // should happen synchronously in this method and occurs after the DB save/audit
            // Log and continue (listeners should be resilient).
            // Using System.err here to avoid adding a logger to this large service
            // class; in future I can use a dedicated logger or metrics.
            System.err.println("Failed to publish SettlementInstructionsUpdatedEvent: " + ex.getMessage());
        }

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

    // Adding a simple rule-based alert so the system can automatically flag
    // trades whose settlement instructions look "non-standard" (words like
    // "manual", "offshore", "warehouse", etc.). This helps operations and risk find
    // problematic trades quickly and is the first

    public String alertNonStandardSettlementKeyword(Long tradeId) {
        // validate the tradeId, and We fetch the active AdditionalInfo record for
        // settlement instructions for that trade
        if (tradeId == null || tradeId <= 0)
            throw new IllegalArgumentException("Trade ID must be valid.");
        AdditionalInfo record = additionalInfoRepository.findActiveByEntityTypeAndEntityIdAndFieldName("TRADE", tradeId,
                "SETTLEMENT_INSTRUCTIONS");// existing query

        // If there are no settlement instructions, return null (nothing non-standard to
        // detect)
        if (record == null || record.getFieldValue() == null)
            return null;
        // Convert the stored text to lowercase so matching is case-insensitive
        String text = record.getFieldValue().toLowerCase();
        // DVP = Delivery-Versus=Payment: securities are delivered only if payment is
        // made
        String[] nonStandardS = new String[] { "manual", "non-dvp", "non dvp", "offshore", "physical", "warehouse" };

        for (int i = 0; i < nonStandardS.length; i++) {
            if (text.contains(nonStandardS[i]))
                return nonStandardS[i];
        }

        // If nonStandardS is found, return null to indicate "no match".
        return null;

    }

    /**
     * Soft-delete (deactivate) settlement instructions for a trade.
     *
     * Behavior: marks the active settlement AdditionalInfo row as inactive and
     * writes an audit record containing the previous value and the actor.
     *
     * Returns: true when a record was found and deactivated; false when no
     * active record existed.
     *
     * Authorization: only the trade owner or users with elevated edit roles
     * are permitted; AccessDeniedException is thrown otherwise.
     *
     * Note: this method assumes at most one active settlement row per trade.
     * Use deleteAdditionalInfoById for operator-precise cleanup of duplicates.
     */
    @Transactional
    public boolean deleteSettlementInstructions(Long tradeId) {
        if (tradeId == null || tradeId <= 0)
            throw new IllegalArgumentException("Trade ID must be valid.");

        // Find active settlement record for the trade
        AdditionalInfo existing = additionalInfoRepository.findActiveByEntityTypeAndEntityIdAndFieldName(
                "TRADE", tradeId, "SETTLEMENT_INSTRUCTIONS");

        // Nothing to delete
        if (existing == null) {
            return false;
        }

        // Authorization: reuse the same ownership checks as for updates
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        com.technicalchallenge.model.Trade trade = tradeRepository.findLatestActiveVersionByTradeId(tradeId)
                .orElse(null);
        String authUser = (auth != null && auth.getName() != null && !auth.getName().isBlank())
                ? auth.getName()
                : "SYSTEM";

        if (userPrivilegeValidator != null) {
            if (!userPrivilegeValidator.canEditTrade(trade, auth)) {
                throw new AccessDeniedException(
                        "Insufficient privileges to delete settlement instructions for trade " + tradeId);
            }
        } else {
            // NOTE: align fallback edit authority with other service paths
            // (e.g., upOrInsertTradeSettlementInstructions and deleteAdditionalInfoById)
            // The controller already allows MIDDLE_OFFICE; the service must also
            // recognise ROLE_MIDDLE_OFFICE and ROLE_ADMIN as elevated editors.
            boolean canEditOthers = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                    .anyMatch(a -> {
                        String ga = a.getAuthority();
                        return "ROLE_SALES".equalsIgnoreCase(ga)
                                || "ROLE_SUPERUSER".equalsIgnoreCase(ga)
                                || "ROLE_MIDDLE_OFFICE".equalsIgnoreCase(ga)
                                || "ROLE_ADMIN".equalsIgnoreCase(ga)
                                || "TRADE_EDIT_ALL".equalsIgnoreCase(ga);
                    });

            String ownerLogin = (trade != null && trade.getTraderUser() != null
                    && trade.getTraderUser().getLoginId() != null)
                            ? trade.getTraderUser().getLoginId()
                            : null;

            if (ownerLogin != null && !ownerLogin.equalsIgnoreCase(authUser) && !canEditOthers) {
                throw new AccessDeniedException(
                        "Insufficient privileges to delete settlement instructions for trade " + tradeId);
            }
        }

        // Perform soft-delete
        existing.setActive(false);
        existing.setDeactivatedDate(java.time.LocalDateTime.now());
        additionalInfoRepository.save(existing);

        // Audit the deletion
        AdditionalInfoAudit audit = new AdditionalInfoAudit();
        audit.setTradeId(tradeId);
        audit.setFieldName("SETTLEMENT_INSTRUCTIONS");
        audit.setOldValue(existing.getFieldValue());
        audit.setNewValue(null);
        audit.setChangedBy(authUser);
        audit.setChangedAt(java.time.LocalDateTime.now());
        additionalInfoAuditRepository.save(audit);

        // Publish an event for listeners to react to the deletion.
        try {
            long longTradeId;
            if (tradeId != null) {
                longTradeId = tradeId.longValue();
            }
            longTradeId = 0L;

            applicationEventPublisher.publishEvent(
                    new com.technicalchallenge.Events.SettlementInstructionsUpdatedEvent(
                            String.valueOf(tradeId),
                            longTradeId,
                            authUser,
                            Instant.now().truncatedTo(ChronoUnit.MILLIS),
                            Map.of("oldValue", existing.getFieldValue(), "newValue", null)));
        } catch (Exception ex) {
            System.err.println("Failed to publish SettlementInstructionsUpdatedEvent (delete): " + ex.getMessage());// Publish
                                                                                                                    // a
                                                                                                                    // event
                                                                                                                    // SettlementInstructionsUpdatedEvent
                                                                                                                    // so
                                                                                                                    // other
                                                                                                                    // components
                                                                                                                    // (notifications,
                                                                                                                    // metrics,
                                                                                                                    // SSE,
                                                                                                                    // etc.)
                                                                                                                    // can
                                                                                                                    // react
                                                                                                                    // to
                                                                                                                    // settlement
                                                                                                                    // instruction
                                                                                                                    // changes.
        }

        return true;
    }

    /**
     * Soft-delete (deactivate) a single AdditionalInfo record by its primary
     * key. Intended for operator use to remove duplicates or incorrect rows.
     *
     * Authorization: for records linked to trades the caller must be allowed
     * to edit the trade (owner or elevated role). For non-trade records the
     * caller must have admin/superuser/middle-office authority.
     *
     * Returns: true when a change was made, false when the record did not
     * exist or was already inactive.
     */
    @Transactional
    public boolean deleteAdditionalInfoById(Long additionalInfoId) {
        if (additionalInfoId == null || additionalInfoId <= 0)
            throw new IllegalArgumentException("AdditionalInfo ID must be valid.");

        AdditionalInfo record = additionalInfoRepository.findById(additionalInfoId).orElse(null);
        if (record == null) {
            return false;
        }

        // If already inactive, nothing to do
        if (record.getActive() != null && !record.getActive()) {
            return false;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String authUser = (auth != null && auth.getName() != null && !auth.getName().isBlank())
                ? auth.getName()
                : "SYSTEM";

        String entityType = record.getEntityType();

        if ("TRADE".equalsIgnoreCase(entityType)) {
            Long tradeId = record.getEntityId();
            com.technicalchallenge.model.Trade trade = tradeRepository.findLatestActiveVersionByTradeId(tradeId)
                    .orElse(null);

            if (userPrivilegeValidator != null) {
                if (!userPrivilegeValidator.canEditTrade(trade, auth)) {
                    throw new AccessDeniedException(
                            "Insufficient privileges to delete this additional info record: " + additionalInfoId);
                }
            } else {
                boolean canEditOthers = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                        .anyMatch(a -> {
                            String ga = a.getAuthority();
                            return "ROLE_SALES".equalsIgnoreCase(ga) || "ROLE_SUPERUSER".equalsIgnoreCase(ga)
                                    || "ROLE_MIDDLE_OFFICE".equalsIgnoreCase(ga) || "ROLE_ADMIN".equalsIgnoreCase(ga)
                                    || "TRADE_EDIT_ALL".equalsIgnoreCase(ga);
                        });

                String ownerLogin = (trade != null && trade.getTraderUser() != null
                        && trade.getTraderUser().getLoginId() != null)
                                ? trade.getTraderUser().getLoginId()
                                : null;

                if (ownerLogin != null && !ownerLogin.equalsIgnoreCase(authUser) && !canEditOthers) {
                    throw new AccessDeniedException(
                            "Insufficient privileges to delete this additional info record: " + additionalInfoId);
                }
            }
        } else {
            // For non-trade entityTypes require elevated authority
            boolean hasAdmin = auth != null && auth.getAuthorities() != null && auth.getAuthorities().stream()
                    .anyMatch(a -> {
                        String ga = a.getAuthority();
                        return "ROLE_SUPERUSER".equalsIgnoreCase(ga) || "ROLE_ADMIN".equalsIgnoreCase(ga)
                                || "ROLE_MIDDLE_OFFICE".equalsIgnoreCase(ga);
                    });
            if (!hasAdmin) {
                throw new AccessDeniedException("Insufficient privileges to delete this additional info record: "
                        + additionalInfoId);
            }
        }

        // Soft-delete. it does not delete from the database but marks it as cancelled
        record.setActive(false);
        record.setDeactivatedDate(java.time.LocalDateTime.now());
        additionalInfoRepository.save(record);

        // Audit
        AdditionalInfoAudit audit = new AdditionalInfoAudit();
        audit.setTradeId(record.getEntityId());
        audit.setFieldName(record.getFieldName());
        audit.setOldValue(record.getFieldValue());
        audit.setNewValue(null);
        audit.setChangedBy(authUser);
        audit.setChangedAt(java.time.LocalDateTime.now());
        additionalInfoAuditRepository.save(audit);

        return true;
    }

    /*
     * Notes about deleteAdditionalInfoById:
     * - Purpose: provides a precise operator-facing way to deactivate a single
     * AdditionalInfo row by its primary key. This is the recommended approach
     * when cleaning up duplicate rows because it avoids ambiguity about which
     * row will be removed.
     * - Authorization: For records linked to trades the same ownership/edit
     * checks apply (owner or elevated edit roles). For non-trade records the
     * method requires admin/superuser (or middle-office where appropriate).
     * - Audit: the method always writes an audit record with the old value and
     * the actor who performed the deletion. This keeps the audit trail intact
     * and allows recovery/analysis after cleanup.
     */

}
