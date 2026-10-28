package com.agriplatform.geography.lgd.client;

import com.agriplatform.geography.lgd.config.LgdProperties;
import com.agriplatform.geography.lgd.dto.LgdDistrictDto;
import com.agriplatform.geography.lgd.dto.LgdHierarchyRowDto;
import com.agriplatform.geography.lgd.dto.LgdStateDto;
import com.agriplatform.geography.lgd.dto.LgdSubDistrictDto;
import com.agriplatform.geography.lgd.dto.LgdVillageDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Production HTTP client for the official LGD/NAPIX API.
 *
 * <h2>Security guarantees</h2>
 * <ul>
 *   <li>Auth headers injected from {@link LgdProperties} — never from literals.</li>
 *   <li>Client ID and Secret are NEVER logged at any level.</li>
 *   <li>Error messages include only status codes, never credential values.</li>
 * </ul>
 *
 * <h2>Resilience</h2>
 * <ul>
 *   <li>Bounded retry with exponential backoff for transient errors (5xx, connection, 429 rate limit).</li>
 *   <li>No retry for auth failures (401/403) or bad requests / not found (4xx).</li>
 *   <li>Configurable rate-limiting delay between calls.</li>
 *   <li>Configurable connection and read timeouts.</li>
 * </ul>
 *
 * <h2>LGD API conventions used (per official spec)</h2>
 * <ul>
 *   <li>Base URL: https://delhigw.napix.gov.in/nic/lgd</li>
 *   <li>HTTP Method: POST for all data retrieval endpoints.</li>
 *   <li>Auth: X-IBM-Client-Id and X-IBM-Client-Secret headers.</li>
 *   <li>Rate limit: NOT ESTABLISHED BY THE PROVIDED SPECIFICATION — conservative default applied.</li>
 * </ul>
 */
@Component
public class LgdApiClient implements LgdApiOperations {

    private static final Logger log = LoggerFactory.getLogger(LgdApiClient.class);

    private static final TypeReference<List<LgdStateDto>>        STATE_LIST_TYPE      = new TypeReference<>() {};
    private static final TypeReference<List<LgdDistrictDto>>     DISTRICT_LIST_TYPE   = new TypeReference<>() {};
    private static final TypeReference<List<LgdSubDistrictDto>>  SUBDISTRICT_LIST_TYPE = new TypeReference<>() {};
    private static final TypeReference<List<LgdVillageDto>>      VILLAGE_LIST_TYPE    = new TypeReference<>() {};
    private static final TypeReference<List<LgdHierarchyRowDto>> HIERARCHY_LIST_TYPE  = new TypeReference<>() {};

