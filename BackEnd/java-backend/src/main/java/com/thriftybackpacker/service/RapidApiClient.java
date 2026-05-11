package com.thriftybackpacker.service;

import com.thriftybackpacker.config.RapidApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP client for booking-com.p.rapidapi.com — mirrors the Python RapidApiClient exactly.
 *
 * Caching: successful responses are cached by full URL for rapidapi.cache-ttl-ms (default 10 min).
 * 429 rate-limit state is cached for rapidapi.rate-limit-ttl-ms (default 2 min) so the app
 * does not keep hammering an already-limited endpoint during testing.
 *
 * 429 handling: fail fast — no retries. Rate-limit errors go straight to the fallback
 * mechanism in each controller instead of burning remaining quota on retry attempts.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RapidApiClient {

    private final RestTemplate restTemplate;
    private final RapidApiProperties props;

    @Value("${rapidapi.cache-ttl-ms:600000}")
    private long cacheTtlMs;

    @Value("${rapidapi.rate-limit-ttl-ms:120000}")
    private long rateLimitTtlMs;

    // Cache: URL → {responseBody or RATE_LIMITED_SENTINEL, expiryEpochMs}
    private static final Object RATE_LIMITED_SENTINEL = new Object();
    private final ConcurrentHashMap<String, Object[]> responseCache = new ConcurrentHashMap<>();

    // ── Flights ───────────────────────────────────────────────────────────────

    /**
     * GET v1/flights/search
     * Python: RapidApiClient.search_flights()
     *
     * from_code / to_code format: "JFK.AIRPORT", "LHR.AIRPORT"
     * order_by values: "BEST", "CHEAPEST", "FASTEST", "QUICKEST"
     */
    public Map<String, Object> searchFlights(
            String fromCode, String toCode, String departDate, int adults,
            String currency, String orderBy, String flightType, String cabinClass,
            String locale, int pageNumber, String returnDate, String childrenAges) {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("from_code", fromCode);
        params.add("to_code", toCode);
        params.add("depart_date", departDate);
        params.add("adults", String.valueOf(adults));
        params.add("currency", currency);
        params.add("order_by", orderBy);
        params.add("flight_type", flightType);
        params.add("cabin_class", cabinClass);
        params.add("locale", locale);
        params.add("page_number", String.valueOf(pageNumber));
        if (returnDate != null && !returnDate.isBlank()) params.add("return_date", returnDate);
        if (childrenAges != null && !childrenAges.isBlank()) params.add("children_ages", childrenAges);

        return get("v1/flights/search", params);
    }

    // ── Hotels ────────────────────────────────────────────────────────────────

    /**
     * GET v1/hotels/search
     * Python: RapidApiClient.search_hotels()
     *
     * dest_id: Booking.com numeric city ID (e.g. -2601889 for London)
     * dest_type: "city"
     */
    public Map<String, Object> searchHotels(
            String destId, String destType, String checkinDate, String checkoutDate,
            int adultsNumber, int roomNumber, int pageNumber, String units,
            String locale, String filterByCurrency, String orderBy,
            boolean includeAdjacency, int childrenNumber, String childrenAges,
            String categoriesFilterIds) {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("dest_id", destId);
        params.add("dest_type", destType);
        params.add("checkin_date", checkinDate);
        params.add("checkout_date", checkoutDate);
        params.add("adults_number", String.valueOf(adultsNumber));
        params.add("room_number", String.valueOf(roomNumber));
        params.add("page_number", String.valueOf(pageNumber));
        params.add("units", units);
        params.add("locale", locale);
        params.add("filter_by_currency", filterByCurrency);
        params.add("order_by", orderBy);
        params.add("include_adjacency", String.valueOf(includeAdjacency).toLowerCase());
        if (childrenNumber > 0) {
            params.add("children_number", String.valueOf(childrenNumber));
            if (childrenAges != null) params.add("children_ages", childrenAges);
        }
        if (categoriesFilterIds != null) params.add("categories_filter_ids", categoriesFilterIds);

        return get("v1/hotels/search", params);
    }

    // ── Attractions ───────────────────────────────────────────────────────────

    /**
     * GET v1/attractions/search
     * Python: RapidApiClient.search_attractions()
     */
    public Map<String, Object> searchAttractions(
            String startDate, String endDate, String destId,
            String locale, int pageNumber, String currency, String orderBy) {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        // start_date and end_date are required by the API (422 if absent).
        // The controller guarantees they are always non-null before calling here.
        params.add("start_date", startDate);
        params.add("end_date", endDate);
        params.add("dest_id", destId);
        params.add("locale", locale);
        params.add("page_number", String.valueOf(pageNumber));
        params.add("currency", currency);
        params.add("order_by", orderBy);

        return get("v1/attractions/search", params);
    }

    // ── Internal HTTP helpers ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String path, MultiValueMap<String, String> params) {
        validateKey();

        String url = UriComponentsBuilder
                .fromHttpUrl("https://" + props.getBaseHost() + "/" + path)
                .queryParams(params)
                .build()
                .toUriString();

        // ── Cache lookup ───────────────────────────────────────────────────────
        Object[] cached = responseCache.get(url);
        if (cached != null && System.currentTimeMillis() < (long) cached[1]) {
            if (cached[0] == RATE_LIMITED_SENTINEL) {
                log.info("Cache: rate-limit sentinel active for {} — returning 429 without calling API", path);
                throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS,
                        "Rate limit cached — try again in a moment.");
            }
            log.debug("Cache hit for {}", path);
            return (Map<String, Object>) cached[0];
        }

        // ── HTTP call ──────────────────────────────────────────────────────────
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-key", props.getKey());
        headers.set("x-rapidapi-host", props.getBaseHost());
        headers.setContentType(MediaType.APPLICATION_JSON);

        log.debug("RapidAPI GET {}", url);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {});
            Map<String, Object> body = response.getBody();
            // Cache successful response
            responseCache.put(url, new Object[]{body, System.currentTimeMillis() + cacheTtlMs});
            return body;
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode().value() == 429) {
                // Cache the rate-limit state — next identical request within rateLimitTtlMs
                // will return immediately without hitting the API again.
                responseCache.put(url, new Object[]{RATE_LIMITED_SENTINEL,
                        System.currentTimeMillis() + rateLimitTtlMs});
                log.error("RapidAPI 429 TOO_MANY_REQUESTS at {}: {}", path, ex.getResponseBodyAsString());
            } else {
                log.error("RapidAPI {} at {}: {}", ex.getStatusCode(), path, ex.getResponseBodyAsString());
            }
            // Re-throw with original status so GlobalExceptionHandler forwards it correctly.
            // No retry on 4xx — rate limits and auth errors don't improve with retrying.
            throw ex;
        } catch (HttpServerErrorException ex) {
            log.error("RapidAPI server error {} at {}: {}", ex.getStatusCode(), path, ex.getResponseBodyAsString());
            throw new IllegalStateException("RapidAPI server error. Try again in a moment.");
        } catch (ResourceAccessException ex) {
            log.error("RapidAPI connection timeout at {}: {}", path, ex.getMessage());
            throw new IllegalStateException("RapidAPI connection timed out. Try again in a moment.");
        }
    }

    private void validateKey() {
        if (props.getKey() == null || props.getKey().isBlank()) {
            throw new IllegalStateException(
                    "RAPIDAPI_KEY is not configured. Set it in your environment before starting.");
        }
    }
}
