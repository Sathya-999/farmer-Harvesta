package com.agriplatform.geography.ingestion.controller;

import com.agriplatform.common.config.SecurityConfig;
import com.agriplatform.common.exception.GlobalExceptionHandler;
import com.agriplatform.geography.controller.GeographyController;
import com.agriplatform.geography.ingestion.model.ImportMode;
import com.agriplatform.geography.ingestion.model.ImportSummary;
import com.agriplatform.geography.ingestion.service.GeographyIngestionService;
import com.agriplatform.geography.lgd.sync.LgdSyncOperations;
import com.agriplatform.geography.lgd.sync.LgdSyncResult;
import com.agriplatform.geography.service.GeographyLibraryIntegrationService;
import com.agriplatform.geography.service.GeographyLocationService;
import com.agriplatform.geography.service.GeographyService;
import com.agriplatform.geography.service.OnDemandGeographySyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
    controllers = {GeographyAdminController.class, GeographyController.class},
    excludeAutoConfiguration = {
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    }
)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@ActiveProfiles("test")
@DisplayName("Geography Admin Controller & Security Tests")
class GeographyAdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GeographyIngestionService ingestionService;

    @MockBean
    private LgdSyncOperations lgdSyncService;

    @MockBean
    private GeographyService geographyService;

    @MockBean
    private GeographyLocationService locationService;

    @MockBean
    private GeographyLibraryIntegrationService libraryService;

    @MockBean
    private OnDemandGeographySyncService syncService;

    // -------------------------------------------------------------------------
    // 1. Unauthenticated requests to Admin Ingestion endpoints MUST be 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Unauthenticated POST /validate-csv is rejected with 401 Unauthorized")
    void unauthenticatedValidateCsvIsUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "dummy".getBytes());

        mockMvc.perform(multipart("/api/v1/admin/geography/validate-csv").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated POST /import-csv is rejected with 401 Unauthorized")
    void unauthenticatedImportCsvIsUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "dummy".getBytes());

        mockMvc.perform(multipart("/api/v1/admin/geography/import-csv").file(file))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated POST /import-json is rejected with 401 Unauthorized")
    void unauthenticatedImportJsonIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/admin/geography/import-json")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // 2. LGD sync endpoints — unauthenticated → 401
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Unauthenticated POST /sync-lgd/initial is rejected with 401")
    void unauthenticated_syncLgdInitial_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/admin/geography/sync-lgd/initial"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Unauthenticated POST /sync-lgd/incremental is rejected with 401")
    void unauthenticated_syncLgdIncremental_isUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/admin/geography/sync-lgd/incremental"))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // 3. Standard farmer / authenticated user WITHOUT admin role MUST be 403
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "authenticated") // Standard Supabase user role
    @DisplayName("Authenticated standard user (farmer) calling admin import is 403 Forbidden")
    void standardUserCallingImportIsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/geography/import-json")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "authenticated")
    @DisplayName("Farmer calling sync-lgd/initial is 403 Forbidden")
    void farmer_syncLgdInitial_isForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/geography/sync-lgd/initial"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "authenticated")
    @DisplayName("Farmer calling sync-lgd/incremental is 403 Forbidden")
    void farmer_syncLgdIncremental_isForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/admin/geography/sync-lgd/incremental"))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // 4. Admin user WITH ROLE_admin is allowed
    // -------------------------------------------------------------------------

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("Admin user with ROLE_admin calling import-json is 200 OK")
    void adminUserCallingImportIsAllowed() throws Exception {
        ImportSummary summary = new ImportSummary();
        summary.setMode(ImportMode.IMPORT);
        summary.setSuccessful(true);
        when(ingestionService.ingest(any(), eq(ImportMode.IMPORT))).thenReturn(summary);

        mockMvc.perform(post("/api/v1/admin/geography/import-json")
                .contentType(MediaType.APPLICATION_JSON)
                .content("[]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.mode").value("IMPORT"));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("Admin calling sync-lgd/initial returns 202 Accepted")
    void admin_syncLgdInitial_isAccepted() throws Exception {
        LgdSyncResult result = new LgdSyncResult(LgdSyncResult.SyncType.INITIAL);
        result.complete();
        when(lgdSyncService.runInitialSync()).thenReturn(result);

        mockMvc.perform(post("/api/v1/admin/geography/sync-lgd/initial"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.syncType").value("INITIAL"));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("Admin calling sync-lgd/incremental with params returns 202 Accepted")
    void admin_syncLgdIncremental_isAccepted() throws Exception {
        LgdSyncResult result = new LgdSyncResult(LgdSyncResult.SyncType.INCREMENTAL);
        result.complete();
        when(lgdSyncService.runIncrementalSync("01-09-2026", "01-10-2026")).thenReturn(result);

        mockMvc.perform(post("/api/v1/admin/geography/sync-lgd/incremental")
                .param("from", "01-09-2026")
                .param("to", "01-10-2026"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.syncType").value("INCREMENTAL"));
    }

    @Test
    @WithMockUser(roles = "admin")
    @DisplayName("Admin calling sync-lgd/incremental without params uses defaults (no crash)")
    void admin_syncLgdIncremental_defaultDates_isAccepted() throws Exception {
        LgdSyncResult result = new LgdSyncResult(LgdSyncResult.SyncType.INCREMENTAL);
        result.complete();
        // Default dates are calculated at runtime — use any() matcher
        when(lgdSyncService.runIncrementalSync(any(), any())).thenReturn(result);

        mockMvc.perform(post("/api/v1/admin/geography/sync-lgd/incremental"))
                .andExpect(status().isAccepted());
    }

    // -------------------------------------------------------------------------
    // 5. Regression: Public GET /api/v1/geography/states remains 200 OK
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Public GET /api/v1/geography/states remains accessible without authentication")
    void publicGetStatesRemainsAccessible() throws Exception {
        when(geographyService.getAllActiveStates()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/geography/states"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // -------------------------------------------------------------------------
    // 6. Write attempts on public geography endpoint are blocked
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /api/v1/geography/states is blocked (not permitted)")
    void postToPublicGeographyIsBlocked() throws Exception {
        mockMvc.perform(post("/api/v1/geography/states")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
