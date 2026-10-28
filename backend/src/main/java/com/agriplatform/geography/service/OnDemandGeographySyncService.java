package com.agriplatform.geography.service;

import com.agriplatform.geography.dto.StateCoverageDto;
import com.agriplatform.geography.dto.StateSyncStatusDto;
import com.agriplatform.geography.ingestion.model.ImportMode;
import com.agriplatform.geography.ingestion.model.ImportSummary;
import com.agriplatform.geography.ingestion.service.GeographyIngestionService;
import com.agriplatform.geography.ingestion.source.LgdCsvGeographySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Orchestrates secure, asynchronous on-demand state geography synchronization.
 *
 * <p>Guarantees:
 * <ul>
 *   <li>Accepts only pre-approved pilot state codes.</li>
 *   <li>Deduplicates concurrent requests for the same state.</li>
 *   <li>Never blocks interactive web requests; runs asynchronously in bounded background executor.</li>
 *   <li>Restricts synchronization to the requested state ONLY.</li>
 *   <li>Zero administrative credentials or raw SQL exposed to clients.</li>
 * </ul>
 */
@Service
public class OnDemandGeographySyncService {

    private static final Logger log = LoggerFactory.getLogger(OnDemandGeographySyncService.class);

    private final GeographyLibraryIntegrationService libraryService;
    private final GeographyIngestionService ingestionService;
    private final Executor syncExecutor;

    private final Map<String, StateSyncStatusDto> jobStatuses = new ConcurrentHashMap<>();

    @Autowired
    public OnDemandGeographySyncService(
            GeographyLibraryIntegrationService libraryService,
            GeographyIngestionService ingestionService) {
        this(libraryService, ingestionService, Executors.newFixedThreadPool(2));
    }

    public OnDemandGeographySyncService(
            GeographyLibraryIntegrationService libraryService,
            GeographyIngestionService ingestionService,
            Executor executor) {
        this.libraryService = libraryService;
        this.ingestionService = ingestionService;
        this.syncExecutor = executor != null ? executor : Executors.newFixedThreadPool(2);
    }

    /**
     * Initiates asynchronous synchronization for an approved state if required.
     *
     * @param stateCode the 2-digit official state code
     * @return current or newly launched synchronization job status
     */
    public synchronized StateSyncStatusDto requestStateSynchronization(String stateCode) {
        if (!libraryService.isApprovedState(stateCode)) {
            throw new IllegalArgumentException("State code '" + stateCode + "' is not an approved pilot state");
        }

        var manifest = libraryService.getManifest(stateCode);

        // 1. Check existing in-flight job (Deduplication)
        StateSyncStatusDto current = jobStatuses.get(stateCode);
        if (current != null && "IN_PROGRESS".equals(current.status())) {
            log.info("State synchronization for {} ({}) already in progress — returning existing job status",
                    manifest.stateName(), stateCode);
            return current;
        }

        // 2. Check current database coverage
        StateCoverageDto coverage = libraryService.checkStateCoverage(stateCode);
        if ("COMPLETE".equals(coverage.coverageStatus())) {
            StateSyncStatusDto completed = StateSyncStatusDto.completed(
                    stateCode, manifest.stateName(), manifest.villages(),
                    OffsetDateTime.now(ZoneOffset.UTC), OffsetDateTime.now(ZoneOffset.UTC),
                    "State geography is already 100% complete (" + coverage.dbVillages() + " villages present). No synchronization needed."
            );
            jobStatuses.put(stateCode, completed);
            return completed;
        }

        // 3. Launch asynchronous synchronization
        OffsetDateTime startTime = OffsetDateTime.now(ZoneOffset.UTC);
        StateSyncStatusDto initialProgress = StateSyncStatusDto.inProgress(
                stateCode, manifest.stateName(), coverage.dbVillages(), manifest.villages(), startTime);
        jobStatuses.put(stateCode, initialProgress);

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting background synchronization for state {} ({})", manifest.stateName(), stateCode);
                File sourceFile = libraryService.resolveApprovedStateCsvFile(stateCode);

                try (FileInputStream fis = new FileInputStream(sourceFile)) {
                    LgdCsvGeographySource source = new LgdCsvGeographySource(sourceFile.getName(), fis);
                    ImportSummary summary = ingestionService.ingest(source, ImportMode.IMPORT);

                    OffsetDateTime completedTime = OffsetDateTime.now(ZoneOffset.UTC);
                    String msg = String.format("Synchronization completed: inserted %d mandals, %d villages in %d ms (valid=%d, invalid=%d)",
                            summary.getMandalsInserted(), summary.getVillagesInserted(),
                            summary.getDurationMs(), summary.getValidRowsCount(), summary.getInvalidRowsCount());

                    StateSyncStatusDto finalStatus;
                    if (summary.isSuccessful() || summary.getVillagesInserted() > 0) {
                        finalStatus = StateSyncStatusDto.completed(
                                stateCode, manifest.stateName(), summary.getTotalRowsRead(),
                                startTime, completedTime, msg);
                    } else {
                        finalStatus = StateSyncStatusDto.failed(
                                stateCode, manifest.stateName(), startTime,
                                "Synchronization completed with errors: " + summary.getErrors().size() + " validation errors");
                    }
                    jobStatuses.put(stateCode, finalStatus);
                    log.info("Background synchronization finished for {}: {}", manifest.stateName(), msg);
                }
            } catch (Exception e) {
                log.error("Background synchronization failed for state " + stateCode, e);
                jobStatuses.put(stateCode, StateSyncStatusDto.failed(
                        stateCode, manifest.stateName(), startTime, "Synchronization failed: " + e.getMessage()));
            }
        }, syncExecutor);

        return initialProgress;
    }

    /**
     * Returns the latest synchronization status for a state.
     */
    public StateSyncStatusDto getSyncStatus(String stateCode) {
        if (!libraryService.isApprovedState(stateCode)) {
            throw new IllegalArgumentException("State code '" + stateCode + "' is not an approved pilot state");
        }
        var manifest = libraryService.getManifest(stateCode);
        return jobStatuses.getOrDefault(stateCode, StateSyncStatusDto.idle(stateCode, manifest.stateName()));
    }
}
