package com.technicalchallenge.mapper;

import com.technicalchallenge.dto.AdditionalInfoAuditDTO;
import com.technicalchallenge.model.AdditionalInfoAudit;
import org.springframework.stereotype.Component;

// Responsible for converting between the audit entity and DTO.
// Keeps  service and controller layers clean and consistent.
@Component
public class AdditionalInfoAuditMapper {

    public AdditionalInfoAuditDTO toDto(AdditionalInfoAudit entity) {
        AdditionalInfoAuditDTO dto = new AdditionalInfoAuditDTO();
        dto.setId(entity.getId());
        dto.setTradeId(entity.getTradeId());
        dto.setFieldName(entity.getFieldName());
        dto.setOldValue(entity.getOldValue());
        dto.setNewValue(entity.getNewValue());
        dto.setChangedBy(entity.getChangedBy());
        dto.setChangedAt(entity.getChangedAt());
        return dto;
    }
}
