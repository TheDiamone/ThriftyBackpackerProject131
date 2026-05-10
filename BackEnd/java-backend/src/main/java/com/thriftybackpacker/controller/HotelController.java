package com.thriftybackpacker.controller;

import com.thriftybackpacker.service.RapidApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Passes real RapidAPI hotel search results directly to the caller.
 * Mirrors Python rapidapi.py: GET /api/v1/hotels/search
 *
 * The frontend hotelService.js calls GET /api/v1/hotels/search.
 * Note: dest_id is a Booking.com numeric city ID (e.g. -2601889 for London),
 * not an IATA code. The frontend sends dest_name/country_name instead of dest_id —
 * that is a mismatch in the existing Python integration that is preserved here.
 */
@Tag(name = "Hotel Search", description = "Real-time hotel search via RapidAPI (booking-com.p.rapidapi.com)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HotelController {

    private final RapidApiClient rapidApiClient;

    @Operation(
            summary = "Search hotels",
            description = "Calls booking-com.p.rapidapi.com/v1/hotels/search and returns the raw response. " +
                    "dest_id must be a Booking.com city ID (e.g. -2601889 for London). Requires RAPIDAPI_KEY."
    )
    @GetMapping("/hotels/search")
    public Map<String, Object> searchHotels(
            @RequestParam String dest_id,
            @RequestParam(defaultValue = "city") String dest_type,
            @RequestParam String checkin_date,
            @RequestParam String checkout_date,
            @RequestParam(defaultValue = "1") int adults_number,
            @RequestParam(defaultValue = "1") int room_number,
            @RequestParam(defaultValue = "0") int page_number,
            @RequestParam(defaultValue = "metric") String units,
            @RequestParam(defaultValue = "en-gb") String locale,
            @RequestParam(defaultValue = "AED") String filter_by_currency,
            @RequestParam(defaultValue = "popularity") String order_by,
            @RequestParam(defaultValue = "true") boolean include_adjacency,
            @RequestParam(defaultValue = "0") int children_number,
            @RequestParam(required = false) String children_ages,
            @RequestParam(required = false) String categories_filter_ids) {

        return rapidApiClient.searchHotels(
                dest_id, dest_type, checkin_date, checkout_date,
                adults_number, room_number, page_number, units,
                locale, filter_by_currency, order_by,
                include_adjacency, children_number, children_ages,
                categories_filter_ids);
    }
}
