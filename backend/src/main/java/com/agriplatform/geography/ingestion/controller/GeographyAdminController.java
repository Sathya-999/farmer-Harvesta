package com.agriplatform.geography.ingestion.controller;

import com.agriplatform.common.response.ApiResponse;
import com.agriplatform.geography.ingestion.model.ImportMode;
import com.agriplatform.geography.ingestion.model.ImportSummary;
import com.agriplatform.geography.ingestion.service.GeographyIngestionService;
import com.agriplatform.geography.ingestion.source.LgdCsvGeographySource;
import com.agriplatform.geography.ingestion.source.LgdJsonGeographySource;
import com.agriplatform.geography.lgd.sync.LgdSyncOperations;
import com.agriplatform.geography.lgd.sync.LgdSyncResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Administrative controller for geography master data validation and ingestion.
 *
 * <h2>Security Boundary</h2>
 * <ul>
 *   <li>Endpoints are NEVER public. Authentication is strictly required.</li>
 *   <li>Guarded by {@code @PreAuthorize("hasRole('admin') or hasAuthority('ROLE_admin')")}.</li>
 *   <li>Standard users/farmers (with authority {@code ROLE_authenticated}) receive HTTP 403 Forbidden.</li>
 *   <li>Unauthenticated callers receive HTTP 401 Unauthorized.</li>
 *   <li>Zero database credentials or internal secrets exposed.</li>
 *   <li>LGD credentials are NEVER returned in any response body.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/admin/geography")
@PreAuthorize("hasRole('admin') or hasAuthority('ROLE_admin')")
public class GeographyAdminController {

    private static final DateTimeFormatter LGD_DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final GeographyIngestionService ingestionService;
    private final LgdSyncOperations lgdSyncService;
    private final ObjectMapper objectMapper;

    public GeographyAdminController(
            GeographyIngestionService ingestionService,
            LgdSyncOperations lgdSyncService,
            ObjectMapper objectMapper) {
        this.ingestionService = ingestionService;
        this.lgdSyncService = lgdSyncService;
        this.objectMapper = objectMapper;
    }

    // =========================================================================
    // EXISTING CSV / JSON INGESTION ENDPOINTS (PRESERVED UNCHANGED)
    // =========================================================================

    /**
     * Validates a CSV file without writing to the database (Dry-Run mode).
     */
    @PostMapping(value = "/validate-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportSummary>> validateCsv(
            @RequestParam("file") MultipartFile file) throws IOException {

        LgdCsvGeographySource source = new LgdCsvGeographySource(file.getOriginalFilename(), file.getInputStream());
        ImportSummary summary = ingestionService.ingest(source, ImportMode.DRY_RUN);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Ingests a CSV file into the database with idempotent upsert.
     */
    @PostMapping(value = "/import-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportSummary>> importCsv(
            @RequestParam("file") MultipartFile file) throws IOException {

        LgdCsvGeographySource source = new LgdCsvGeographySource(file.getOriginalFilename(), file.getInputStream());
        ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Validates a JSON payload without writing to the database (Dry-Run mode).
     */
    @PostMapping(value = "/validate-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ImportSummary>> validateJson(
            @RequestBody String jsonPayload) throws IOException {

        LgdJsonGeographySource source = new LgdJsonGeographySource("json-payload", jsonPayload, objectMapper);
        ImportSummary summary = ingestionService.ingest(source, ImportMode.DRY_RUN);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * Ingests a JSON payload into the database with idempotent upsert.
     */
    @PostMapping(value = "/import-json", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<ImportSummary>> importJson(
            @RequestBody String jsonPayload) throws IOException {

        LgdJsonGeographySource source = new LgdJsonGeographySource("json-payload", jsonPayload, objectMapper);
        ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // =========================================================================
    // LGD LIVE API SYNCHRONIZATION ENDPOINTS
    // =========================================================================

    /**
     * Triggers a full initial synchronization from the official LGD/NAPIX API.
     *
     * <p>Idempotent: safe to run multiple times. LGD integer codes are the identity key.
     * <p>Security: admin-only. LGD credentials are NEVER returned in the response.
     *
     * <ul>
     *   <li>Unauthenticated → 401</li>
     *   <li>Authenticated farmer → 403</li>
     *   <li>Authenticated admin → 202 Accepted + sync result</li>
     * </ul>
     */
    @PostMapping("/sync-lgd/initial")
    public ResponseEntity<ApiResponse<LgdSyncResult>> triggerInitialLgdSync() {
        LgdSyncResult result = lgdSyncService.runInitialSync();
        return ResponseEntity.accepted().body(ApiResponse.success(result));
    }

    /**
     * Triggers an incremental synchronization from the official LGD/NAPIX API.
     *
     * <p>Date format: DD-MM-YYYY (required by all LGD duration APIs).
     * <p>Defaults: past 7 days → today if not provided.
     * <p>Security: admin-only. LGD credentials are NEVER returned in the response.
     *
     * <ul>
     *   <li>Unauthenticated → 401</li>
     *   <li>Authenticated farmer → 403</li>
     *   <li>Authenticated admin → 202 Accepted + sync result</li>
     * </ul>
     */
    @PostMapping("/sync-lgd/incremental")
    public ResponseEntity<ApiResponse<LgdSyncResult>> triggerIncrementalLgdSync(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to",   required = false) String to) {

        if (from == null || from.isBlank()) {
            from = LocalDate.now().minusDays(7).format(LGD_DATE_FORMAT);
        }
        if (to == null || to.isBlank()) {
            to = LocalDate.now().format(LGD_DATE_FORMAT);
        }

        LgdSyncResult result = lgdSyncService.runIncrementalSync(from, to);
        return ResponseEntity.accepted().body(ApiResponse.success(result));
    }
}
