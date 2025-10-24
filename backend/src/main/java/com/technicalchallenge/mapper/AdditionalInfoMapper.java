package com.technicalchallenge.mapper;

import com.technicalchallenge.dto.AdditionalInfoDTO;
import com.technicalchallenge.dto.AdditionalInfoRequestDTO;
import com.technicalchallenge.model.AdditionalInfo;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

// for converting between AdditionalInfo entities and DTOs.

@Component
public class AdditionalInfoMapper {

    /**
     * Converts an AdditionalInfo entity (from the database)
     * into a DTO to send to the frontend or API clients.
     *
     * @param entity The AdditionalInfo entity retrieved from the database
     * @return A DTO representing the same data in a frontend-friendly format
     */
    public AdditionalInfoDTO toDto(AdditionalInfo entity) {
        if (entity == null) {
            return null;
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
        return dto;
    }

    /**
     * Converts an AdditionalInfoRequestDTO into a new AdditionalInfo entity.
     *
     * This is typically used when creating a new record
     * (for example, when a trader adds settlement instructions).
     *
     * @param request The incoming request data from the frontend
     * @return A new AdditionalInfo entity ready to be persisted
     */
    public AdditionalInfo toEntity(AdditionalInfoRequestDTO request) {
        if (request == null) {
            return null;
        }

        AdditionalInfo entity = new AdditionalInfo();
        entity.setEntityType(request.getEntityType());
        entity.setEntityId(request.getEntityId());
        entity.setFieldName(request.getFieldName());
        entity.setFieldValue(request.getFieldValue());
        entity.setFieldType("STRING"); // Default type for now
        entity.setActive(true);
        entity.setCreatedDate(LocalDateTime.now());
        entity.setLastModifiedDate(LocalDateTime.now());
        return entity;
    }

    /**
     * Updates an existing AdditionalInfo entity with new data
     * from a request DTO.
     *
     * This is used when updating an existing field value
     * (for example, when a trader edits settlement instructions).
     *
     * @param entity  The existing entity fetched from the database
     * @param request The updated values from the frontend
     */
    public void updateEntityFromRequest(AdditionalInfo entity, AdditionalInfoRequestDTO request) {
        if (entity == null || request == null) {
            return;
        }

        entity.setFieldValue(request.getFieldValue());
        entity.setLastModifiedDate(LocalDateTime.now());
    }
}
