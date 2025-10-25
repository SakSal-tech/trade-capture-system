package com.technicalchallenge.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

// This DTO is used to safely expose audit information to ADMIN and
// MIDDLE_OFFICE users.
// It omits internal database fields and focuses only on what is relevant for
// display.
public class AdditionalInfoAuditDTO {

    private Long id; // Unique audit record ID
    private Long tradeId; // The trade this audit relates to
    private String fieldName; // e.g., "SETTLEMENT_INSTRUCTIONS"
    private String oldValue; // The previous value before the change
    private String newValue; // The updated value after the change
    private String changedBy; // Who made the change
    private LocalDateTime changedAt; // When the change happened

}
