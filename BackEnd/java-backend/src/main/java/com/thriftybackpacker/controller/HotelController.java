package com.thriftybackpacker.controller;

import com.thriftybackpacker.service.LocationMapper;
import com.thriftybackpacker.service.RapidApiClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hotel search — passes real RapidAPI results to the client after applying a USD budget filter.
 * Mirrors Python rapidapi.py: GET /api/v1/hotels/search
 *
 * Currency: the frontend sends filter_by_currency=AED but this controller always requests USD
 * from RapidAPI so that the budget filter compares like-for-like. The frontend does not display
 * a currency label, so the change from AED → USD is reflected in lower numeric values.
 *
 * Budget filter: maxHotelUsd = budget-cap × budget-hotel-ratio (default $3,600 for a
 * $6,000 cap at 60%). Price is extracted as total-stay cost. Hotels with no parseable price
 * are excluded. If all hotels have unparseable prices the filter is bypassed.
 *
 * dest_id resolution: the Vue frontend sends dest_name + country_name (not dest_id).
 * This controller resolves those to the correct Booking.com dest_id automatically.
 */
@Slf4j
@Tag(name = "Hotel Search", description = "Real-time hotel search via RapidAPI")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HotelController {

    private final RapidApiClient rapidApiClient;
    private final LocationMapper locationMapper;

    @Value("${travel.api.budget-cap:6000.00}")
    private BigDecimal budgetCap;

    @Value("${travel.api.budget-hotel-ratio:0.60}")
    private double hotelRatio;

    @Operation(
            summary = "Search hotels",
            description = "Calls booking-com.p.rapidapi.com/v1/hotels/search. " +
                    "Accepts either dest_id directly (e.g. -2601889 for London) " +
                    "OR dest_name (e.g. 'London') which is auto-resolved to dest_id. " +
                    "Prices are always requested in USD. Results are filtered to hotels " +
                    "whose total stay cost is at or below budget-cap × budget-hotel-ratio."
    )
    @GetMapping("/hotels/search")
    public ResponseEntity<?> searchHotels(
            @RequestParam(required = false) String dest_id,
            @RequestParam(required = false) String dest_name,
            @RequestParam(required = false) String country_name,
            @RequestParam(defaultValue = "city")   String dest_type,
            @RequestParam(required = false)         String checkin_date,
            @RequestParam(required = false)         String checkout_date,
            @RequestParam(defaultValue = "1")       int adults_number,
            @RequestParam(defaultValue = "1")       int room_number,
            @RequestParam(defaultValue = "0")       int page_number,
            @RequestParam(defaultValue = "metric")  String units,
            @RequestParam(defaultValue = "en-gb")   String locale,
            @RequestParam(defaultValue = "AED")     String filter_by_currency,
            @RequestParam(defaultValue = "popularity") String order_by,
            @RequestParam(defaultValue = "true")    boolean include_adjacency,
            @RequestParam(defaultValue = "0")       int children_number,
            @RequestParam(required = false)         String children_ages,
            @RequestParam(required = false)         String categories_filter_ids) {

        // Resolve dest_id: use provided value, or look up from city name
        String resolvedDestId = dest_id;
        if ((resolvedDestId == null || resolvedDestId.isBlank()) && dest_name != null) {
            resolvedDestId = locationMapper.hotelDestId(dest_name).orElse(null);
        }

        if (resolvedDestId == null || resolvedDestId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "detail", "Provide either dest_id or a recognised dest_name. " +
                            "Received dest_name='" + dest_name + "'. " +
                            "Supported cities: London, New York, Paris, Tokyo, Dubai, etc."));
        }

        double maxHotelUsd = budgetCap.doubleValue() * hotelRatio;

        log.info("Hotel search — dest='{}' checkin={} checkout={} | frontend_currency={} effective_currency=USD | budget_cap={} max_hotel_price={}",
                dest_name != null ? dest_name : resolvedDestId,
                checkin_date, checkout_date,
                filter_by_currency, budgetCap, String.format("%.2f", maxHotelUsd));

        // Always request USD from RapidAPI regardless of the frontend filter_by_currency param.
        // This ensures budget comparison is valid (budget-cap is in USD).
        Map<String, Object> raw = rapidApiClient.searchHotels(
                resolvedDestId, dest_type, checkin_date, checkout_date,
                adults_number, room_number, page_number, units,
                locale, "USD", order_by,
                include_adjacency, children_number, children_ages,
                categories_filter_ids);

        Map<String, Object> filtered = capHotelResults(raw, maxHotelUsd);
        return ResponseEntity.ok(filtered);
    }

    /**
     * Filters hotel results to those whose total stay cost is at or below maxHotelPrice (USD).
     * Looks for the same response-key candidates as PackageSearchService.normalizeHotels().
     *
     * Unknown-price handling:
     *   - A hotel whose price cannot be extracted (returns 0) is logged and excluded.
     *   - If ALL hotels have unparseable prices, the filter is bypassed (can't compare,
     *     better to show something than an empty list due to a parsing failure).
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> capHotelResults(Map<String, Object> raw, double maxHotelPrice) {
        if (raw == null) return null;

        for (String key : new String[]{"result", "results", "hotels", "data"}) {
            Object val = raw.get(key);
            if (!(val instanceof List<?> list)) continue;

            List<Object> allHotels = (List<Object>) list;
            int totalBefore = allHotels.size();
            int unknownCount = 0;
            List<Object> budgetHotels = new ArrayList<>();

            for (Object item : allHotels) {
                if (!(item instanceof Map<?, ?> m)) continue;
                Map<String, Object> hotel = (Map<String, Object>) m;
                double price = extractHotelPrice(hotel);
                if (price == 0) {
                    unknownCount++;
                    log.warn("Hotel budget filter: '{}' has no parseable total-stay price — excluding",
                            hotel.getOrDefault("hotel_name", hotel.getOrDefault("hotel_name_trans", "?")));
                } else if (price <= maxHotelPrice) {
                    budgetHotels.add(item);
                }
            }

            // Safety bypass: if every hotel had an unparseable price, we cannot apply the
            // filter. Return all hotels unfiltered rather than an empty list from a parse failure.
            if (budgetHotels.isEmpty() && unknownCount == totalBefore && totalBefore > 0) {
                log.warn("Hotel budget filter: ALL {} hotels have unparseable prices — bypassing price filter", totalBefore);
                return raw;
            }

            int overBudget = (totalBefore - unknownCount) - budgetHotels.size();
            log.info("Hotel budget filter — max={}USD | total={} known={} under_budget={} over_budget={} unknown={}",
                    String.format("%.2f", maxHotelPrice),
                    totalBefore, totalBefore - unknownCount,
                    budgetHotels.size(), overBudget, unknownCount);

            if (budgetHotels.isEmpty()) {
                log.info("Hotel budget filter: no hotels found at or below {}USD — returning empty list", String.format("%.2f", maxHotelPrice));
            }

            if (budgetHotels.size() != totalBefore) {
                Map<String, Object> capped = new LinkedHashMap<>(raw);
                capped.put(key, budgetHotels);
                return capped;
            }
            return raw;
        }
        return raw;
    }

    /**
     * Extracts a USD total-stay price from a raw Booking.com hotel object.
     * Price path mirrors PackageSearchService.extractHotelPrice() and frontend hotelService.js:
     *   composite_price_breakdown.gross_amount.value
     *   ?? composite_price_breakdown.all_inclusive_amount.value
     *   ?? min_total_price
     * Returns 0 when no price can be found — callers must treat 0 as unknown, not free.
     */
    @SuppressWarnings("unchecked")
    private double extractHotelPrice(Map<String, Object> hotel) {
        try {
            Object cpb = hotel.get("composite_price_breakdown");
            if (cpb instanceof Map<?, ?> cpbMap) {
                Object ga = ((Map<String, Object>) cpbMap).get("gross_amount");
                if (ga instanceof Map<?, ?> gaMap) {
                    Object v = ((Map<String, Object>) gaMap).get("value");
                    if (v instanceof Number n && n.doubleValue() > 0) return n.doubleValue();
                }
                Object aia = ((Map<String, Object>) cpbMap).get("all_inclusive_amount");
                if (aia instanceof Map<?, ?> aiaMap) {
                    Object v = ((Map<String, Object>) aiaMap).get("value");
                    if (v instanceof Number n && n.doubleValue() > 0) return n.doubleValue();
                }
            }
            Object mtp = hotel.get("min_total_price");
            if (mtp instanceof Number n && n.doubleValue() > 0) return n.doubleValue();
            if (mtp instanceof String s) {
                double v = Double.parseDouble(s.replaceAll("[^\\d.-]", ""));
                if (v > 0) return v;
            }
        } catch (Exception e) {
            log.warn("Hotel price extraction error: {}", e.getMessage());
        }
        return 0;
    }
}
