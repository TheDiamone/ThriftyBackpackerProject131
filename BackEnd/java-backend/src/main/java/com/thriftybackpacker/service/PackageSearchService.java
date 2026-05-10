package com.thriftybackpacker.service;

import com.thriftybackpacker.dto.search.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implements POST /api/v1/search/packages using real RapidAPI data.
 *
 * Algorithm (matching the frontend flightService.js + hotelService.js price extraction):
 *  1. Fetch real flights from booking-com.p.rapidapi.com/v1/flights/search (USD)
 *  2. Fetch real hotels from booking-com.p.rapidapi.com/v1/hotels/search (USD)
 *  3. Normalize both into FlightOption / HotelOption with BigDecimal prices
 *  4. Sort flights ascending by price
 *  5. Sort hotels ascending by price
 *  6. For each flight: binary search on hotel price list to find the most
 *     expensive hotel where flightPrice + hotelPrice <= budget
 *  7. Return up to maxPackages results sorted by totalCost ascending
 *
 * No mock data is used anywhere in this class.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PackageSearchService {

    private final RapidApiClient rapidApi;
    private final ActivityService activityService;

    @Value("${travel.api.max-packages:10}")
    private int maxPackages;

    // ── IATA code → Booking.com numeric hotel dest_id ────────────────────────
    // These are city-level dest_ids from Booking.com used for hotel search.
    private static final Map<String, String> IATA_TO_HOTEL_DEST_ID = Map.ofEntries(
            Map.entry("LHR", "-2601889"), // London
            Map.entry("LGW", "-2601889"), // London Gatwick → London city
            Map.entry("STN", "-2601889"), // London Stansted → London city
            Map.entry("JFK", "-553173"),  // New York
            Map.entry("LGA", "-553173"),  // New York LaGuardia → New York city
            Map.entry("EWR", "-553173"),  // Newark → New York city
            Map.entry("LAX", "-1506450"), // Los Angeles
            Map.entry("SFO", "-1307956"), // San Francisco
            Map.entry("CDG", "-1456928"), // Paris
            Map.entry("ORY", "-1456928"), // Paris Orly → Paris city
            Map.entry("NRT", "-246227"),  // Tokyo
            Map.entry("HND", "-246227"),  // Tokyo Haneda → Tokyo city
            Map.entry("DXB", "-782831"),  // Dubai
            Map.entry("SIN", "-73635"),   // Singapore
            Map.entry("SYD", "-1603135"), // Sydney
            Map.entry("ICN", "-716583"),  // Seoul
            Map.entry("BKK", "-3077749"), // Bangkok
            Map.entry("FRA", "-1746443"), // Frankfurt
            Map.entry("AMS", "-2140479"), // Amsterdam
            Map.entry("MAD", "-390625"),  // Madrid
            Map.entry("FCO", "-126963"),  // Rome
            Map.entry("YYZ", "-574490"),  // Toronto
            Map.entry("ORD", "-634685"),  // Chicago
            Map.entry("MIA", "-1215325"), // Miami
            Map.entry("BOS", "-1506547"), // Boston
            Map.entry("SEA", "-1815797"), // Seattle
            Map.entry("HNL", "-1338795")  // Honolulu
    );

    private static final String DEFAULT_ORIGIN = "JFK";

    // ── Public entry point ────────────────────────────────────────────────────

    public PackageSearchResponse search(PackageSearchRequest req) {
        String dest = req.getDestination().toUpperCase().trim();
        String origin = (req.getOrigin() != null && !req.getOrigin().isBlank())
                ? req.getOrigin().toUpperCase().trim()
                : DEFAULT_ORIGIN;

        log.info("Package search: {} → {} | dates {} – {} | travelers={} | budget={}",
                origin, dest, req.getStartDate(), req.getEndDate(), req.getTravelers(), req.getBudget());

        int nights = calcNights(req.getStartDate(), req.getEndDate());

        // 1. Fetch flights ─────────────────────────────────────────────────────
        List<FlightOption> flights = Collections.emptyList();
        try {
            Map<String, Object> rawFlights = rapidApi.searchFlights(
                    origin + ".AIRPORT",    // from_code format: "JFK.AIRPORT"
                    dest + ".AIRPORT",      // to_code format: "LHR.AIRPORT"
                    req.getStartDate(),
                    req.getTravelers(),
                    "USD",                  // currency — matches frontend expectation
                    "CHEAPEST",
                    "ONEWAY",
                    "ECONOMY",
                    "en-gb",
                    0,
                    null,
                    null
            );
            flights = normalizeFlights(rawFlights, dest);
        } catch (IllegalStateException ex) {
            log.error("Flight search failed for {} → {}: {}", origin, dest, ex.getMessage());
            if (ex.getMessage().contains("RAPIDAPI_KEY")) {
                return errorResponse(req, "RAPIDAPI_KEY is not configured. Cannot fetch real flight data.");
            }
            return errorResponse(req, "Flight search failed: " + ex.getMessage());
        }

        if (flights.isEmpty()) {
            log.info("No usable flight data for {} → {}", origin, dest);
            return PackageSearchResponse.builder()
                    .destination(req.getDestination())
                    .budget(req.getBudget())
                    .packages(Collections.emptyList())
                    .message("No flights found for this route and date. Try different dates or check airport codes.")
                    .build();
        }

        // 2. Fetch hotels ──────────────────────────────────────────────────────
        String hotelDestId = IATA_TO_HOTEL_DEST_ID.get(dest);
        if (hotelDestId == null) {
            log.warn("No hotel dest_id mapping for '{}'. Add it to IATA_TO_HOTEL_DEST_ID.", dest);
            return PackageSearchResponse.builder()
                    .destination(req.getDestination())
                    .budget(req.getBudget())
                    .packages(Collections.emptyList())
                    .message("Destination '" + dest + "' is not yet supported for hotel search. Contact support.")
                    .build();
        }

        List<HotelOption> hotels = Collections.emptyList();
        try {
            Map<String, Object> rawHotels = rapidApi.searchHotels(
                    hotelDestId,
                    "city",
                    req.getStartDate(),
                    req.getEndDate(),
                    req.getTravelers(),
                    1,
                    0,
                    "metric",
                    "en-gb",
                    "USD",
                    "price",
                    true,
                    0,
                    null,
                    null
            );
            hotels = normalizeHotels(rawHotels, dest, nights);
        } catch (IllegalStateException ex) {
            log.error("Hotel search failed for dest_id={}: {}", hotelDestId, ex.getMessage());
            return errorResponse(req, "Hotel search failed: " + ex.getMessage());
        }

        if (hotels.isEmpty()) {
            return PackageSearchResponse.builder()
                    .destination(req.getDestination())
                    .budget(req.getBudget())
                    .packages(Collections.emptyList())
                    .message("No hotels found for this destination and dates.")
                    .build();
        }

        // 3. Build packages with binary search ─────────────────────────────────
        List<TravelPackage> packages = buildPackages(flights, hotels, req.getBudget(), dest);

        String message = packages.isEmpty()
                ? "No packages found within budget."
                : "Packages found";

        return PackageSearchResponse.builder()
                .destination(req.getDestination())
                .budget(req.getBudget())
                .packages(packages)
                .message(message)
                .build();
    }

    // ── Price extraction helpers ──────────────────────────────────────────────

    /**
     * Extracts FlightOption list from raw RapidAPI flight response.
     *
     * Price extraction path (matching frontend flightService.js getTotalPrice()):
     *   offer.priceBreakdown.total.units
     *   ?? offer.unifiedPriceBreakdown.price.units
     *   ?? offer.unifiedPriceBreakdown.price
     */
    @SuppressWarnings("unchecked")
    List<FlightOption> normalizeFlights(Map<String, Object> raw, String dest) {
        if (raw == null) return Collections.emptyList();

        List<Object> offers = extractList(raw, "flightOffers", "results", "result", "flights", "data");
        if (offers.isEmpty()) {
            log.warn("No flight offers found in RapidAPI response for {}", dest);
            return Collections.emptyList();
        }

        List<FlightOption> normalized = new ArrayList<>();
        for (int i = 0; i < offers.size(); i++) {
            if (!(offers.get(i) instanceof Map)) continue;
            Map<String, Object> offer = (Map<String, Object>) offers.get(i);

            BigDecimal price = extractFlightPrice(offer);
            if (price == null) {
                log.debug("Skipping flight offer[{}]: missing or invalid price", i);
                continue;
            }

            String carrier = extractFlightCarrier(offer);
            String[] times = extractFlightTimes(offer);

            normalized.add(FlightOption.builder()
                    .id(stringOrDefault(offer.get("token"), "FL-" + i))
                    .provider(carrier)
                    .price(price)
                    .currency("USD")
                    .departureTime(times[0])
                    .arrivalTime(times[1])
                    .build());
        }

        log.info("Normalized {}/{} flight offers for {}", normalized.size(), offers.size(), dest);
        return normalized;
    }

    /**
     * Extracts HotelOption list from raw RapidAPI hotel response.
     *
     * Price extraction path (matching frontend hotelService.js getHotelTotalPrice()):
     *   hotel.composite_price_breakdown.gross_amount.value
     *   ?? hotel.composite_price_breakdown.all_inclusive_amount.value
     *   ?? hotel.min_total_price
     */
    @SuppressWarnings("unchecked")
    List<HotelOption> normalizeHotels(Map<String, Object> raw, String dest, int nights) {
        if (raw == null) return Collections.emptyList();

        List<Object> items = extractList(raw, "result", "results", "hotels", "data");
        if (items.isEmpty()) {
            log.warn("No hotel results found in RapidAPI response for {}", dest);
            return Collections.emptyList();
        }

        List<HotelOption> normalized = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (!(items.get(i) instanceof Map)) continue;
            Map<String, Object> hotel = (Map<String, Object>) items.get(i);

            BigDecimal price = extractHotelPrice(hotel);
            if (price == null) {
                log.debug("Skipping hotel[{}] '{}': missing or invalid price",
                        i, hotel.getOrDefault("hotel_name", "?"));
                continue;
            }

            String name = firstNonNull(
                    hotel.get("hotel_name_trans"),
                    hotel.get("hotel_name"),
                    "Hotel-" + i
            );

            normalized.add(HotelOption.builder()
                    .id(stringOrDefault(hotel.get("hotel_id"), "HT-" + i))
                    .provider(name)
                    .price(price)
                    .currency("USD")
                    .nights(nights)
                    .build());
        }

        log.info("Normalized {}/{} hotel results for {}", normalized.size(), items.size(), dest);
        return normalized;
    }

    // ── Binary-search package combination ────────────────────────────────────

    /**
     * Sorts both lists by price ascending, then uses binary search to find the
     * most expensive hotel that, when combined with each flight, stays within budget.
     *
     * Binary search:
     *   For each sorted flight:
     *     remaining = budget - flight.price
     *     idx = upperBound(hotelPrices, remaining) - 1
     *     if idx >= 0: hotels[0..idx] are all affordable with this flight
     *     we pick hotels[idx] (most expensive one that fits) as the best pairing
     *
     * upperBound returns the first index where hotelPrices[i] > target,
     * so idx = upperBound(...) - 1 is the last price <= target.
     */
    List<TravelPackage> buildPackages(List<FlightOption> flights, List<HotelOption> hotels,
                                      BigDecimal budget, String dest) {
        // Sort ascending by price
        flights.sort(Comparator.comparing(FlightOption::getPrice));
        hotels.sort(Comparator.comparing(HotelOption::getPrice));

        List<BigDecimal> hotelPrices = hotels.stream()
                .map(HotelOption::getPrice)
                .collect(Collectors.toList());

        List<String> activities = activityService.getActivityNames(dest);
        List<TravelPackage> result = new ArrayList<>();
        int counter = 1;

        for (FlightOption flight : flights) {
            if (flight.getPrice().compareTo(budget) >= 0) {
                // Flights are sorted — all remaining flights also exceed budget alone
                break;
            }

            BigDecimal remaining = budget.subtract(flight.getPrice());

            // upperBound gives the insertion index of remaining+ε, so [idx] is the last hotel <= remaining
            int idx = upperBound(hotelPrices, remaining) - 1;
            if (idx < 0) {
                // No hotel fits under the remaining budget for this flight
                continue;
            }

            HotelOption bestHotel = hotels.get(idx);
            BigDecimal totalCost = flight.getPrice().add(bestHotel.getPrice());

            // Defensive double-check (upperBound guarantees this, but be explicit)
            if (totalCost.compareTo(budget) > 0) continue;

            result.add(TravelPackage.builder()
                    .packageId(String.format("pkg-%03d", counter++))
                    .flightCost(flight.getPrice())
                    .hotelCost(bestHotel.getPrice())
                    .totalCost(totalCost)
                    .provider("RapidAPI")
                    .activities(activities)
                    .build());

            if (result.size() >= maxPackages) break;
        }

        // Sort final list by totalCost ascending
        result.sort(Comparator.comparing(TravelPackage::getTotalCost));
        return result;
    }

    /**
     * Binary search: returns the index of the first element > target.
     * This means [0, result-1] contains all elements <= target.
     */
    private int upperBound(List<BigDecimal> prices, BigDecimal target) {
        int lo = 0, hi = prices.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (prices.get(mid).compareTo(target) <= 0) {
                lo = mid + 1;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    // ── Low-level extraction helpers ──────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private BigDecimal extractFlightPrice(Map<String, Object> offer) {
        // Path 1: priceBreakdown.total.units
        Object val = nestedGet(offer, "priceBreakdown", "total", "units");
        // Path 2: unifiedPriceBreakdown.price.units
        if (val == null) val = nestedGet(offer, "unifiedPriceBreakdown", "price", "units");
        // Path 3: unifiedPriceBreakdown.price (direct value)
        if (val == null) {
            Object p = nestedGet(offer, "unifiedPriceBreakdown", "price");
            if (!(p instanceof Map)) val = p;
        }
        return safeDecimal(val);
    }

    @SuppressWarnings("unchecked")
    private BigDecimal extractHotelPrice(Map<String, Object> hotel) {
        // Path 1: composite_price_breakdown.gross_amount.value
        Object val = nestedGet(hotel, "composite_price_breakdown", "gross_amount", "value");
        // Path 2: composite_price_breakdown.all_inclusive_amount.value
        if (val == null) val = nestedGet(hotel, "composite_price_breakdown", "all_inclusive_amount", "value");
        // Path 3: min_total_price
        if (val == null) val = hotel.get("min_total_price");
        return safeDecimal(val);
    }

    @SuppressWarnings("unchecked")
    private String extractFlightCarrier(Map<String, Object> offer) {
        try {
            List<Map<String, Object>> segments = (List<Map<String, Object>>) offer.get("segments");
            if (segments != null && !segments.isEmpty()) {
                Map<String, Object> seg = segments.get(0);
                List<Map<String, Object>> legs = (List<Map<String, Object>>) seg.get("legs");
                if (legs != null && !legs.isEmpty()) {
                    Map<String, Object> leg = legs.get(0);
                    List<Map<String, Object>> carriers = (List<Map<String, Object>>) leg.get("carriersData");
                    if (carriers != null && !carriers.isEmpty()) {
                        Object name = carriers.get(0).get("name");
                        if (name != null) return name.toString();
                    }
                }
            }
        } catch (ClassCastException ignored) { /* malformed response */ }
        return "Airline";
    }

    @SuppressWarnings("unchecked")
    private String[] extractFlightTimes(Map<String, Object> offer) {
        String dep = null, arr = null;
        try {
            List<Map<String, Object>> segments = (List<Map<String, Object>>) offer.get("segments");
            if (segments != null && !segments.isEmpty()) {
                Map<String, Object> seg = segments.get(0);
                dep = seg.get("departureTime") != null ? seg.get("departureTime").toString() : null;
                arr = seg.get("arrivalTime") != null ? seg.get("arrivalTime").toString() : null;
            }
        } catch (ClassCastException ignored) { }
        return new String[]{dep, arr};
    }

    /**
     * Navigate a chain of map keys (like lodash _.get).
     * Returns null if any level is missing or not a Map.
     */
    @SuppressWarnings("unchecked")
    private Object nestedGet(Map<String, Object> root, String... keys) {
        Object current = root;
        for (String key : keys) {
            if (!(current instanceof Map)) return null;
            current = ((Map<String, Object>) current).get(key);
        }
        return current;
    }

    /** Converts any numeric value to BigDecimal, returns null if zero, negative, or unparseable. */
    private BigDecimal safeDecimal(Object value) {
        if (value == null) return null;
        try {
            BigDecimal d = new BigDecimal(value.toString().replaceAll("[^\\d.-]", ""));
            return d.compareTo(BigDecimal.ZERO) > 0 ? d : null;
        } catch (NumberFormatException | ArithmeticException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> extractList(Map<String, Object> raw, String... keys) {
        for (String key : keys) {
            Object val = raw.get(key);
            if (val instanceof List) return (List<Object>) val;
        }
        return Collections.emptyList();
    }

    private String stringOrDefault(Object value, String fallback) {
        return value != null ? value.toString() : fallback;
    }

    private String firstNonNull(Object... values) {
        for (Object v : values) {
            if (v != null) return v.toString();
        }
        return "Unknown";
    }

    private int calcNights(String startDate, String endDate) {
        try {
            long nights = ChronoUnit.DAYS.between(
                    LocalDate.parse(startDate), LocalDate.parse(endDate));
            return (int) Math.max(1, nights);
        } catch (Exception e) {
            return 1;
        }
    }

    private PackageSearchResponse errorResponse(PackageSearchRequest req, String message) {
        return PackageSearchResponse.builder()
                .destination(req.getDestination())
                .budget(req.getBudget())
                .packages(Collections.emptyList())
                .message(message)
                .build();
    }
}
