package com.thriftybackpacker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Maps city names (sent by the Vue frontend) to Booking.com internal location IDs.
 *
 * The frontend's hotelService.js and activityService.js never send dest_id —
 * they send dest_name (e.g., "London") and country_name (e.g., "United Kingdom")
 * derived from the DESTINATION_ALIASES map in hotelService.js.
 *
 * Hotels and Attractions use DIFFERENT dest_id systems on Booking.com:
 *   - Hotel dest_ids:      negative integers  (e.g., -2601889 for London)
 *   - Attraction dest_ids: positive integers  (e.g., 20088325 for London)
 */
@Slf4j
@Service
public class LocationMapper {

    private static final Map<String, String> HOTEL_DEST_IDS = Map.ofEntries(
            Map.entry("london",        "-2601889"),
            Map.entry("new york",      "-553173"),
            Map.entry("paris",         "-1456928"),
            Map.entry("tokyo",         "-246227"),
            Map.entry("los angeles",   "-1506450"),
            Map.entry("san francisco", "-1307956"),
            Map.entry("honolulu",      "-1338795"),
            Map.entry("chicago",       "-634685"),
            Map.entry("miami",         "-1215325"),
            Map.entry("seattle",       "-1815797"),
            Map.entry("boston",        "-1506547"),
            Map.entry("dubai",         "-782831"),
            Map.entry("singapore",     "-73635"),
            Map.entry("sydney",        "-1603135"),
            Map.entry("seoul",         "-716583"),
            Map.entry("bangkok",       "-3077749"),
            Map.entry("frankfurt",     "-1746443"),
            Map.entry("amsterdam",     "-2140479"),
            Map.entry("toronto",       "-574490"),
            Map.entry("vancouver",     "-575267"),
            Map.entry("madrid",        "-390625"),
            Map.entry("rome",          "-126963")
    );

    // Attraction dest_ids use the same hotel city IDs (negative integers).
    // Confirmed: London -2601889 returned 20 real London attractions.
    // The 20XXXXXX IDs were unreliable (London's returned empty; NYC's returned NYC results
    // which happened to be correct but the format is inconsistent across cities).
    // Using hotel city IDs ensures every city maps to a validated Booking.com location.
    private static final Map<String, String> ATTRACTIONS_DEST_IDS = Map.ofEntries(
            Map.entry("london",        "-2601889"),
            Map.entry("new york",      "-553173"),
            Map.entry("paris",         "-1456928"),
            Map.entry("tokyo",         "-246227"),
            Map.entry("los angeles",   "-1506450"),
            Map.entry("san francisco", "-1307956"),
            Map.entry("honolulu",      "-1338795"),
            Map.entry("chicago",       "-634685"),
            Map.entry("miami",         "-1215325"),
            Map.entry("seattle",       "-1815797"),
            Map.entry("boston",        "-1506547"),
            Map.entry("dubai",         "-782831"),
            Map.entry("singapore",     "-73635"),
            Map.entry("sydney",        "-1603135"),
            Map.entry("seoul",         "-716583"),
            Map.entry("bangkok",       "-3077749"),
            Map.entry("frankfurt",     "-1746443"),
            Map.entry("amsterdam",     "-2140479"),
            Map.entry("toronto",       "-574490"),
            Map.entry("vancouver",     "-575267"),
            Map.entry("madrid",        "-390625"),
            Map.entry("rome",          "-126963")
    );

    /** Resolve Booking.com hotel dest_id from a city name. */
    public Optional<String> hotelDestId(String cityName) {
        if (cityName == null || cityName.isBlank()) return Optional.empty();
        String key = cityName.trim().toLowerCase();
        Optional<String> result = Optional.ofNullable(HOTEL_DEST_IDS.get(key));
        if (result.isEmpty()) log.warn("No hotel dest_id mapping for city name '{}'", cityName);
        return result;
    }

    /** Resolve Booking.com attractions dest_id from a city name. */
    public Optional<String> attractionsDestId(String cityName) {
        if (cityName == null || cityName.isBlank()) return Optional.empty();
        String key = cityName.trim().toLowerCase();
        Optional<String> result = Optional.ofNullable(ATTRACTIONS_DEST_IDS.get(key));
        if (result.isEmpty()) log.warn("No attractions dest_id mapping for city name '{}'", cityName);
        return result;
    }

    /**
     * Returns the best available dest_id for an attractions search.
     * Tries the dedicated attractions ID first; if absent, falls back to the hotel
     * city ID (same city — the Booking.com v1/attractions/search endpoint accepts
     * the hotel city ID format when no attractions-specific ID is known).
     * Returns a record so callers can log which ID source was used.
     */
    public record DestIdResult(String destId, String source) {}

    public Optional<DestIdResult> resolveAttractionDestId(String cityName) {
        if (cityName == null || cityName.isBlank()) return Optional.empty();
        String key = cityName.trim().toLowerCase();

        String attractionsId = ATTRACTIONS_DEST_IDS.get(key);
        if (attractionsId != null) {
            return Optional.of(new DestIdResult(attractionsId, "attractions-map"));
        }

        String hotelId = HOTEL_DEST_IDS.get(key);
        if (hotelId != null) {
            log.info("No attractions dest_id for '{}' — falling back to hotel city ID {}", cityName, hotelId);
            return Optional.of(new DestIdResult(hotelId, "hotel-map-fallback"));
        }

        log.warn("No dest_id found for city '{}' in either map", cityName);
        return Optional.empty();
    }
}
