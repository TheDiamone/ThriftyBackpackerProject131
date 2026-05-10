package com.thriftybackpacker.controller;

import com.thriftybackpacker.service.RapidApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Passes real RapidAPI attractions search results directly to the caller.
 * Mirrors Python rapidapi.py: GET /api/v1/attractions/search
 */
@Tag(name = "Attraction Search", description = "Real-time attraction search via RapidAPI (booking-com.p.rapidapi.com)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AttractionController {

    private final RapidApiClient rapidApiClient;

    @Operation(
            summary = "Search attractions",
            description = "Calls booking-com.p.rapidapi.com/v1/attractions/search. Requires RAPIDAPI_KEY."
    )
    @GetMapping("/attractions/search")
    public Map<String, Object> searchAttractions(
            @RequestParam String start_date,
            @RequestParam String end_date,
            @RequestParam String dest_id,
            @RequestParam(defaultValue = "en-gb") String locale,
            @RequestParam(defaultValue = "0") int page_number,
            @RequestParam(defaultValue = "AED") String currency,
            @RequestParam(defaultValue = "attr_book_score") String order_by) {

        return rapidApiClient.searchAttractions(
                start_date, end_date, dest_id, locale, page_number, currency, order_by);
    }
}
