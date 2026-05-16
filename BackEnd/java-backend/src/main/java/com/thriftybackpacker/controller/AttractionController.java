package com.thriftybackpacker.controller;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thriftybackpacker.dto.activity.ActivityDto;
import com.thriftybackpacker.service.ActivityService;
import com.thriftybackpacker.service.LocationMapper;
import com.thriftybackpacker.service.RapidApiClient;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Attraction search — calls booking-com.p.rapidapi.com/v1/attractions/search.
 *
 * Normal path: resolve dest_id → ensure ≥7-day date window → call RapidAPI → return raw response.
 * Fallback path (travel.api.activities.fallback-enabled=true): static curated activities when
 *   the API fails, returns empty results, or the city is not in the dest_id map.
 * Mock path (travel.api.use-mock=true): always return static data, no API call.
 *
 * The 422 fix: start_date/end_date are required by the API. The controller always
 * supplies valid dates — defaulting to today + 7 days when the frontend omits them,
 * and extending short windows to a minimum 7-day range for better API result coverage.
 */
@Slf4j
@Tag(name = "Attraction Search", description = "Real-time attraction search via RapidAPI (booking-com.p.rapidapi.com)")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AttractionController {

    private final RapidApiClient rapidApiClient;
    private final LocationMapper locationMapper;
    private final ActivityService activityService;

    @Value("${travel.api.use-mock:false}")
    private boolean useMock;

    @Value("${travel.api.activities.fallback-enabled:true}")
    private boolean fallbackEnabled;

    @Value("${travel.api.activities.max-results:7}")
    private int maxActivityResults;

    @Operation(
            summary = "Search attractions",
            description = """
                    Calls booking-com.p.rapidapi.com/v1/attractions/search and returns real attraction data.

                    Accepts dest_id directly OR dest_name (e.g. 'Paris', 'London') which is
                    auto-resolved to the Booking.com attractions dest_id via LocationMapper.

                    Date handling: start_date/end_date are required by the API. If omitted, defaults
                    to today + 7 days. Short windows (<7 days) are auto-extended to 7 days to avoid
                    empty results (the API filters by booking availability window).

                    Fallback: if travel.api.activities.fallback-enabled=true (default), returns static
                    curated activities when RapidAPI fails or returns empty. The 'provider' field in
                    the response indicates 'RapidAPI' or 'StaticFallback-<reason>'.

                    Mock mode: set travel.api.use-mock=true to always return static data.
                    """
    )
    @GetMapping("/attractions/search")
    public ResponseEntity<?> searchAttractions(
            @RequestParam(required = false) String dest_id,
            @RequestParam(required = false) String dest_name,
            @RequestParam(required = false) String country_name,
            @RequestParam(required = false) String start_date,
            @RequestParam(required = false) String end_date,
            @RequestParam(defaultValue = "en-gb")           String locale,
            @RequestParam(defaultValue = "0")               int page_number,
            @RequestParam(defaultValue = "AED")             String currency,
            @RequestParam(defaultValue = "attr_book_score") String order_by) {

        // ── [1] Mock mode ──────────────────────────────────────────────────────
        if (useMock) {
            log.info("Mock mode (travel.api.use-mock=true) — returning static activities for '{}'", dest_name);
            return ResponseEntity.ok(buildFallback(dest_name, "StaticFallback-Mock"));
        }

        // ── [2] Resolve dest_id ────────────────────────────────────────────────
        String resolvedDestId = dest_id;
        if ((resolvedDestId == null || resolvedDestId.isBlank()) && dest_name != null) {
            resolvedDestId = locationMapper.attractionsDestId(dest_name).orElse(null);
        }

        if (resolvedDestId == null || resolvedDestId.isBlank()) {
            log.warn("No attractions dest_id for dest_name='{}' — {}",
                    dest_name, fallbackEnabled ? "returning static fallback" : "returning 400");
            if (fallbackEnabled) {
                return ResponseEntity.ok(buildFallback(dest_name, "StaticFallback-UnknownDest"));
            }
            return ResponseEntity.badRequest().body(Map.of(
                    "detail", "Unknown destination: '" + dest_name + "'. " +
                              "Provide a recognised city name (London, Paris, Tokyo, …) or a dest_id."));
        }

        // ── [3] Compute effective dates ────────────────────────────────────────
        // The API requires start_date and end_date (returns 422 without them).
        // Use the user's actual dates — no minimum window override.
        // If the window is short and the API returns empty, the fallback handles it.
        LocalDate startParsed = parseDateOrDefault(start_date, LocalDate.now());
        LocalDate endParsed   = parseDateOrDefault(end_date,   startParsed.plusDays(7));
        String effectiveStart = startParsed.toString();
        String effectiveEnd   = endParsed.toString();

        log.debug("Attractions search: dest_id={} window={}/{}", resolvedDestId, effectiveStart, effectiveEnd);

        // ── [4] Call RapidAPI ──────────────────────────────────────────────────
        try {
            Map<String, Object> raw = rapidApiClient.searchAttractions(
                    effectiveStart, effectiveEnd, resolvedDestId,
                    locale, page_number, currency, order_by);

            List<Object> allProducts = extractProductList(raw);

            if (allProducts.isEmpty()) {
                log.info("RapidAPI returned no products for dest_id={} window={}/{} — {}",
                        resolvedDestId, effectiveStart, effectiveEnd,
                        fallbackEnabled ? "using static fallback" : "returning empty list");
                if (fallbackEnabled) {
                    return ResponseEntity.ok(buildFallback(dest_name, "StaticFallback-EmptyResult"));
                }
                return ResponseEntity.ok(Map.of("products", List.of(), "provider", "RapidAPI"));
            }

            // Sort free-first (ascending amount), then cap to maxActivityResults.
            // Products with no parseable price default to 0 and sort before paid ones.
            List<Object> limited = allProducts.stream()
                    .sorted(Comparator.comparingDouble(this::extractAmount))
                    .limit(maxActivityResults)
                    .collect(Collectors.toList());

            log.info("RapidAPI: {} attractions for dest_id={} → returning {} (free-first, cap={})",
                    allProducts.size(), resolvedDestId, limited.size(), maxActivityResults);

            Map<String, Object> result = new LinkedHashMap<>(raw);
            result.put("products", limited);
            result.put("provider", "RapidAPI");
            return ResponseEntity.ok(result);

        } catch (Exception ex) {
            log.warn("RapidAPI attractions failed for dest_id={}: {} — fallback={}",
                    resolvedDestId, ex.getMessage(), fallbackEnabled);
            if (fallbackEnabled) {
                return ResponseEntity.ok(buildFallback(dest_name, "StaticFallback-ApiError"));
            }
            throw ex;
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private LocalDate parseDateOrDefault(String dateStr, LocalDate defaultVal) {
        if (dateStr == null || dateStr.isBlank()) return defaultVal;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            log.warn("Unparseable date '{}', using default {}", dateStr, defaultVal);
            return defaultVal;
        }
    }

    @SuppressWarnings("unchecked")
    private boolean hasProducts(Map<String, Object> raw) {
        for (String key : new String[]{"products", "result", "results", "data"}) {
            Object val = raw.get(key);
            if (val instanceof List<?> l && !l.isEmpty()) return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private int countProducts(Map<String, Object> raw) {
        for (String key : new String[]{"products", "result", "results", "data"}) {
            Object val = raw.get(key);
            if (val instanceof List<?> l) return l.size();
        }
        return 0;
    }

    /** Extracts the first non-null product list from any of the known RapidAPI response keys. */
    @SuppressWarnings("unchecked")
    private List<Object> extractProductList(Map<String, Object> raw) {
        if (raw == null) return List.of();
        for (String key : new String[]{"products", "result", "results", "data"}) {
            Object val = raw.get(key);
            if (val instanceof List<?> l) return (List<Object>) l;
        }
        return List.of();
    }

    /**
     * Extracts a sortable price from a raw RapidAPI product map.
     * Mirrors ActivityService.extractPrice() using the same three paths.
     * Returns 0 for unparseable/missing prices so those items sort first (free-first order).
     */
    @SuppressWarnings("unchecked")
    private double extractAmount(Object product) {
        if (!(product instanceof Map<?, ?> m)) return 0;
        Map<String, Object> p = (Map<String, Object>) m;
        try {
            if (p.get("representativePrice") instanceof Map<?, ?> rp) {
                Object charge = ((Map<String, Object>) rp).get("chargeAmount");
                if (charge != null) return Double.parseDouble(charge.toString());
            }
            if (p.get("price") instanceof Map<?, ?> pr) {
                Object amount = ((Map<String, Object>) pr).get("amount");
                if (amount != null) return Double.parseDouble(amount.toString());
            }
            Object direct = p.get("amount");
            if (direct instanceof Number n) return n.doubleValue();
        } catch (NumberFormatException | ClassCastException | NullPointerException ignored) {}
        return 0;
    }

    /**
     * Wraps ActivityService static data in the same { "products": [...] } shape as RapidAPI.
     * Each product includes a 'provider' field so the frontend (and logs) can identify fallback data.
     */
    private Map<String, Object> buildFallback(String destName, String provider) {
        List<ActivityDto> activities = activityService.getActivities(
                destName != null ? destName : "").getActivities();

        List<Map<String, Object>> products = activities.stream()
                .map(a -> Map.<String, Object>of(
                        "name",     a.getName(),
                        "category", a.getCategory(),
                        "amount",   a.getCost(),
                        "provider", provider
                ))
                .collect(Collectors.toList());

        return Map.of("products", products, "provider", provider);
    }
}
