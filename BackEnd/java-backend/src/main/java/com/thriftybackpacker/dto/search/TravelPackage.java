package com.thriftybackpacker.dto.search;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * One combined flight + hotel package returned inside PackageSearchResponse.
 * Fields match the Phase 2 response contract used by the frontend.
 */
@Data
@Builder
public class TravelPackage {

    private String packageId;
    private BigDecimal flightCost;
    private BigDecimal hotelCost;
    private BigDecimal totalCost;
    private String provider;
    private List<String> activities;
}
