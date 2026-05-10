package com.thriftybackpacker.dto.search;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Internal normalized hotel object produced from the raw RapidAPI response.
 * price = total cost for the entire stay (not nightly rate).
 * Not exposed directly to the client — used only by PackageSearchService.
 */
@Data
@Builder
public class HotelOption {

    private String id;
    private String provider;
    private BigDecimal price;  // BigDecimal — never double/float
    private String currency;
    private int nights;
}
