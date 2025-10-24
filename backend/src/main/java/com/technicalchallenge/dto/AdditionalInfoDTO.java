package com.technicalchallenge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) for the AdditionalInfo entity.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalInfoDTO {

    private Long additionalInfoId; // Unique ID (primary key)
    private String entityType; // e.g. "TRADE", "COUNTERPARTY"
    private Long entityId; // ID of the related entity record (e.g. Trade ID)
    private String fieldName; // The name of the key (e.g. "SETTLEMENT_INSTRUCTIONS")
    private String fieldValue; // The stored data (e.g. "Settle via JPM New York...")
    private String fieldType; // Data type ("STRING", "NUMBER", etc.)
    private Boolean active; // Whether this record is active
    private LocalDateTime createdDate; // When this record was created
    private LocalDateTime lastModifiedDate; // When this record was last updated
    private LocalDateTime deactivatedDate; // When this record was deactivated (if applicable)
}
