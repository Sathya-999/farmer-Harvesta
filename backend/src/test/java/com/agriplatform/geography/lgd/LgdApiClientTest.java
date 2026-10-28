package com.agriplatform.geography.lgd;

import com.agriplatform.geography.lgd.client.LgdApiClient;
import com.agriplatform.geography.lgd.config.LgdProperties;
import com.agriplatform.geography.lgd.dto.LgdStateDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration-level unit tests for {@link LgdApiClient}.
 *
 * <p>Uses the JDK built-in {@code com.sun.net.httpserver.HttpServer}
 * (no additional test dependencies required) to verify:
 * <ul>
 *   <li>Correct authentication headers are sent (X-IBM-Client-Id, X-IBM-Client-Secret)</li>
 *   <li>Credentials are NEVER absent from outbound requests</li>
 *   <li>401/403 response throws {@code LgdAuthException}</li>
 *   <li>5xx response throws {@code LgdServerException}</li>
 *   <li>Empty/null response returns empty list safely</li>
 *   <li>Valid JSON response is parsed correctly</li>
 *   <li>Correct URL paths are used (POST method, no query params)</li>
 * </ul>
 */
@DisplayName("LgdApiClient — HTTP Authentication and Response Tests")
class LgdApiClientTest {

    private static final String CLIENT_ID     = "test-client-id-12345";
    private static final String CLIENT_SECRET = "test-client-secret-67890";

    private HttpServer server;
    private int port;
    private LgdApiClient client;

    /** Captures the last received request for assertion. */
    private final AtomicReference<HttpExchange> lastRequest = new AtomicReference<>();

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.setExecutor(null); // default executor
        port = server.getAddress().getPort();
        server.start();

        LgdProperties props = new LgdProperties();
        props.setClientId(CLIENT_ID);
        props.setClientSecret(CLIENT_SECRET);
        props.setBaseUrl("http://localhost:" + port);
        props.setRequestDelayMs(0);     // no rate limit in tests
        props.setMaxRetries(1);         // minimal retries
        props.setRetryBackoffMs(50);    // fast backoff
        props.setConnectTimeoutMs(3000);
        props.setReadTimeoutMs(3000);

