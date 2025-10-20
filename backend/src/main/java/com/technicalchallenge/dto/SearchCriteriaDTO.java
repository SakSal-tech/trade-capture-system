package com.technicalchallenge.dto;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SearchCriteriaDTO {
    /**
     * This search DTO This DTO is used only to receive the search parameters from
     * the request and keeps endpoint clean and validation logic simple.
     * Otherwise, I would have to make every field in the tradeDTOoptional and have
     * validation annotations (like @NotNull, @NotBlank) that don't apply when
     * searching. It would confuse API: /api/trades/search should accept filters,
     * not full trade objects.
     * The same DTO would be serving two totally different use cases vs
     * input, which violates the Single Responsibility Principle.
     */

    private String counterparty;
    private String book;
    private String trader;
    private String status;
    // Added: This tells Spring: “expect yyyy-MM-dd format when binding URL query
    // parameters to these fields.” Without it, Spring was throwing a 400 if it
    // can't interpret an empty or oddly formatted value
    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate startDate;
    @DateTimeFormat(iso = ISO.DATE)
    private LocalDate endDate;
    // Adding this line to force commit

}
