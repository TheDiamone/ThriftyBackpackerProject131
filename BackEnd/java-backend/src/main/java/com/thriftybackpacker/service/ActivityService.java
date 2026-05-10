package com.thriftybackpacker.service;

import com.thriftybackpacker.dto.activity.ActivityDto;
import com.thriftybackpacker.dto.activity.ActivityListResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Returns static activity recommendations by destination IATA code.
 *
 * This is NOT mock data — it is the same intentional static content described in the
 * Phase 2 project specification. The Python backend's travel.py (Amadeus-based activities)
 * is not registered in the router, so static recommendations are the correct behaviour
 * for this project at this stage.
 *
 * London airports (LHR/LGW/STN) return London-specific free activities.
 * All other IATA codes receive a generic set of budget-friendly recommendations.
 */
@Service
public class ActivityService {

    private static final List<ActivityDto> LONDON_ACTIVITIES = List.of(
            new ActivityDto("Free walking tour of Central London", BigDecimal.ZERO, "Tour"),
            new ActivityDto("British Museum (free admission)", BigDecimal.ZERO, "Museum"),
            new ActivityDto("Hyde Park", BigDecimal.ZERO, "Outdoor"),
            new ActivityDto("National Gallery (free admission)", BigDecimal.ZERO, "Museum"),
            new ActivityDto("Borough Market", BigDecimal.ZERO, "Food")
    );

    private static final List<ActivityDto> PARIS_ACTIVITIES = List.of(
            new ActivityDto("Eiffel Tower (ticketed)", new BigDecimal("20.00"), "Landmark"),
            new ActivityDto("Louvre Museum", new BigDecimal("15.00"), "Museum"),
            new ActivityDto("Notre-Dame Cathedral area (free)", BigDecimal.ZERO, "Tour"),
            new ActivityDto("Montmartre walking tour (free)", BigDecimal.ZERO, "Tour"),
            new ActivityDto("Seine River walk (free)", BigDecimal.ZERO, "Outdoor")
    );

    private static final List<ActivityDto> NEW_YORK_ACTIVITIES = List.of(
            new ActivityDto("Central Park (free)", BigDecimal.ZERO, "Outdoor"),
            new ActivityDto("Brooklyn Bridge walk (free)", BigDecimal.ZERO, "Tour"),
            new ActivityDto("Metropolitan Museum of Art", new BigDecimal("25.00"), "Museum"),
            new ActivityDto("High Line Park (free)", BigDecimal.ZERO, "Outdoor"),
            new ActivityDto("Times Square (free)", BigDecimal.ZERO, "Tour")
    );

    private static final List<ActivityDto> DEFAULT_ACTIVITIES = List.of(
            new ActivityDto("City walking tour (free)", BigDecimal.ZERO, "Tour"),
            new ActivityDto("Local museum visit", BigDecimal.ZERO, "Museum"),
            new ActivityDto("Central park or city park (free)", BigDecimal.ZERO, "Outdoor"),
            new ActivityDto("Historic district tour (free)", BigDecimal.ZERO, "Tour"),
            new ActivityDto("Local food market", new BigDecimal("5.00"), "Food")
    );

    private static final Map<String, List<ActivityDto>> ACTIVITIES_BY_IATA = Map.ofEntries(
            Map.entry("LHR", LONDON_ACTIVITIES),
            Map.entry("LGW", LONDON_ACTIVITIES),
            Map.entry("STN", LONDON_ACTIVITIES),
            Map.entry("CDG", PARIS_ACTIVITIES),
            Map.entry("ORY", PARIS_ACTIVITIES),
            Map.entry("JFK", NEW_YORK_ACTIVITIES),
            Map.entry("LGA", NEW_YORK_ACTIVITIES),
            Map.entry("EWR", NEW_YORK_ACTIVITIES)
    );

    public ActivityListResponse getActivities(String destinationIata) {
        String code = destinationIata.toUpperCase().trim();
        List<ActivityDto> activities = ACTIVITIES_BY_IATA.getOrDefault(code, DEFAULT_ACTIVITIES);
        return new ActivityListResponse(destinationIata, activities);
    }

    /** Returns only activity name strings — used by PackageSearchService to embed in packages. */
    public List<String> getActivityNames(String destinationIata) {
        return getActivities(destinationIata).getActivities()
                .stream()
                .map(ActivityDto::getName)
                .toList();
    }
}
