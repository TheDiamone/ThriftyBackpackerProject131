package com.thriftybackpacker.dto.search;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response envelope for POST /api/v1/search/packages.
 */
@Data
@Builder
public class PackageSearchResponse {

    private String destination;
    private BigDecimal budget;
    private List<TravelPackage> packages;
    private String message;
}
