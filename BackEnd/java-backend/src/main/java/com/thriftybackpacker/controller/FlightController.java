package com.thriftybackpacker.controller;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thriftybackpacker.service.RapidApiClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Passes real RapidAPI flight search results to the caller, capped at max-flights.
 * Mirrors Python rapidapi.py: GET /api/v1/flights/search
 *
 * Currency: the frontend sends currency=AED but this controller always requests USD from
 * RapidAPI so that the budget filter compares like-for-like. The frontend does not display
 * a currency label, so the change from AED → USD is reflected in lower numeric values.
 *
 * Budget filter: maxFlightUsd = budget-cap × budget-flight-ratio (default $900 for a
 * $1,500 cap at 60%). Offers with no parseable price are excluded. If all offers have
 * unparseable prices the filter is bypassed and all offers are returned unfiltered.
 */
@Slf4j
@Tag(name = "Flight Search", description = "Real-time flight search via RapidAPI (booking-com.p.rapidapi.com)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FlightController {

    private final RapidApiClient rapidApiClient;

    @Value("${travel.api.max-flights:40}")
    private int maxFlights;

    @Value("${travel.api.budget-cap:1500.00}")
    private BigDecimal budgetCap;

    @Value("${travel.api.budget-flight-ratio:0.60}")
    private double flightRatio;

    @Operation(
            summary = "Search flights",
            description = "Calls booking-com.p.rapidapi.com/v1/flights/search and returns up to " +
                    "max-flights results within the USD budget split. " +
                    "Prices are always requested in USD regardless of the frontend currency parameter. " +
                    "from_code format: 'LHR.AIRPORT'. Requires RAPIDAPI_KEY."
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

        // Calculating per leg budget cap = 1500 × 0.60 / 2 = $450 for a round trip, or the full $900 for a one-way.
        double maxFlightUsd = budgetCap.doubleValue() * flightRatio / 2.0;

        log.info("Flight search — from={} to={} date={} | frontend_currency={} effective_currency=USD | budget_cap={} max_flight_price={}",
                from_code, to_code, depart_date, currency, budgetCap, String.format("%.2f", maxFlightUsd));

        // Always request USD from RapidAPI regardless of the frontend currency param.
        // This ensures budget comparison is valid (budget-cap is in USD).
        Map<String, Object> raw = rapidApiClient.searchFlights(
                from_code, to_code, depart_date, adults,
                "USD", order_by, flight_type, cabin_class,
                locale, page_number, return_date, children_ages);

        return capFlightOffers(raw, maxFlightUsd);
    }

    /**
     * Filters flight offers to those priced at or below maxFlightPrice (USD), then
     * truncates to maxFlights. Price extraction mirrors frontend flightService.js.
     *
     * Unknown-price handling:
     *   - An offer whose price cannot be extracted (returns 0) is logged and excluded.
     *   - If ALL offers have unparseable prices, the price filter is bypassed and all offers
     *     are returned up to maxFlights (can't filter, better than an empty list).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> capFlightOffers(Map<String, Object> raw, double maxFlightPrice) {
        if (raw == null) return null;

        for (String key : new String[]{"flightOffers", "results", "result", "flights", "data"}) {
            Object val = raw.get(key);
            if (!(val instanceof List<?> list)) continue;

            List<Object> allOffers = (List<Object>) list;
            int totalBefore = allOffers.size();
            int unknownCount = 0;
            List<Object> budgetOffers = new ArrayList<>();

            for (Object offer : allOffers) {
                if (!(offer instanceof Map<?, ?> m)) continue;
                double price = extractFlightPrice((Map<String, Object>) m);
                if (price == 0) {
                    unknownCount++;
                    log.warn("Flight budget filter: offer has no parseable price — excluding from results");
                } else if (price <= maxFlightPrice) {
                    budgetOffers.add(offer);
                }
            }

            // Safety bypass: if every single offer had an unparseable price, we cannot
            // apply the budget filter at all. Return all offers up to maxFlights.
            if (budgetOffers.isEmpty() && unknownCount == totalBefore && totalBefore > 0) {
                log.warn("Flight budget filter: ALL {} offers have unparseable prices — bypassing price filter", totalBefore);
                List<Object> countCapped = totalBefore > maxFlights
                        ? allOffers.subList(0, maxFlights)
                        : allOffers;
                if (countCapped.size() != totalBefore) {
                    Map<String, Object> c = new LinkedHashMap<>(raw);
                    c.put(key, countCapped);
                    return c;
                }
                return raw;
            }

            int overBudget = (totalBefore - unknownCount) - budgetOffers.size();
            log.info("Flight budget filter — max={}USD | total={} known={} under_budget={} over_budget={} unknown={}",
                    String.format("%.2f", maxFlightPrice),
                    totalBefore, totalBefore - unknownCount,
                    budgetOffers.size(), overBudget, unknownCount);

            if (budgetOffers.isEmpty()) {
                log.info("Flight budget filter: no flights found at or below {}USD — returning empty list",
                        String.format("%.2f", maxFlightPrice));
            }

            // Apply count cap after price filter
            List<Object> limited = budgetOffers.size() > maxFlights
                    ? budgetOffers.subList(0, maxFlights)
                    : budgetOffers;

            if (limited.size() != totalBefore) {
                Map<String, Object> capped = new LinkedHashMap<>(raw);
                capped.put(key, limited);
                return capped;
            }
            return raw;
        }
        return raw;
    }

    /**
     * Extracts a USD price from a raw RapidAPI flight offer.
     * Mirrors the price extraction chain in the frontend's flightService.js:
     *   priceBreakdown.total.units
     *   ?? unifiedPriceBreakdown.price.units
     *   ?? unifiedPriceBreakdown.price (if numeric, not a nested map)
     * Returns 0 when no price can be found — callers must treat 0 as unknown, not free.
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
            log.warn("Flight price extraction error: {}", e.getMessage());
        }
        return 0;
    }
}
