package com.technicalchallenge.mapper;

import com.technicalchallenge.dto.AdditionalInfoDTO;
import com.technicalchallenge.dto.AdditionalInfoRequestDTO;
import com.technicalchallenge.model.AdditionalInfo;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

/**
 * Handles conversion between AdditionalInfo entities and DTOs.
 * 
 * Refactored ADDED:
 * - Validation checks: Prevent null-pointer errors.
 * - Version tracking: Supports audit history requirement.
 * - Business logic awareness: Keeps timestamps consistent for compliance.
 */
@Component
public class AdditionalInfoMapper {

    /**
     * Converts an AdditionalInfo entity (from the database)
     * into a DTO to send to the frontend or API clients.
     *
     * BUSINESS REQUIREMENT:
     * Supports data visibility — ensures UI can view structured
     * settlement instructions safely without exposing database internals.
     */
    public AdditionalInfoDTO toDto(AdditionalInfo entity) {
        if (entity == null) {
            return null; // Validation check: avoids NullPointerException
        }

        AdditionalInfoDTO dto = new AdditionalInfoDTO();
        dto.setAdditionalInfoId(entity.getAdditionalInfoId());
        dto.setEntityType(entity.getEntityType());
        dto.setEntityId(entity.getEntityId());
        dto.setFieldName(entity.getFieldName());
        dto.setFieldValue(entity.getFieldValue());
        dto.setFieldType(entity.getFieldType());
        dto.setActive(entity.getActive());
        dto.setCreatedDate(entity.getCreatedDate());
        dto.setLastModifiedDate(entity.getLastModifiedDate());
        dto.setDeactivatedDate(entity.getDeactivatedDate());
        dto.setVersion(entity.getVersion()); // Refactored ADDED: supports audit/version tracking
        return dto;
    }

    /**
     * Converts an AdditionalInfoRequestDTO into a new AdditionalInfo entity.
     *
     * BUSINESS REQUIREMENT:
     * Used when creating new settlement instructions (TRADER/SALES users).
     * Ensures new entries are marked active and have consistent timestamps.
     */
    public AdditionalInfo toEntity(AdditionalInfoRequestDTO request) {
        if (request == null) {
            return null; // Validation check
        }

        AdditionalInfo entity = new AdditionalInfo();
        entity.setEntityType(request.getEntityType());
        entity.setEntityId(request.getEntityId());
        entity.setFieldName(request.getFieldName());
        entity.setFieldValue(request.getFieldValue());
        entity.setFieldType("STRING"); // Default type for text-based settlement data
        entity.setActive(true);
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedDate(LocalDateTime.now());
        entity.setVersion(1); // Refactored ADDED: first version of record for audit trail so ti tracks how
                              // many times a record changed. Prevents overwriting older updates by mistake if
                              // multiple users edit simultaneously.
        return entity;
    }

    /**
     * Updates an existing AdditionalInfo entity with new data from a request DTO.
     *
     * BUSINESS REQUIREMENT:
     * Supports trade amendment handling — updating existing settlement instructions
     * while maintaining version history and modification timestamps.
     */
    public void updateEntityFromRequest(AdditionalInfo entity, AdditionalInfoRequestDTO request) {
        if (entity == null || request == null) {
            return; // Validation check
        }

        // Only update mutable fields (fieldValue). Other data like entityType remains
        // stable.
        if (request.getFieldValue() != null) {
            entity.setFieldValue(request.getFieldValue());
        }

        // Track the edit time and increment version number for audit trail
        entity.setLastModifiedDate(LocalDateTime.now());
        entity.setVersion(entity.getVersion() == null ? 1 : entity.getVersion() + 1); // Refactored ADDED version to
                                                                                      // keep track how many times a
                                                                                      // record changes by increasing
                                                                                      // version by one.
    }
}
