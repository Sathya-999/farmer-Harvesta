package com.agriplatform.common;

import com.agriplatform.analytics.service.AnalyticsService;
import com.agriplatform.notification.service.NotificationService;
import com.agriplatform.cultivation.service.CultivationService;
import com.agriplatform.geography.lgd.client.LgdApiOperations;
import com.agriplatform.geography.lgd.sync.LgdSyncOperations;
import com.agriplatform.geography.service.GeographyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test that verifies the Actuator health endpoint is publicly
 * accessible without a JWT token.
 *
 * <p>Uses {@code @SpringBootTest(webEnvironment = RANDOM_PORT)} to load the
 * full application context including the Actuator servlet infrastructure.
 * This is the correct test slice for Actuator endpoints.
 *
 * <p>The {@code test} profile excludes JPA/Datasource auto-configuration
 * (see {@code application-test.yml}), so no database connection is needed.
 * {@link GeographyService} is mocked because its implementation depends on
 * JPA repositories that are unavailable without JPA auto-configuration.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Actuator Security Integration Tests")
class ActuatorSecurityTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * Mock the GeographyService to break the dependency chain:
     * GeographyController → GeographyServiceImpl → JPA Repositories.
     * Without JPA auto-config, the repositories can't be created.
     */
    @MockBean
    @SuppressWarnings("unused")
    private GeographyService geographyService;

    @MockBean
    @SuppressWarnings("unused")
    private com.agriplatform.geography.ingestion.service.GeographyIngestionService geographyIngestionService;

    @MockBean
    @SuppressWarnings("unused")
    private com.agriplatform.geography.service.GeographyLocationService geographyLocationService;

    @MockBean
    @SuppressWarnings("unused")
    private com.agriplatform.geography.service.GeographyLibraryIntegrationService geographyLibraryIntegrationService;

    @MockBean
    @SuppressWarnings("unused")
    private com.agriplatform.geography.service.OnDemandGeographySyncService onDemandGeographySyncService;

    @MockBean
    @SuppressWarnings("unused")
    private LgdApiOperations lgdApiOperations;

    @MockBean
    @SuppressWarnings("unused")
    private LgdSyncOperations lgdSyncOperations;

    @MockBean
    @SuppressWarnings("unused")
    private com.agriplatform.farmer.service.FarmerProfileService farmerProfileService;

    @MockBean
    @SuppressWarnings("unused")
    private com.agriplatform.plot.service.PlotService plotService;

    @MockBean
    @SuppressWarnings("unused")
    private CultivationService cultivationService;

    @MockBean
    @SuppressWarnings("unused")
    private com.agriplatform.history.service.HistoryService historyService;

    /**
     * Mock AnalyticsService to break the dependency chain:
     * AnalyticsController → AnalyticsServiceImpl → AnalyticsRepository (JPA).
     * Without JPA auto-config, the repository cannot be instantiated.
     */
    @MockBean
    @SuppressWarnings("unused")
    private AnalyticsService analyticsService;

    /**
     * Mock NotificationService to break the dependency chain:
     * NotificationController → NotificationServiceImpl → NotificationRepository (JPA).
     * Without JPA auto-config, the repository cannot be instantiated.
     */
    @MockBean
    @SuppressWarnings("unused")
    private NotificationService notificationService;

    /**
     * Mock AuthService to break the dependency chain:
     * AuthController → AuthServiceImpl → JdbcTemplate.
     * Without DataSource auto-config in test slice, JdbcTemplate cannot be instantiated.
     */
    @MockBean
    @SuppressWarnings("unused")
    private com.agriplatform.auth.service.AuthService authService;

    // =========================================================================
    // Actuator health is accessible without any JWT (public endpoint)
    // =========================================================================

    @Test
    @DisplayName("Actuator /health endpoint returns 200 without Authorization header")
    void actuator_health_returns_200_without_jwt() {
        ResponseEntity<String> response = restTemplate
            .getForEntity("http://localhost:" + port + "/actuator/health", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // =========================================================================
    // Farmer API returns 401 without JWT (protected by default)
    // =========================================================================

    @Test
    @DisplayName("Protected endpoint returns 401 without JWT in full application context")
    void protected_endpoint_returns_401_without_jwt_full_context() {
        ResponseEntity<String> response = restTemplate
            .getForEntity("http://localhost:" + port + "/api/v1/farmer/profile", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
