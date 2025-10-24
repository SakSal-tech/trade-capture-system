package com.technicalchallenge.service;

import com.technicalchallenge.dto.AdditionalInfoDTO;
import com.technicalchallenge.dto.AdditionalInfoRequestDTO;
import com.technicalchallenge.mapper.AdditionalInfoMapper;
import com.technicalchallenge.model.AdditionalInfo;
import com.technicalchallenge.repository.AdditionalInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.List;

/**
 * Service layer that handles business logic related to AdditionalInfo.
 *
 * This service is responsible for validating, saving, updating,
 * and searching for additional information records.
 *
 * Validation rules:
 * - Settlement instructions are optional.
 * - If provided, must be between 10 and 500 characters.
 * - Must not contain unsafe characters or SQL injection attempts.
 * - Should support structured multi-line text (label:value style).
 */
@Service
@Transactional
public class AdditionalInfoService {

    private final AdditionalInfoRepository additionalInfoRepository;
    private final AdditionalInfoMapper additionalInfoMapper;

    public AdditionalInfoService(AdditionalInfoRepository additionalInfoRepository,
            AdditionalInfoMapper additionalInfoMapper) {
        this.additionalInfoRepository = additionalInfoRepository;
        this.additionalInfoMapper = additionalInfoMapper;
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
         * it is perfectly fine — we skip further checks.
         * 
         * VALIDATION RULE 2: If provided, it must meet length and content standards.
         */
        String fieldValue = additionalInfoRequestDTO.getFieldValue();
        if (fieldValue != null && !fieldValue.trim().isEmpty()) {
            fieldValue = fieldValue.trim();

            /*
             * VALIDATION RULE 2A: Length Check.
             * Ensures the text is long enough to be meaningful (>=10 chars)
             * but not excessively long (<=500 chars) which could affect performance or
             * readability.
             */
            if (fieldValue.length() < 10 || fieldValue.length() > 500) {
                throw new IllegalArgumentException("Field value must be between 10 and 500 characters when provided.");
            }

            /*
             * VALIDATION RULE 3: Content Validation: No special characters that could cause
             * security issues to prevent SQL injection.
             * Checking for suspicious patterns such as:
             * - Semicolons (;)
             * - Comment markers (--)
             * - Common SQL keywords like DROP TABLE or DELETE FROM
             * 
             * These could indicate an attempt to inject SQL code into the system.
             */
            if (fieldValue.contains(";") ||
                    fieldValue.contains("--") ||
                    fieldValue.toLowerCase().contains("drop table") ||
                    fieldValue.toLowerCase().contains("delete from")) {
                throw new IllegalArgumentException("Field value contains unsafe or invalid characters.");
            }

            /*
             * VALIDATION RULE 4: Structured format support
             * Using a regular expression (regex) to ensure the text follows
             * a structured and clean pattern.
             * 
             * The regex below allows:
             * - Letters and numbers (a-z, A-Z, 0-9)
             * - Spaces, commas, periods, colons, semicolons
             * - Hyphens (-), slashes (/)
             * - Line breaks (\n, \r)
             * 
             * This supports formats like:
             * "Account: 123456\nBeneficiary: ABC Bank\nCurrency: GBP"
             * 
             * It also rejects dangerous symbols such as <, >, %, $, etc.
             */
            if (!fieldValue.matches("^[a-zA-Z0-9 ,.:;/\\-\\n\\r]+$")) {
                throw new IllegalArgumentException(
                        "Field value format not supported. Only structured text is allowed.");
            }

        } else {
            // Field is optional — no value provided is perfectly acceptable
            fieldValue = null;
        }

        // entityType tells us which entity this additional info belongs to (e.g.,
        // "TRADE")
        String entityType = additionalInfoRequestDTO.getEntityType();
        if (entityType == null || entityType.trim().isEmpty()) {
            throw new IllegalArgumentException("Entity type must be provided (e.g. TRADE).");
        }
        entityType = entityType.trim();

        // entityId identifies which record (trade, counterparty, etc.) this info is
        // linked to
        Long entityId = additionalInfoRequestDTO.getEntityId();
        if (entityId == null || entityId <= 0) {
            throw new IllegalArgumentException("Entity ID must be a valid positive number.");
        }

        // Map validated DTO to entity for persistence
        AdditionalInfo additionalInfo = additionalInfoMapper.toEntity(additionalInfoRequestDTO);

        // Save the entity in the database
        AdditionalInfo savedInfo = additionalInfoRepository.save(additionalInfo);

        // Convert back to DTO for safe return to the controller
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

        // Fetch the record or throw an exception if not found
        AdditionalInfo existingAdditionalInfo = additionalInfoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("AdditionalInfo not found with ID: " + id));

        // Validate updated value using same rules as create()
        String updatedValue = requestDTO.getFieldValue();
        if (updatedValue != null && !updatedValue.trim().isEmpty()) {
            updatedValue = updatedValue.trim();

            if (updatedValue.length() < 10 || updatedValue.length() > 500) {
                throw new IllegalArgumentException("Field value must be between 10 and 500 characters when provided.");
            }

            if (updatedValue.contains(";") ||
                    updatedValue.contains("--") ||
                    updatedValue.toLowerCase().contains("drop table") ||
                    updatedValue.toLowerCase().contains("delete from")) {
                throw new IllegalArgumentException("Field value contains unsafe or invalid characters.");
            }

            if (!updatedValue.matches("^[a-zA-Z0-9 ,.:;/\\-\\n\\r]+$")) {
                throw new IllegalArgumentException(
                        "Field value format not supported. Only structured text is allowed.");
            }
        }

        // Mapper updates only relevant fields (avoiding accidental overwrites)
        additionalInfoMapper.updateEntityFromRequest(existingAdditionalInfo, requestDTO);

        // Save the updated entity
        AdditionalInfo updatedInfo = additionalInfoRepository.save(existingAdditionalInfo);

        // Convert back to DTO for response
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

        // Trim and lowercase to make the search user-friendly (case-insensitive)
        String lowerKeyword = keyword.trim().toLowerCase();

        // Retrieve all records from the database
        List<AdditionalInfo> allRecords = additionalInfoRepository.findAll();

        // Collect records that match the keyword
        List<AdditionalInfo> matchingRecords = new ArrayList<>();
        for (AdditionalInfo record : allRecords) {
            if (record.getFieldValue() != null) {
                String fieldValue = record.getFieldValue().toLowerCase();
                if (fieldValue.contains(lowerKeyword)) {
                    matchingRecords.add(record);
                }
            }
        }

        // Convert matching entities into DTOs for safe return
        List<AdditionalInfoDTO> result = new ArrayList<>();
        for (AdditionalInfo record : matchingRecords) {
            result.add(additionalInfoMapper.toDto(record));
        }

        return result;
    }
}
