package com.thriftybackpacker.controller;

import com.thriftybackpacker.service.RapidApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Passes real RapidAPI flight search results to the caller, capped at max-flights.
 * Mirrors Python rapidapi.py: GET /api/v1/flights/search
 *
 * The cap (travel.api.max-flights) reduces the payload size sent to the frontend
 * and keeps the response fast. The frontend sorts by price anyway so returning
 * 20 well-sorted results is equivalent to returning 100.
 */
@Slf4j
@Tag(name = "Flight Search", description = "Real-time flight search via RapidAPI (booking-com.p.rapidapi.com)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FlightController {

    private final RapidApiClient rapidApiClient;

    @Value("${travel.api.max-flights:20}")
    private int maxFlights;

    @Value("${travel.api.max-flight-price:0}")
    private double maxFlightPrice;

    @Operation(
            summary = "Search flights",
            description = "Calls booking-com.p.rapidapi.com/v1/flights/search and returns up to " +
                    "max-flights results (default 20). from_code format: 'LHR.AIRPORT'. Requires RAPIDAPI_KEY."
    )
    @GetMapping("/flights/search")
    public Map<String, Object> searchFlights(
            @RequestParam String from_code,
            @RequestParam String to_code,
            @RequestParam String depart_date,
            @RequestParam(defaultValue = "1")        int adults,
            @RequestParam(defaultValue = "AED")      String currency,
            @RequestParam(defaultValue = "BEST")     String order_by,
            @RequestParam(defaultValue = "ONEWAY")   String flight_type,
            @RequestParam(defaultValue = "ECONOMY")  String cabin_class,
            @RequestParam(defaultValue = "en-gb")    String locale,
            @RequestParam(defaultValue = "0")        int page_number,
            @RequestParam(required = false)          String return_date,
            @RequestParam(required = false)          String children_ages) {

        Map<String, Object> raw = rapidApiClient.searchFlights(
                from_code, to_code, depart_date, adults,
                currency, order_by, flight_type, cabin_class,
                locale, page_number, return_date, children_ages);

        return capFlightOffers(raw);
    }

    /**
     * Filters flight offers by price cap (travel.api.max-flight-price) then
     * truncates to maxFlights. Looks for the same keys the frontend checks:
     * flightOffers → results → result → flights → data
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> capFlightOffers(Map<String, Object> raw) {
        if (raw == null) return raw;
        for (String key : new String[]{"flightOffers", "results", "result", "flights", "data"}) {
            Object val = raw.get(key);
            if (!(val instanceof List<?> list)) continue;

            List<Object> offers = (List<Object>) list;

            if (maxFlightPrice > 0) {
                offers = offers.stream()
                        .filter(o -> o instanceof Map<?, ?> m && extractFlightPrice((Map<String, Object>) m) <= maxFlightPrice)
                        .collect(Collectors.toList());
                log.debug("Price filter (<={}): {} → {} offers", maxFlightPrice, list.size(), offers.size());
            }

            if (offers.size() > maxFlights) {
                offers = offers.subList(0, maxFlights);
            }

            if (offers.size() != list.size()) {
                Map<String, Object> capped = new LinkedHashMap<>(raw);
                capped.put(key, offers);
                return capped;
            }
            return raw;
        }
        return raw;
    }

    /**
     * Mirrors the price extraction chain in the frontend's flightService.js:
     *   priceBreakdown.total.units
     *   ?? unifiedPriceBreakdown.price.units
     *   ?? unifiedPriceBreakdown.price
     * Returns 0 when no price can be found (offer is kept, not filtered out).
     */
    @SuppressWarnings("unchecked")
    private double extractFlightPrice(Map<String, Object> offer) {
        try {
            Object pb = offer.get("priceBreakdown");
            if (pb instanceof Map<?, ?> pbMap) {
                Object total = ((Map<String, Object>) pbMap).get("total");
                if (total instanceof Map<?, ?> totalMap) {
                    Object units = ((Map<String, Object>) totalMap).get("units");
                    if (units instanceof Number n) return n.doubleValue();
                }
            }
            Object upb = offer.get("unifiedPriceBreakdown");
            if (upb instanceof Map<?, ?> upbMap) {
                Object price = ((Map<String, Object>) upbMap).get("price");
                if (price instanceof Map<?, ?> priceMap) {
                    Object units = ((Map<String, Object>) priceMap).get("units");
                    if (units instanceof Number n) return n.doubleValue();
                }
                if (price instanceof Number n) return n.doubleValue();
            }
        } catch (Exception e) {
            log.warn("Could not extract flight price: {}", e.getMessage());
        }
        return 0;
    }
}
