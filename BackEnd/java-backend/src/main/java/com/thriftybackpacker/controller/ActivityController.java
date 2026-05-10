package com.thriftybackpacker.controller;

import com.thriftybackpacker.dto.activity.ActivityListResponse;
import com.thriftybackpacker.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Activities endpoint — matches Phase 2 spec: GET /api/v1/activities?destination=LHR
 *
 * The Python backend's travel.py has an Amadeus-based activities endpoint but it is
 * NOT registered in the Python router. This Java endpoint provides the static activity
 * recommendations that the Phase 2 spec requires.
 */
@Tag(name = "Activities", description = "Free and low-cost activity recommendations by destination")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityService activityService;

    @Operation(
            summary = "Get activity recommendations",
            description = "Returns free or low-cost activity recommendations for a destination IATA code. " +
                    "London airports (LHR/LGW/STN) return London-specific activities. " +
                    "All other destinations return a generic set of budget-friendly recommendations."
    )
    @GetMapping("/activities")
    public ActivityListResponse getActivities(
            @Parameter(description = "IATA airport code, e.g. LHR", example = "LHR")
            @RequestParam String destination) {
        return activityService.getActivities(destination);
    }
}
