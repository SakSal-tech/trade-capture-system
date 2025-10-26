package com.technicalchallenge.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/*This class is used only for data coming in (requests).

The controller will accept this DTO in POST or PUT methods.

The backend will convert it to an AdditionalInfo entity using Mapper.

After saving, the backend returns an AdditionalInfoDTO (the response)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalInfoRequestDTO {

    private String entityType; // e.g. "TRADE"
    private Long entityId; // e.g. Trade ID 1234
    private String fieldName; // e.g. "SETTLEMENT_INSTRUCTIONS"
    private String fieldValue; // e.g. "Settle via JPM New York, Account: 123456789"
}
