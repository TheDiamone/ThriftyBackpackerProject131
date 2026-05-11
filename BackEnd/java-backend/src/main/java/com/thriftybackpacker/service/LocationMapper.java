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

    private static final Map<String, String> ATTRACTIONS_DEST_IDS = Map.ofEntries(
            Map.entry("london",        "20088325"),
            Map.entry("new york",      "20065512"),
            Map.entry("paris",         "20015493"),
            Map.entry("tokyo",         "20024994"),
            Map.entry("los angeles",   "20044399"),
            Map.entry("san francisco", "20044537"),
            Map.entry("honolulu",      "20041249"),
            Map.entry("chicago",       "20044382"),
            Map.entry("miami",         "20044398"),
            Map.entry("seattle",       "20044401"),
            Map.entry("boston",        "20044379"),
            Map.entry("dubai",         "20014390"),
            Map.entry("singapore",     "20019531"),
            Map.entry("sydney",        "20008785"),
            Map.entry("seoul",         "20022439"),
            Map.entry("bangkok",       "20015406"),
            Map.entry("frankfurt",     "20014924"),
            Map.entry("amsterdam",     "20014458"),
            Map.entry("toronto",       "20065225"),
            Map.entry("vancouver",     "20065224"),
            Map.entry("madrid",        "20023938"),
            Map.entry("rome",          "20024413")
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
}
