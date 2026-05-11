package com.thriftybackpacker.service;

import com.thriftybackpacker.dto.activity.ActivityDto;
import com.thriftybackpacker.dto.activity.ActivityListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Activity recommendations — tries the real RapidAPI attractions endpoint first,
 * falls back to curated static data if the API is unavailable or returns nothing.
 *
 * Real API: booking-com.p.rapidapi.com/v1/attractions/search
 * Fallback: static London / city-specific lists matching Phase 2 spec.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final RapidApiClient rapidApiClient;

    // ── Static fallback data ──────────────────────────────────────────────────

    private static final List<ActivityDto> LONDON_ACTIVITIES = List.of(
            new ActivityDto("Free walking tour of Central London", BigDecimal.ZERO, "Tour"),
            new ActivityDto("British Museum (free admission)", BigDecimal.ZERO, "Museum"),
            new ActivityDto("Hyde Park", BigDecimal.ZERO, "Outdoor"),
            new ActivityDto("National Gallery (free admission)", BigDecimal.ZERO, "Museum"),
            new ActivityDto("Borough Market", BigDecimal.ZERO, "Food")
    );

    private static final List<ActivityDto> PARIS_ACTIVITIES = List.of(
            new ActivityDto("Eiffel Tower", new BigDecimal("20.00"), "Landmark"),
            new ActivityDto("Louvre Museum", new BigDecimal("15.00"), "Museum"),
            new ActivityDto("Notre-Dame Cathedral area", BigDecimal.ZERO, "Tour"),
            new ActivityDto("Montmartre walking tour", BigDecimal.ZERO, "Tour"),
            new ActivityDto("Seine River walk", BigDecimal.ZERO, "Outdoor")
    );

    private static final List<ActivityDto> NEW_YORK_ACTIVITIES = List.of(
            new ActivityDto("Central Park", BigDecimal.ZERO, "Outdoor"),
            new ActivityDto("Brooklyn Bridge walk", BigDecimal.ZERO, "Tour"),
            new ActivityDto("Metropolitan Museum of Art", new BigDecimal("25.00"), "Museum"),
            new ActivityDto("High Line Park", BigDecimal.ZERO, "Outdoor"),
            new ActivityDto("Times Square", BigDecimal.ZERO, "Tour")
    );

    private static final List<ActivityDto> DEFAULT_ACTIVITIES = List.of(
            new ActivityDto("City walking tour", BigDecimal.ZERO, "Tour"),
            new ActivityDto("Local museum visit", BigDecimal.ZERO, "Museum"),
            new ActivityDto("City park or garden", BigDecimal.ZERO, "Outdoor"),
            new ActivityDto("Historic district tour", BigDecimal.ZERO, "Tour"),
            new ActivityDto("Local food market", new BigDecimal("5.00"), "Food")
    );

    private static final Map<String, List<ActivityDto>> STATIC_BY_IATA = Map.ofEntries(
            Map.entry("LHR", LONDON_ACTIVITIES),
            Map.entry("LGW", LONDON_ACTIVITIES),
            Map.entry("STN", LONDON_ACTIVITIES),
            Map.entry("CDG", PARIS_ACTIVITIES),
            Map.entry("ORY", PARIS_ACTIVITIES),
            Map.entry("JFK", NEW_YORK_ACTIVITIES),
            Map.entry("LGA", NEW_YORK_ACTIVITIES),
            Map.entry("EWR", NEW_YORK_ACTIVITIES)
    );

    // City names sent by activityService.js (e.g. "Paris" from CDG via DESTINATION_ALIASES)
    private static final Map<String, List<ActivityDto>> STATIC_BY_CITY = Map.ofEntries(
            Map.entry("LONDON",   LONDON_ACTIVITIES),
            Map.entry("PARIS",    PARIS_ACTIVITIES),
            Map.entry("NEW YORK", NEW_YORK_ACTIVITIES)
    );

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Used by GET /api/v1/activities?destination=LHR and by AttractionController.
     * Accepts IATA codes (LHR, CDG, JFK) or city names (London, Paris, New York).
     * Always returns curated free/low-cost static data.
     */
    public ActivityListResponse getActivities(String destination) {
        String code = destination != null ? destination.toUpperCase().trim() : "";
        List<ActivityDto> activities = STATIC_BY_IATA.getOrDefault(code,
                STATIC_BY_CITY.getOrDefault(code, DEFAULT_ACTIVITIES));
        return new ActivityListResponse(destination, activities);
    }

    /**
     * Used by PackageSearchService to embed activities in each package.
     * Calls the real RapidAPI attractions endpoint; falls back to static data on any failure.
     *
     * @param destId    Booking.com numeric city ID (same dest_id used for hotel search)
     * @param startDate Trip start date (YYYY-MM-DD)
     * @param endDate   Trip end date (YYYY-MM-DD)
     * @param iataCode  IATA code used only for the fallback lookup
     */
    public List<String> fetchRealActivityNames(String hotelDestId, String startDate, String endDate, String iataCode) {
        // Use the attractions-specific dest_id — different from the hotel dest_id
        String attractionsDestId = ATTRACTIONS_DEST_ID.get(iataCode.toUpperCase().trim());
        if (attractionsDestId != null) {
            try {
                List<ActivityDto> real = callAttractionsApi(attractionsDestId, startDate, endDate);
                if (!real.isEmpty()) {
                    log.info("Loaded {} real activities for {} from RapidAPI", real.size(), iataCode);
                    return real.stream().map(ActivityDto::getName).toList();
                }
            } catch (Exception ex) {
                log.warn("Attractions API failed for {} (dest_id={}): {} — using static fallback",
                        iataCode, attractionsDestId, ex.getMessage());
            }
        } else {
            log.debug("No attractions dest_id mapping for {} — using static fallback", iataCode);
        }
        return getActivities(iataCode).getActivities().stream().map(ActivityDto::getName).toList();
    }

    /** Still used by PackageSearchService when destId is not available. */
    public List<String> getActivityNames(String destinationIata) {
        return getActivities(destinationIata).getActivities().stream()
                .map(ActivityDto::getName).toList();
    }

    // ── Attractions dest_id mapping (different from hotel dest_id) ───────────
    // booking-com.p.rapidapi.com/v1/attractions/search uses a separate numeric ID
    // system from the hotel search. These are the Booking.com attraction location IDs.
    private static final Map<String, String> ATTRACTIONS_DEST_ID = Map.ofEntries(
            Map.entry("LHR", "20088325"),  // London
            Map.entry("LGW", "20088325"),
            Map.entry("STN", "20088325"),
            Map.entry("CDG", "20015493"),  // Paris
            Map.entry("ORY", "20015493"),
            Map.entry("JFK", "20065512"),  // New York
            Map.entry("LGA", "20065512"),
            Map.entry("EWR", "20065512"),
            Map.entry("LAX", "20044399"),  // Los Angeles
            Map.entry("SFO", "20044537"),  // San Francisco — was missing, caused static fallback for LAX→SFO
            Map.entry("HNL", "20041249"),  // Honolulu
            Map.entry("ORD", "20044382"),  // Chicago O'Hare
            Map.entry("MDW", "20044382"),  // Chicago Midway
            Map.entry("MIA", "20044398"),  // Miami
            Map.entry("SEA", "20044401"),  // Seattle
            Map.entry("BOS", "20044379"),  // Boston
            Map.entry("YYZ", "20065225"),  // Toronto
            Map.entry("YVR", "20065224"),  // Vancouver
            Map.entry("NRT", "20024994"),  // Tokyo
            Map.entry("HND", "20024994"),
            Map.entry("DXB", "20014390"),  // Dubai
            Map.entry("SIN", "20019531"),  // Singapore
            Map.entry("SYD", "20008785"),  // Sydney
            Map.entry("ICN", "20022439"),  // Seoul
            Map.entry("BKK", "20015406"),  // Bangkok
            Map.entry("FRA", "20014924"),  // Frankfurt
            Map.entry("AMS", "20014458"),  // Amsterdam
            Map.entry("MAD", "20023938"),  // Madrid
            Map.entry("FCO", "20024413")   // Rome
    );

    // ── Real API call + response parsing ─────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<ActivityDto> callAttractionsApi(String hotelDestId, String startDate, String endDate) {
        // Find the attractions-specific dest_id from the hotel dest_id via reverse lookup,
        // or use the IATA-keyed attractions map directly from the caller.
        // (hotelDestId here is the IATA code passed from fetchRealActivityNames)
        Map<String, Object> raw = rapidApiClient.searchAttractions(
                startDate, endDate, hotelDestId, "en-gb", 0, "USD", "attr_book_score");

        if (raw == null) return Collections.emptyList();

        log.debug("Attractions response top-level keys: {}", raw.keySet());

        // booking-com.p.rapidapi.com/v1/attractions/search returns { "products": [...], ... }
        // (not "result" — confirmed from live response keys)
        List<Object> items = Collections.emptyList();
        for (String key : new String[]{"products", "result", "results", "data"}) {
            Object val = raw.get(key);
            if (val instanceof List) {
                items = (List<Object>) val;
                break;
            }
        }

        if (items.isEmpty()) {
            log.warn("Attractions API returned empty result for dest_id={}. Response keys: {}",
                    hotelDestId, raw.keySet());
            return Collections.emptyList();
        }

        log.debug("First attraction keys: {}", ((Map<?, ?>) items.get(0)).keySet());

        List<ActivityDto> result = new ArrayList<>();
        for (Object item : items) {
            if (!(item instanceof Map)) continue;
            Map<String, Object> attraction = (Map<String, Object>) item;

            String name = extractName(attraction);
            if (name == null || name.isBlank()) continue;

            BigDecimal price = extractPrice(attraction);
            String category = extractCategory(attraction);

            result.add(new ActivityDto(name, price, category));

            if (result.size() >= 5) break;   // embed at most 5 per package
        }

        return result;
    }

    private String extractName(Map<String, Object> attraction) {
        Object name = attraction.get("name");
        if (name != null) return name.toString().trim();
        Object title = attraction.get("title");
        if (title != null) return title.toString().trim();
        return null;
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractPrice(Map<String, Object> attraction) {
        try {
            // Path 1: representativePrice.chargeAmount
            Object rp = attraction.get("representativePrice");
            if (rp instanceof Map) {
                Object charge = ((Map<String, Object>) rp).get("chargeAmount");
                if (charge != null) {
                    BigDecimal v = new BigDecimal(charge.toString());
                    return v.setScale(2, RoundingMode.HALF_UP);
                }
            }
            // Path 2: price.amount
            Object p = attraction.get("price");
            if (p instanceof Map) {
                Object amount = ((Map<String, Object>) p).get("amount");
                if (amount != null) return new BigDecimal(amount.toString()).setScale(2, RoundingMode.HALF_UP);
            }
            // Path 3: direct numeric price field
            Object direct = attraction.get("amount");
            if (direct != null) return new BigDecimal(direct.toString()).setScale(2, RoundingMode.HALF_UP);
        } catch (Exception ignored) { }
        return BigDecimal.ZERO;
    }

    private String extractCategory(Map<String, Object> attraction) {
        Object cat = attraction.get("primaryLabel");
        if (cat != null) return cat.toString();
        Object type = attraction.get("attractionType");
        if (type != null) return type.toString();
        Object slug = attraction.get("slug");
        if (slug != null) return slug.toString().replace("-", " ");
        return "Attraction";
    }
}
