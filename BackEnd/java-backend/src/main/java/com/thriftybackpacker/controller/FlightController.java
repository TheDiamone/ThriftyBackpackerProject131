package com.thriftybackpacker.controller;

import com.thriftybackpacker.service.RapidApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Passes real RapidAPI flight search results directly to the caller.
 * Mirrors Python rapidapi.py: GET /api/v1/flights/search
 *
 * The frontend flightService.js calls GET /api/v1/flights/search with these query params:
 *   from_code, to_code, depart_date, adults, currency, order_by, flight_type, cabin_class,
 *   locale, page_number, return_date (optional), children_ages (optional)
 */
@Tag(name = "Flight Search", description = "Real-time flight search via RapidAPI (booking-com.p.rapidapi.com)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FlightController {

    private final RapidApiClient rapidApiClient;

    @Operation(
            summary = "Search flights",
            description = "Calls booking-com.p.rapidapi.com/v1/flights/search and returns the raw response. " +
                    "from_code format: 'LHR.AIRPORT'. Requires RAPIDAPI_KEY."
    )
    @GetMapping("/flights/search")
    public Map<String, Object> searchFlights(
            @RequestParam String from_code,
            @RequestParam String to_code,
            @RequestParam String depart_date,
            @RequestParam(defaultValue = "1") int adults,
            @RequestParam(defaultValue = "AED") String currency,
            @RequestParam(defaultValue = "BEST") String order_by,
            @RequestParam(defaultValue = "ONEWAY") String flight_type,
            @RequestParam(defaultValue = "ECONOMY") String cabin_class,
            @RequestParam(defaultValue = "en-gb") String locale,
            @RequestParam(defaultValue = "0") int page_number,
            @RequestParam(required = false) String return_date,
            @RequestParam(required = false) String children_ages) {

        return rapidApiClient.searchFlights(
                from_code, to_code, depart_date, adults,
                currency, order_by, flight_type, cabin_class,
                locale, page_number, return_date, children_ages);
    }
}