    private final LgdProperties props;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public LgdApiClient(LgdProperties props, ObjectMapper objectMapper) {
        this.props = props;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()));
        requestFactory.setReadTimeout(Duration.ofMillis(props.getReadTimeoutMs()));

        String configured = props.getBaseUrl();
        String baseUrl = (configured != null && !configured.isBlank())
                ? configured
                : "https://delhigw.napix.gov.in/nic/lgd";

        this.restClient = RestClient.builder()
                .baseUrl(java.util.Objects.requireNonNull(baseUrl))
                .requestFactory(requestFactory)
                .build();
    }

    // =========================================================================
    // ACTIVE SNAPSHOT APIS
    // =========================================================================

    /**
     * Fetches all active states from LGD.
     * POST /states/v1/status/active
     */
    public List<LgdStateDto> fetchActiveStates() {
        log.info("[LGD] Fetching active states");
        String body = postWithRetry("/states/v1/status/active");
        if (body == null || body.isBlank() || body.equals("null")) {
            log.warn("[LGD] Empty response for active states");
            return Collections.emptyList();
        }
        return parseList(body, STATE_LIST_TYPE, "states");
    }

    /**
     * Fetches all active districts for a given state.
     * POST /state/{stateCode}/districts/v1/status/active
     */
    public List<LgdDistrictDto> fetchActiveDistrictsByState(int stateCode) {
        String path = "/state/" + stateCode + "/districts/v1/status/active";
        log.debug("[LGD] Fetching active districts for stateCode={}", stateCode);
        String body = postWithRetry(path);
        if (body == null || body.isBlank() || body.equals("null")) {
            return Collections.emptyList();
        }
        return parseList(body, DISTRICT_LIST_TYPE, "districts[state=" + stateCode + "]");
    }

    /**
     * Fetches all active sub-districts (mandals) for a given state.
     * POST /state/{stateCode}/subdistricts/v1/status/active
     */
    public List<LgdSubDistrictDto> fetchActiveSubDistrictsByState(int stateCode) {
        String path = "/state/" + stateCode + "/subdistricts/v1/status/active";
        log.debug("[LGD] Fetching active sub-districts for stateCode={}", stateCode);
        String body = postWithRetry(path);
        if (body == null || body.isBlank() || body.equals("null")) {
            return Collections.emptyList();
        }
        return parseList(body, SUBDISTRICT_LIST_TYPE, "subdistricts[state=" + stateCode + "]");
    }

    /**
     * Fetches all active villages for a given state.
     * POST /state/{stateCode}/villages/v2/status/active  (v2 includes districtCode)
     */
    public List<LgdVillageDto> fetchActiveVillagesByState(int stateCode) {
        String path = "/state/" + stateCode + "/villages/v2/status/active";
        log.debug("[LGD] Fetching active villages for stateCode={}", stateCode);
        String body = postWithRetry(path);
        if (body == null || body.isBlank() || body.equals("null")) {
            return Collections.emptyList();
        }
        return parseList(body, VILLAGE_LIST_TYPE, "villages[state=" + stateCode + "]");
    }

    // =========================================================================
    // PAGINATED HIERARCHY API (INITIAL FULL LOAD)
    // =========================================================================

    /**
     * Fetches one page of the full hierarchy for a given state.
     * POST /stateDistrictSubdistBlockVillage/{stateCode}/page/{page}
     *
     * <p>Page numbering starts at 1. Returns empty list when no more data.
     * Page-end termination is not explicitly documented by the spec;
     * callers should stop when an empty list is returned.
     */
    public List<LgdHierarchyRowDto> fetchHierarchyPage(int stateCode, int page) {
        String path = "/stateDistrictSubdistBlockVillage/" + stateCode + "/page/" + page;
        log.debug("[LGD] Fetching hierarchy page stateCode={} page={}", stateCode, page);
        String body = postWithRetry(path);
        if (body == null || body.isBlank() || body.equals("null") || body.equals("[]")) {
            return Collections.emptyList();
        }
        return parseList(body, HIERARCHY_LIST_TYPE, "hierarchy[state=" + stateCode + ",page=" + page + "]");
    }

    // =========================================================================
    // INCREMENTAL DURATION APIS
    // =========================================================================

    /**
     * Fetches states updated between two dates.
     * POST /states/v1/updatedFrom/{from}/updatedTo/{to}
     *
     * @param from date string in DD-MM-YYYY format
     * @param to   date string in DD-MM-YYYY format
     */
    public List<LgdStateDto> fetchStatesByDuration(String from, String to) {
        String path = "/states/v1/updatedFrom/" + from + "/updatedTo/" + to;
        log.info("[LGD] Fetching states updated from {} to {}", from, to);
        String body = postWithRetry(path);
        if (body == null || body.isBlank() || body.equals("null")) {
            return Collections.emptyList();
        }
        return parseList(body, STATE_LIST_TYPE, "states-duration[" + from + "→" + to + "]");
    }

    /**
     * Fetches districts for a state updated between two dates.
     * POST /state/{stateCode}/districts/v1/updatedFrom/{from}/updatedTo/{to}
     */
    public List<LgdDistrictDto> fetchDistrictsByStateDuration(int stateCode, String from, String to) {
        String path = "/state/" + stateCode + "/districts/v1/updatedFrom/" + from + "/updatedTo/" + to;
        log.debug("[LGD] Fetching districts by duration stateCode={} from={} to={}", stateCode, from, to);
        String body = postWithRetry(path);
        if (body == null || body.isBlank() || body.equals("null")) {
            return Collections.emptyList();
        }
        return parseList(body, DISTRICT_LIST_TYPE, "districts-duration[state=" + stateCode + "]");
    }

    /**
     * Fetches sub-districts for a state updated between two dates.
     * POST /state/{stateCode}/subdistricts/v2/updatedFrom/{from}/updatedTo/{to}
     */
    public List<LgdSubDistrictDto> fetchSubDistrictsByStateDuration(int stateCode, String from, String to) {
        String path = "/state/" + stateCode + "/subdistricts/v2/updatedFrom/" + from + "/updatedTo/" + to;
        log.debug("[LGD] Fetching sub-districts by duration stateCode={} from={} to={}", stateCode, from, to);
        String body = postWithRetry(path);
        if (body == null || body.isBlank() || body.equals("null")) {
            return Collections.emptyList();
        }
        return parseList(body, SUBDISTRICT_LIST_TYPE, "subdistricts-duration[state=" + stateCode + "]");
    }

    /**
     * Fetches villages for a state updated between two dates.
     * POST /state/{stateCode}/villages/v2/updatedFrom/{from}/updatedTo/{to}
     */
    public List<LgdVillageDto> fetchVillagesByStateDuration(int stateCode, String from, String to) {
        String path = "/state/" + stateCode + "/villages/v2/updatedFrom/" + from + "/updatedTo/" + to;
        log.debug("[LGD] Fetching villages by duration stateCode={} from={} to={}", stateCode, from, to);
        String body = postWithRetry(path);
        if (body == null || body.isBlank() || body.equals("null")) {
            return Collections.emptyList();
        }
        return parseList(body, VILLAGE_LIST_TYPE, "villages-duration[state=" + stateCode + "]");
    }

    // =========================================================================
    // PRIVATE — HTTP + RETRY
    // =========================================================================

    /**
     * Executes a POST to the given path with authentication headers.
     * Retries on transient failures (5xx / connection errors) with exponential backoff.
     * Does NOT retry on auth failures or 4xx errors.
     *
     * <p>Credentials are injected into headers — NEVER logged.
     */
    private String postWithRetry(String path) {
        applyRateLimit();
        int attempt = 0;
        long backoff = props.getRetryBackoffMs();

        while (true) {
            attempt++;
            try {
                String response = restClient.post()
                        .uri(URI.create(props.getBaseUrl() + path))
                        .header("X-IBM-Client-Id", props.getClientId())
                        .header("X-IBM-Client-Secret", props.getClientSecret())
                        .retrieve()
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                            int code = res.getStatusCode().value();
                            if (code == HttpStatus.UNAUTHORIZED.value()
                                    || code == HttpStatus.FORBIDDEN.value()) {
                                throw new LgdAuthException("LGD auth failure: HTTP " + code
                                        + " — check LGD_CLIENT_ID and LGD_CLIENT_SECRET credentials");
                            }
                            if (code == HttpStatus.TOO_MANY_REQUESTS.value()) {
                                throw new LgdRateLimitException("LGD rate limit exceeded: HTTP 429 for path: " + path);
                            }
                            throw new LgdClientException("LGD client error: HTTP " + code + " for path: " + path);
                        })
                        .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                            int code = res.getStatusCode().value();
                            throw new LgdServerException("LGD server error: HTTP " + code + " for path: " + path);
                        })
                        .body(String.class);

                return response;

            } catch (LgdAuthException | LgdClientException ex) {
                // Never retry auth or permanent client-error failures (401, 403, 404, etc.)
                log.error("[LGD] Non-retryable error on path '{}': {}", path, ex.getMessage());
                throw ex;

            } catch (Exception ex) {
                // Retryable: LgdRateLimitException (429), LgdServerException (5xx), connection/socket timeout errors
                if (attempt > props.getMaxRetries()) {
                    log.error("[LGD] Max retries ({}) exhausted for path '{}'. Last error: {}",
                            props.getMaxRetries(), path, ex.getMessage());
                    throw new LgdServerException("LGD call failed after " + props.getMaxRetries() + " retries for path: " + path, ex);
                }
                log.warn("[LGD] Attempt {}/{} failed for path '{}': {}. Retrying in {}ms...",
                        attempt, props.getMaxRetries(), path, ex.getMessage(), backoff);
                sleep(backoff);
                backoff *= 2; // exponential backoff
            }
        }
    }

    /**
     * Applies configured delay between requests.
     * Guards against rate limiting. Official LGD rate limit is NOT established
     * by the provided specification — conservative configurable default used.
     */
    private void applyRateLimit() {
        if (props.getRequestDelayMs() > 0) {
            sleep(props.getRequestDelayMs());
        }
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private <T> List<T> parseList(String body, TypeReference<List<T>> typeRef, String context) {
        try {
            List<T> result = objectMapper.readValue(body, typeRef);
            return result != null ? result : Collections.emptyList();
        } catch (Exception e) {
            log.warn("[LGD] Failed to parse response for '{}'. Body snippet: {}. Error: {}",
                    context, truncate(body, 200), e.getMessage());
            return Collections.emptyList();
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "<null>";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    // =========================================================================
    // EXCEPTION TYPES
    // =========================================================================

    public static class LgdAuthException extends RuntimeException {
        public LgdAuthException(String msg) { super(msg); }
    }

    public static class LgdClientException extends RuntimeException {
        public LgdClientException(String msg) { super(msg); }
    }

    public static class LgdRateLimitException extends RuntimeException {
        public LgdRateLimitException(String msg) { super(msg); }
    }

    public static class LgdServerException extends RuntimeException {
        public LgdServerException(String msg) { super(msg); }
        public LgdServerException(String msg, Throwable cause) { super(msg, cause); }
    }
}
