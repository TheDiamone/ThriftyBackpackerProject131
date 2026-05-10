package com.thriftybackpacker.dto.search;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Internal normalized flight object produced from the raw RapidAPI response.
 * Not exposed directly to the client — used only by PackageSearchService.
 */
@Data
@Builder
public class FlightOption {

    private String id;
    private String provider;
    private BigDecimal price;  // BigDecimal — never double/float
    private String currency;
    private String departureTime;
    private String arrivalTime;
}
