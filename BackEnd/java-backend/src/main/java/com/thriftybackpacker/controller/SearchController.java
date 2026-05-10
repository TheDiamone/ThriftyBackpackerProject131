package com.thriftybackpacker.controller;

import com.thriftybackpacker.dto.search.PackageSearchRequest;
import com.thriftybackpacker.dto.search.PackageSearchResponse;
import com.thriftybackpacker.service.PackageSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Package search endpoint — this endpoint does NOT exist in the Python backend.
 * It is added here to fulfil the Phase 2 / Phase 3 project specification.
 *
 * Always uses real RapidAPI data. No mock data exists anywhere in this class.
 */
@Tag(name = "Package Search", description = "Combined flight + hotel package search using real RapidAPI data")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SearchController {

    private final PackageSearchService packageSearchService;

    @Operation(
            summary = "Search travel packages",
            description = """
                    Searches for combined flight + hotel packages within the specified budget.

                    Algorithm:
                    1. Calls booking-com.p.rapidapi.com for real flight data
                    2. Calls booking-com.p.rapidapi.com for real hotel data
                    3. Normalizes prices to BigDecimal (USD)
                    4. Sorts flights and hotels by price ascending
                    5. Uses binary search to find hotel+flight pairs where totalCost <= budget
                    6. Returns up to 10 packages sorted by totalCost ascending

                    If no packages fit the budget, returns an empty list with a clear message.
                    Requires RAPIDAPI_KEY to be set in environment variables.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Search completed (packages may be empty)"),
                    @ApiResponse(responseCode = "422", description = "Invalid request body"),
                    @ApiResponse(responseCode = "500", description = "RapidAPI error or server failure")
            }
    )
    @PostMapping("/search/packages")
    public PackageSearchResponse searchPackages(@Valid @RequestBody PackageSearchRequest request) {
        return packageSearchService.search(request);
    }
}
