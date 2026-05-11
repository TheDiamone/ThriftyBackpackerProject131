package com.thriftybackpacker.controller;

import com.thriftybackpacker.service.LocationMapper;
import com.thriftybackpacker.service.RapidApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Hotel search — passes real RapidAPI results to the client.
 * Mirrors Python rapidapi.py: GET /api/v1/hotels/search
 *
 * The Vue frontend hotelService.js sends dest_name + country_name (not dest_id).
 * This controller resolves those to the correct Booking.com dest_id automatically.
 * dest_id can also be provided directly for direct API access or Swagger testing.
 */
@Tag(name = "Hotel Search", description = "Real-time hotel search via RapidAPI")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HotelController {

    private final RapidApiClient rapidApiClient;
    private final LocationMapper locationMapper;

    @Operation(
            summary = "Search hotels",
            description = "Calls booking-com.p.rapidapi.com/v1/hotels/search. " +
                    "Accepts either dest_id directly (e.g. -2601889 for London) " +
                    "OR dest_name (e.g. 'London') which is auto-resolved to dest_id. " +
                    "The Vue frontend sends dest_name — no manual dest_id lookup needed."
    )
    @GetMapping("/hotels/search")
    public ResponseEntity<?> searchHotels(
            @RequestParam(required = false) String dest_id,
            @RequestParam(required = false) String dest_name,
            @RequestParam(required = false) String country_name,
            @RequestParam(defaultValue = "city")   String dest_type,
            @RequestParam(required = false)         String checkin_date,
            @RequestParam(required = false)         String checkout_date,
            @RequestParam(defaultValue = "1")       int adults_number,
            @RequestParam(defaultValue = "1")       int room_number,
            @RequestParam(defaultValue = "0")       int page_number,
            @RequestParam(defaultValue = "metric")  String units,
            @RequestParam(defaultValue = "en-gb")   String locale,
            @RequestParam(defaultValue = "AED")     String filter_by_currency,
            @RequestParam(defaultValue = "popularity") String order_by,
            @RequestParam(defaultValue = "true")    boolean include_adjacency,
            @RequestParam(defaultValue = "0")       int children_number,
            @RequestParam(required = false)         String children_ages,
            @RequestParam(required = false)         String categories_filter_ids) {

        // Resolve dest_id: use provided value, or look up from city name
        String resolvedDestId = dest_id;
        if ((resolvedDestId == null || resolvedDestId.isBlank()) && dest_name != null) {
            resolvedDestId = locationMapper.hotelDestId(dest_name).orElse(null);
        }

        if (resolvedDestId == null || resolvedDestId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "detail", "Provide either dest_id or a recognised dest_name. " +
                            "Received dest_name='" + dest_name + "'. " +
                            "Supported cities: London, New York, Paris, Tokyo, Dubai, etc."));
        }

        Map<String, Object> result = rapidApiClient.searchHotels(
                resolvedDestId, dest_type, checkin_date, checkout_date,
                adults_number, room_number, page_number, units,
                locale, filter_by_currency, order_by,
                include_adjacency, children_number, children_ages,
                categories_filter_ids);

        return ResponseEntity.ok(result);
    }
}
