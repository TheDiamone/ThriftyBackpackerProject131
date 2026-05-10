package com.thriftybackpacker.service;

import com.thriftybackpacker.config.RapidApiProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * Low-level HTTP client for booking-com.p.rapidapi.com — mirrors the Python RapidApiClient.
 *
 * Python equivalent:
 *   class RapidApiClient:
 *       def _get(self, host, path, params):
 *           request = Request(url, headers={x-rapidapi-host, x-rapidapi-key, Content-Type}, method="GET")
 *           with urlopen(request) as response: return json.loads(response.read())
 *
 * All calls return raw Map<String, Object> — the exact JSON the RapidAPI sends back.
 * Controllers that expose individual search endpoints pass this through directly to the client.
 * PackageSearchService extracts prices from the nested structure before building packages.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RapidApiClient {

    private final RestTemplate restTemplate;
    private final RapidApiProperties props;

    // ── Individual search methods ─────────────────────────────────────────────

    /**
     * GET /v1/flights/search — mirrors Python RapidApiClient.search_flights().
     * from_code format: "LHR.AIRPORT", to_code format: "JFK.AIRPORT"
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
        if (returnDate != null) params.add("return_date", returnDate);
        if (childrenAges != null) params.add("children_ages", childrenAges);

        return get("v1/flights/search", params);
    }

    /**
     * GET /v1/hotels/search — mirrors Python RapidApiClient.search_hotels().
     * dest_id is the Booking.com numeric location ID, e.g. "-2601889" for London.
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

    /**
     * GET /v1/attractions/search — mirrors Python RapidApiClient.search_attractions().
     */
    public Map<String, Object> searchAttractions(
            String startDate, String endDate, String destId,
            String locale, int pageNumber, String currency, String orderBy) {

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("start_date", startDate);
        params.add("end_date", endDate);
        params.add("dest_id", destId);
        params.add("locale", locale);
        params.add("page_number", String.valueOf(pageNumber));
        params.add("currency", currency);
        params.add("order_by", orderBy);

        return get("v1/attractions/search", params);
    }

    // ── Internal HTTP helper ──────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> get(String path, MultiValueMap<String, String> params) {
        validateKey();

        String url = UriComponentsBuilder
                .fromHttpUrl("https://" + props.getBaseHost() + "/" + path)
                .queryParams(params)
                .build()
                .toUriString();

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-rapidapi-key", props.getKey());
        headers.set("x-rapidapi-host", props.getBaseHost());
        headers.setContentType(MediaType.APPLICATION_JSON);

        log.debug("RapidAPI GET {}", url);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            return response.getBody();
        } catch (HttpClientErrorException ex) {
            log.error("RapidAPI client error {} at {}: {}", ex.getStatusCode(), path, ex.getResponseBodyAsString());
            throw new IllegalStateException("RapidAPI error " + ex.getStatusCode().value() + ": " + ex.getResponseBodyAsString());
        } catch (HttpServerErrorException ex) {
            log.error("RapidAPI server error {} at {}: {}", ex.getStatusCode(), path, ex.getResponseBodyAsString());
            throw new IllegalStateException("RapidAPI server error: " + ex.getResponseBodyAsString());
        } catch (ResourceAccessException ex) {
            log.error("RapidAPI connection timeout at {}: {}", path, ex.getMessage());
            throw new IllegalStateException("RapidAPI connection timed out. Try again in a moment.");
        }
    }

    private void validateKey() {
        if (props.getKey() == null || props.getKey().isBlank()) {
            throw new IllegalStateException(
                    "RAPIDAPI_KEY is not configured. Set it in your .env file or environment variables.");
        }
    }
}
