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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public AdditionalInfoService(AdditionalInfoRepository additionalInfoRepository,
            AdditionalInfoMapper additionalInfoMapper,
            AdditionalInfoAuditRepository additionalInfoAuditRepository) {
        this.additionalInfoRepository = additionalInfoRepository;
        this.additionalInfoMapper = additionalInfoMapper;
        this.additionalInfoAuditRepository = additionalInfoAuditRepository;
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

        // Presence Check. Validate fieldName (required)
        String fieldName = additionalInfoRequestDTO.getFieldName();
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("Field name cannot be empty.");
        }
        fieldName = fieldName.trim();

        /*
         * VALIDATION RULE 1: Settlement instructions are optional.
         * If fieldValue (settlement instruction text) is blank or null,
         * it is perfectly fine to skip further checks.
         */
        String fieldValue = additionalInfoRequestDTO.getFieldValue();
        if (fieldValue != null && !fieldValue.trim().isEmpty()) {
            fieldValue = fieldValue.trim();

            // VALIDATION RULE 2A: Length Check
            if (fieldValue.length() < 10 || fieldValue.length() > 500) {
                throw new IllegalArgumentException("Field value must be between 10 and 500 characters when provided.");
            }

            // VALIDATION RULE 3: Content Validation protect against SQL injection
            // attempts
            if (fieldValue.contains(";") ||
                    fieldValue.contains("--") ||
                    fieldValue.toLowerCase().contains("drop table") ||
                    fieldValue.toLowerCase().contains("delete from")) {
                throw new IllegalArgumentException("Field value contains unsafe or invalid characters.");
            }

            // VALIDATION RULE 4: Structured format check ensure only safe structured text
            // Should support structured multi-line text (label:value style).
            if (!fieldValue.matches("^[a-zA-Z0-9 ,.:;/\\-\\n\\r]+$")) {
                throw new IllegalArgumentException(
                        "Field value format not supported. Only structured text is allowed.");
            }

        } else {
            // Field is optional no value provided is acceptable
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

            if (updatedValue.length() < 10 || updatedValue.length() > 500) {
                throw new IllegalArgumentException("Field value must be between 10 and 500 characters when provided.");
            }
            // Against SQL injection
            if (updatedValue.contains(";") ||
                    updatedValue.contains("--") ||
                    updatedValue.toLowerCase().contains("drop table") ||
                    updatedValue.toLowerCase().contains("delete from")) {
                throw new IllegalArgumentException("Field value contains unsafe or invalid characters.");
            }

            // Validates that settlement instructions contain only safe, structured
            // characters.
            // Allows letters, numbers, spaces, common punctuation, quotes, slashes,
            // hyphens, and line breaks.
            // Prevents unsafe symbols (<, >, $, %, etc.) to reduce injection risk.
            if (!updatedValue.matches("^[a-zA-Z0-9 ,.:;'\"/\\-\\n\\r]+$")) {
                throw new IllegalArgumentException(
                        "Field value format not supported. Only structured text is allowed.");
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
            audit.setChangedBy("SYSTEM_USER"); // Later replace with logged-in username
            audit.setChangedAt(LocalDateTime.now()); // Timestamp of the change

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

            if (settlementText.length() < 10 || settlementText.length() > 500) {
                throw new IllegalArgumentException("Settlement instructions must be between 10 and 500 characters.");
            }

            if (settlementText.contains(";") ||
                    settlementText.contains("--") ||
                    settlementText.toLowerCase().contains("drop table") ||
                    settlementText.toLowerCase().contains("delete from")) {
                throw new IllegalArgumentException("Settlement instructions contain unsafe or invalid characters.");
            }

            // Allow structured input with quotes and formatting safely
            if (!settlementText.matches("^[a-zA-Z0-9 ,.:;'\"/\\-\\n\\r]+$")) {
                throw new IllegalArgumentException("Unsupported format. Only structured text is allowed.");
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

            target = additionalInfoRepository.save(additionalInfoMapper.toEntity(dto));
        }

        // Write to audit trail
        AdditionalInfoAudit audit = new AdditionalInfoAudit();
        audit.setTradeId(tradeId);
        audit.setFieldName(FIELD_NAME);
        audit.setOldValue(oldValue);
        audit.setNewValue(settlementText);
        audit.setChangedBy(changedBy);
        audit.setChangedAt(java.time.LocalDateTime.now());
        // Save audit trail record separately
        additionalInfoAuditRepository.save(audit);

        // Convert to DTO and return
        return additionalInfoMapper.toDto(target);
    }

    // Refactored ADDED:
    // Supports business requirement — “Search capability for finding trades with
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
