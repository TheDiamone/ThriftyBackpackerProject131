package com.thriftybackpacker.dto.search;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for POST /api/v1/search/packages.
 * Field names match the Phase 2 contract expected by the frontend.
 */
@Data
public class PackageSearchRequest {

    @NotBlank(message = "destination is required")
    private String destination;

    @NotBlank(message = "startDate is required")
    private String startDate;

    @NotBlank(message = "endDate is required")
    private String endDate;

    @NotNull @Min(value = 1, message = "travelers must be at least 1")
    private Integer travelers;

    @DecimalMin(value = "0.01", message = "budget must be positive")
    private BigDecimal budget;

    /** Optional. Defaults to JFK inside PackageSearchService if omitted. */
    private String origin;
}