        client = new LgdApiClient(props, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    // =========================================================================
    // AUTHENTICATION HEADERS
    // =========================================================================

    @Test
    @DisplayName("Request includes X-IBM-Client-Id header with correct value")
    void request_includesClientIdHeader() throws Exception {
        registerPath("/states/v1/status/active", 200,
                "[{\"stateCode\":28,\"stateNameEnglish\":\"Andhra Pradesh\",\"isactive\":true}]");

        client.fetchActiveStates();

        HttpExchange req = lastRequest.get();
        assertThat(req).isNotNull();
        assertThat(req.getRequestHeaders().getFirst("X-IBM-Client-Id"))
                .isEqualTo(CLIENT_ID);
    }

    @Test
    @DisplayName("Request includes X-IBM-Client-Secret header with correct value")
    void request_includesClientSecretHeader() throws Exception {
        registerPath("/states/v1/status/active", 200,
                "[{\"stateCode\":28,\"stateNameEnglish\":\"Andhra Pradesh\",\"isactive\":true}]");

        client.fetchActiveStates();

        assertThat(lastRequest.get().getRequestHeaders().getFirst("X-IBM-Client-Secret"))
                .isEqualTo(CLIENT_SECRET);
    }

    @Test
    @DisplayName("Request uses POST method")
    void request_usesPostMethod() throws Exception {
        registerPath("/states/v1/status/active", 200, "[]");

        client.fetchActiveStates();

        assertThat(lastRequest.get().getRequestMethod()).isEqualTo("POST");
    }

    // =========================================================================
    // RESPONSE PARSING
    // =========================================================================

    @Test
    @DisplayName("Valid JSON state list is parsed correctly")
    void fetchActiveStates_validResponse_parsed() throws Exception {
        registerPath("/states/v1/status/active", 200,
                "[{\"stateCode\":28,\"stateNameEnglish\":\"Andhra Pradesh\",\"isactive\":true}," +
                " {\"stateCode\":29,\"stateNameEnglish\":\"Karnataka\",\"isactive\":true}]");

        List<LgdStateDto> states = client.fetchActiveStates();

        assertThat(states).hasSize(2);
        assertThat(states.get(0).getStateCode()).isEqualTo(28);
        assertThat(states.get(0).getStateNameEnglish()).isEqualTo("Andhra Pradesh");
        assertThat(states.get(0).isEffectivelyActive()).isTrue();
    }

    @Test
    @DisplayName("Empty JSON array returns empty list without exception")
    void fetchActiveStates_emptyArray_returnsEmpty() throws Exception {
        registerPath("/states/v1/status/active", 200, "[]");

        List<LgdStateDto> states = client.fetchActiveStates();

        assertThat(states).isEmpty();
    }

    @Test
    @DisplayName("'null' body returns empty list without exception")
    void fetchActiveStates_nullBody_returnsEmpty() throws Exception {
        registerPath("/states/v1/status/active", 200, "null");

        List<LgdStateDto> states = client.fetchActiveStates();

        assertThat(states).isEmpty();
    }

    @Test
    @DisplayName("Blank body returns empty list without exception")
    void fetchActiveStates_blankBody_returnsEmpty() throws Exception {
        registerPath("/states/v1/status/active", 200, "");

        List<LgdStateDto> states = client.fetchActiveStates();

        assertThat(states).isEmpty();
    }

    // =========================================================================
    // AUTHENTICATION FAILURE (NOT RETRIED)
    // =========================================================================

    @Test
    @DisplayName("HTTP 401 throws LgdAuthException (not retried)")
    void fetchActiveStates_401_throwsLgdAuthException() throws Exception {
        registerPath("/states/v1/status/active", 401, "Unauthorized");

        assertThatThrownBy(() -> client.fetchActiveStates())
                .isInstanceOf(LgdApiClient.LgdAuthException.class)
                .hasMessageContaining("LGD auth failure");
    }

    @Test
    @DisplayName("HTTP 403 throws LgdAuthException")
    void fetchActiveStates_403_throwsLgdAuthException() throws Exception {
        registerPath("/states/v1/status/active", 403, "Forbidden");

        assertThatThrownBy(() -> client.fetchActiveStates())
                .isInstanceOf(LgdApiClient.LgdAuthException.class);
    }

    // =========================================================================
    // SERVER ERROR
    // =========================================================================

    @Test
    @DisplayName("HTTP 500 throws LgdServerException after retries exhausted")
    void fetchActiveStates_500_throwsLgdServerException() throws Exception {
        registerPath("/states/v1/status/active", 500, "Internal Server Error");

        assertThatThrownBy(() -> client.fetchActiveStates())
                .isInstanceOf(LgdApiClient.LgdServerException.class);
    }

    // =========================================================================
    // URL CONSTRUCTION
    // =========================================================================

    @Test
    @DisplayName("fetchActiveDistrictsByState uses /state/{code}/districts/v1/status/active")
    void fetchActiveDistrictsByState_correctPath() throws Exception {
        registerPath("/state/28/districts/v1/status/active", 200,
                "[{\"districtCode\":502,\"districtNameEnglish\":\"Ananthapuramu\",\"stateCode\":28,\"isactive\":true}]");

        var districts = client.fetchActiveDistrictsByState(28);

        assertThat(districts).hasSize(1);
        assertThat(districts.get(0).getDistrictCode()).isEqualTo(502);
        // Verify correct path was hit
        assertThat(lastRequest.get().getRequestURI().getPath())
                .isEqualTo("/state/28/districts/v1/status/active");
    }

    @Test
    @DisplayName("fetchHierarchyPage uses /stateDistrictSubdistBlockVillage/{code}/page/{page}")
    void fetchHierarchyPage_correctPath() throws Exception {
        registerPath("/stateDistrictSubdistBlockVillage/28/page/1", 200, "[]");

        var rows = client.fetchHierarchyPage(28, 1);

        assertThat(rows).isEmpty();
        assertThat(lastRequest.get().getRequestURI().getPath())
                .isEqualTo("/stateDistrictSubdistBlockVillage/28/page/1");
    }

    @Test
    @DisplayName("fetchStatesByDuration encodes DD-MM-YYYY dates in path")
    void fetchStatesByDuration_correctPath() throws Exception {
        registerPath("/states/v1/updatedFrom/01-09-2026/updatedTo/01-10-2026", 200, "[]");

        var states = client.fetchStatesByDuration("01-09-2026", "01-10-2026");

        assertThat(states).isEmpty();
        assertThat(lastRequest.get().getRequestURI().getPath())
                .contains("updatedFrom/01-09-2026/updatedTo/01-10-2026");
    }

    @Test
    @DisplayName("fetchActiveSubDistrictsByState uses /state/{code}/subdistricts/v1/status/active")
    void fetchActiveSubDistrictsByState_correctPath() throws Exception {
        registerPath("/state/28/subdistricts/v1/status/active", 200,
                "[{\"subDistrictCode\":5032,\"subDistrictNameEnglish\":\"Kalyandurg\",\"districtCode\":502,\"isactive\":true}]");

        var subdistricts = client.fetchActiveSubDistrictsByState(28);

        assertThat(subdistricts).hasSize(1);
        assertThat(subdistricts.get(0).getSubDistrictCode()).isEqualTo(5032);
        assertThat(lastRequest.get().getRequestURI().getPath())
                .isEqualTo("/state/28/subdistricts/v1/status/active");
        assertThat(lastRequest.get().getRequestMethod()).isEqualTo("POST");
        assertThat(lastRequest.get().getRequestHeaders().getFirst("X-IBM-Client-Id")).isEqualTo(CLIENT_ID);
        assertThat(lastRequest.get().getRequestHeaders().getFirst("X-IBM-Client-Secret")).isEqualTo(CLIENT_SECRET);
    }

    @Test
    @DisplayName("fetchActiveVillagesByState uses /state/{code}/villages/v2/status/active")
    void fetchActiveVillagesByState_correctPath() throws Exception {
        registerPath("/state/28/villages/v2/status/active", 200,
                "[{\"villageCode\":597123,\"villageNameEnglish\":\"Chapiri\",\"subDistrictCode\":5032,\"districtCode\":502,\"isactive\":true}]");

        var villages = client.fetchActiveVillagesByState(28);

        assertThat(villages).hasSize(1);
        assertThat(villages.get(0).getVillageCode()).isEqualTo(597123);
        assertThat(lastRequest.get().getRequestURI().getPath())
                .isEqualTo("/state/28/villages/v2/status/active");
        assertThat(lastRequest.get().getRequestMethod()).isEqualTo("POST");
        assertThat(lastRequest.get().getRequestHeaders().getFirst("X-IBM-Client-Id")).isEqualTo(CLIENT_ID);
        assertThat(lastRequest.get().getRequestHeaders().getFirst("X-IBM-Client-Secret")).isEqualTo(CLIENT_SECRET);
    }

    @Test
    @DisplayName("fetchDistrictsByStateDuration uses /state/{code}/districts/v1/updatedFrom/{from}/updatedTo/{to}")
    void fetchDistrictsByStateDuration_correctPath() throws Exception {
        registerPath("/state/28/districts/v1/updatedFrom/01-09-2026/updatedTo/01-10-2026", 200, "[]");

        var districts = client.fetchDistrictsByStateDuration(28, "01-09-2026", "01-10-2026");

        assertThat(districts).isEmpty();
        assertThat(lastRequest.get().getRequestURI().getPath())
                .isEqualTo("/state/28/districts/v1/updatedFrom/01-09-2026/updatedTo/01-10-2026");
        assertThat(lastRequest.get().getRequestMethod()).isEqualTo("POST");
    }

    @Test
    @DisplayName("fetchSubDistrictsByStateDuration uses /state/{code}/subdistricts/v2/updatedFrom/{from}/updatedTo/{to}")
    void fetchSubDistrictsByStateDuration_correctPath() throws Exception {
        registerPath("/state/28/subdistricts/v2/updatedFrom/01-09-2026/updatedTo/01-10-2026", 200, "[]");

        var subdistricts = client.fetchSubDistrictsByStateDuration(28, "01-09-2026", "01-10-2026");

        assertThat(subdistricts).isEmpty();
        assertThat(lastRequest.get().getRequestURI().getPath())
                .isEqualTo("/state/28/subdistricts/v2/updatedFrom/01-09-2026/updatedTo/01-10-2026");
        assertThat(lastRequest.get().getRequestMethod()).isEqualTo("POST");
    }

    @Test
    @DisplayName("fetchVillagesByStateDuration uses /state/{code}/villages/v2/updatedFrom/{from}/updatedTo/{to}")
    void fetchVillagesByStateDuration_correctPath() throws Exception {
        registerPath("/state/28/villages/v2/updatedFrom/01-09-2026/updatedTo/01-10-2026", 200, "[]");

        var villages = client.fetchVillagesByStateDuration(28, "01-09-2026", "01-10-2026");

        assertThat(villages).isEmpty();
        assertThat(lastRequest.get().getRequestURI().getPath())
                .isEqualTo("/state/28/villages/v2/updatedFrom/01-09-2026/updatedTo/01-10-2026");
        assertThat(lastRequest.get().getRequestMethod()).isEqualTo("POST");
    }


    @Test
    @DisplayName("HTTP 429 triggers retry and eventually throws LgdServerException when exhausted")
    void fetchActiveStates_429_triggersRetry() throws Exception {
        registerPath("/states/v1/status/active", 429, "Too Many Requests");

        assertThatThrownBy(() -> client.fetchActiveStates())
                .isInstanceOf(LgdApiClient.LgdServerException.class)
                .hasMessageContaining("retries");
    }

    @Test
    @DisplayName("HTTP 404 throws LgdClientException without retrying")
    void fetchActiveStates_404_throwsClientException() throws Exception {
        registerPath("/states/v1/status/active", 404, "Not Found");

        assertThatThrownBy(() -> client.fetchActiveStates())
                .isInstanceOf(LgdApiClient.LgdClientException.class)
                .hasMessageContaining("HTTP 404");
    }

    @Test
    @DisplayName("Malformed JSON response body returns empty list safely without exception")
    void fetchActiveStates_malformedJson_returnsEmptySafely() throws Exception {
        registerPath("/states/v1/status/active", 200, "{ invalid json !@#$% }");

        List<LgdStateDto> states = client.fetchActiveStates();

        assertThat(states).isEmpty();
    }

    // =========================================================================
    // HELPER
    // =========================================================================

    /**
     * Registers an HTTP handler at the given path that responds with the given
     * status and body, and captures the request for assertion.
     */
    private void registerPath(String path, int statusCode, String responseBody) {
        server.createContext(path, exchange -> {
            lastRequest.set(exchange);
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }
}
