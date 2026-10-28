package com.agriplatform.geography.service;

import com.agriplatform.geography.dto.StateCoverageDto;
import com.agriplatform.geography.repository.DistrictRepository;
import com.agriplatform.geography.repository.MandalRepository;
import com.agriplatform.geography.repository.StateRepository;
import com.agriplatform.geography.repository.VillageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.Village;

/**
 * Service managing integration with the Agri_Library source repository and state coverage verification.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Server-controlled allowlist mapping state codes to normalized datasets.</li>
 *   <li>Detection of complete vs partial vs empty database coverage.</li>
 *   <li>Strict protection against client-supplied arbitrary paths.</li>
 * </ul>
 */
@Service
public class GeographyLibraryIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(GeographyLibraryIntegrationService.class);

    // Official source counts derived from the five pilot datasets
    public record StateManifest(
            String stateCode,
            String stateName,
            int districts,
            int mandals,
            int villages,
            String sourceCsvFileName
    ) {}

    private static final Map<String, StateManifest> APPROVED_PILOT_STATES = Map.of(
            "28", new StateManifest("28", "Andhra Pradesh", 28, 688, 17957, "Andhra_Pradesh_lgd.csv"),
            "29", new StateManifest("29", "Karnataka", 31, 240, 30771, "Karnataka_lgd.csv"),
            "32", new StateManifest("32", "Kerala", 14, 78, 1666, "Kerala_lgd.csv"),
            "33", new StateManifest("33", "Tamil Nadu", 38, 317, 18681, "Tamil_Nadu_lgd.csv"),
            "36", new StateManifest("36", "Telangana", 33, 617, 11308, "Telangana_lgd.csv")
    );

    private final StateRepository stateRepository;
    @SuppressWarnings("unused")
    private final DistrictRepository districtRepository;
    @SuppressWarnings("unused")
    private final MandalRepository mandalRepository;
    @SuppressWarnings("unused")
    private final VillageRepository villageRepository;

    @org.springframework.beans.factory.annotation.Value("${app.library.storage.base-url:${SUPABASE_URL:}}")
    private String storageBaseUrl;

    @org.springframework.beans.factory.annotation.Value("${app.library.storage.service-role-key:${SUPABASE_SERVICE_ROLE_KEY:}}")
    private String storageServiceKey;

    @org.springframework.beans.factory.annotation.Value("${app.library.storage.bucket-name:geography-source-library}")
    private String bucketName = "geography-source-library";

    @org.springframework.beans.factory.annotation.Value("${app.library.storage.connect-timeout-ms:15000}")
    private int connectTimeoutMs = 15000;

    @org.springframework.beans.factory.annotation.Value("${app.library.storage.read-timeout-ms:60000}")
    private int readTimeoutMs = 60000;

    @org.springframework.beans.factory.annotation.Value("${app.library.storage.max-file-size-bytes:15728640}")
    private long maxFileSizeBytes = 15728640; // 15 MB max

    @Autowired
    public GeographyLibraryIntegrationService(
            StateRepository stateRepository,
            DistrictRepository districtRepository,
            MandalRepository mandalRepository,
            VillageRepository villageRepository) {
        this.stateRepository = stateRepository;
        this.districtRepository = districtRepository;
        this.mandalRepository = mandalRepository;
        this.villageRepository = villageRepository;
    }

    // Constructor for testing with custom storage settings
    public GeographyLibraryIntegrationService(
            StateRepository stateRepository,
            DistrictRepository districtRepository,
            MandalRepository mandalRepository,
            VillageRepository villageRepository,
            String storageBaseUrl,
            String storageServiceKey,
            String bucketName) {
        this(stateRepository, districtRepository, mandalRepository, villageRepository);
        this.storageBaseUrl = storageBaseUrl;
        this.storageServiceKey = storageServiceKey;
        this.bucketName = bucketName != null ? bucketName : "geography-source-library";
    }

    /**
     * Checks administrative coverage for a state against the official source manifest.
     */
    public StateCoverageDto checkStateCoverage(String stateCode) {
        StateManifest manifest = APPROVED_PILOT_STATES.get(stateCode);
        if (manifest == null) {
            throw new IllegalArgumentException("State code '" + stateCode + "' is not part of the five-state pilot");
        }

        // Query database counts
        int dbDistricts = 0;
        int dbMandals = 0;
        int dbVillages = 0;

        try {
            var counts = stateRepository.getCountsByStateCode(stateCode);
            if (counts != null && !counts.isEmpty()) {
                Object[] row = counts.get(0);
                dbDistricts = row[0] != null ? ((Number) row[0]).intValue() : 0;
                dbMandals = row[1] != null ? ((Number) row[1]).intValue() : 0;
                dbVillages = row[2] != null ? ((Number) row[2]).intValue() : 0;
            } else {
                // Graceful fallback for mock unit tests where getCountsByStateCode is not explicitly stubbed
                Optional<State> stateOpt = stateRepository.findByCodeIgnoreCase(stateCode);
                if (stateOpt.isPresent()) {
                    State state = stateOpt.get();
                    List<District> districts = districtRepository.findAllByStateId(state.getId());
                    dbDistricts = districts != null ? districts.size() : 0;
                    if (districts != null) {
                        for (District d : districts) {
                            List<Mandal> mandals = mandalRepository.findAllByDistrictId(d.getId());
                            if (mandals != null) {
                                dbMandals += mandals.size();
                                for (Mandal m : mandals) {
                                    List<Village> villages = villageRepository.findAllByMandalId(m.getId());
                                    if (villages != null) {
                                        dbVillages += villages.size();
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("Could not query live DB counts for state {}: {}", stateCode, ex.getMessage());
        }

        String coverageStatus;
        boolean syncRequired;

        if (dbVillages >= manifest.villages() && dbMandals >= manifest.mandals()) {
            coverageStatus = "COMPLETE";
            syncRequired = false;
        } else if (dbVillages > 0 || dbMandals > 0) {
            coverageStatus = "PARTIAL";
            syncRequired = true;
        } else {
            coverageStatus = "EMPTY";
            syncRequired = true;
        }

        return new StateCoverageDto(
                manifest.stateCode(),
                manifest.stateName(),
                dbDistricts, manifest.districts(),
                dbMandals, manifest.mandals(),
                dbVillages, manifest.villages(),
                coverageStatus,
                syncRequired
        );
    }

    /**
     * Resolves the server-side prevalidated CSV file for an approved state code.
     * Attempts to retrieve from the private Agri_Library Storage bucket if configured,
     * with graceful, safe fallback to the prevalidated local repository cache.
     */
    public File resolveApprovedStateCsvFile(String stateCode) {
        StateManifest manifest = APPROVED_PILOT_STATES.get(stateCode);
        if (manifest == null) {
            throw new IllegalArgumentException("State code '" + stateCode + "' is not an approved pilot state");
        }

        // 1. If remote private storage credentials are provided, attempt retrieval from Agri_Library
        if (storageBaseUrl != null && !storageBaseUrl.isBlank() && storageServiceKey != null && !storageServiceKey.isBlank()) {
            try {
                File storageFile = downloadFromPrivateStorage(manifest);
                if (storageFile != null && storageFile.exists() && storageFile.length() > 0) {
                    log.info("[AGRI-LIBRARY] Successfully retrieved approved source dataset for {} from private bucket '{}'",
                            manifest.stateName(), bucketName);
                    return storageFile;
                }
            } catch (Exception ex) {
                log.warn("[AGRI-LIBRARY] Private Storage retrieval failed for {}: {}. Falling back to prevalidated local archive.",
                        manifest.sourceCsvFileName(), ex.getMessage());
            }
        }

        // 2. Safe Fallback: Prevalidated local archive replica
        // Check packaged application resources first (for deployed builds and local runtime)
        File resFile = new File("src/main/resources/geography", manifest.sourceCsvFileName());
        if (resFile.exists() && resFile.length() > 0) {
            return resFile;
        }

        File targetResFile = new File("target/classes/geography", manifest.sourceCsvFileName());
        if (targetResFile.exists() && targetResFile.length() > 0) {
            return targetResFile;
        }

        File file = new File("target/preflight-csvs", manifest.sourceCsvFileName());
        if (file.exists() && file.length() > 0) {
            return file;
        }

        File alt = new File("agri-platform-backend/target/preflight-csvs", manifest.sourceCsvFileName());
        if (alt.exists() && alt.length() > 0) {
            return alt;
        }

        // Check embedded classpath resource stream (for packaged JAR deployment)
        try {
            java.io.InputStream stream = getClass().getResourceAsStream("/geography/" + manifest.sourceCsvFileName());
            if (stream != null) {
                File tempFile = File.createTempFile("geography_" + manifest.stateCode() + "_", ".csv");
                tempFile.deleteOnExit();
                try (stream; java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                    stream.transferTo(fos);
                }
                return tempFile;
            }
        } catch (Exception e) {
            log.warn("Could not extract embedded classpath fallback dataset for state {}: {}", stateCode, e.getMessage());
        }

        throw new IllegalStateException("Approved source CSV file for state " + stateCode +
                " is missing from both library Storage and local archive: " + manifest.sourceCsvFileName());
    }

    /**
     * Downloads an approved CSV dataset from the private Agri_Library Supabase Storage bucket.
     * Enforces bounded streaming (max 15 MB) and schema validation.
     */
    protected File downloadFromPrivateStorage(StateManifest manifest) throws Exception {
        String sanitizedUrl = storageBaseUrl.replaceAll("/+$", "");
        String objectUrl = sanitizedUrl + "/storage/v1/object/authenticated/" + bucketName + "/" + manifest.sourceCsvFileName();

        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(connectTimeoutMs))
                .build();

        java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(objectUrl))
                .timeout(java.time.Duration.ofMillis(readTimeoutMs))
                .header("Authorization", "Bearer " + storageServiceKey)
                .header("apikey", storageServiceKey)
                .header("Accept", "text/csv, application/octet-stream")
                .GET()
                .build();

        File cacheDir = new File("target/storage-cache");
        if (!cacheDir.exists()) cacheDir.mkdirs();
        File targetFile = new File(cacheDir, manifest.sourceCsvFileName());

        java.net.http.HttpResponse<java.io.InputStream> response = client.send(
                request, java.net.http.HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() == 200) {
            try (java.io.InputStream in = response.body();
                 java.io.FileOutputStream out = new java.io.FileOutputStream(targetFile)) {
                byte[] buffer = new byte[8192];
                int read;
                long totalBytes = 0;
                while ((read = in.read(buffer)) != -1) {
                    totalBytes += read;
                    if (totalBytes > maxFileSizeBytes) {
                        targetFile.delete();
                        throw new IllegalStateException("File size exceeded maximum allowed limit of " + maxFileSizeBytes + " bytes");
                    }
                    out.write(buffer, 0, read);
                }
            }

            // Basic schema / header validation
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(targetFile))) {
                String header = reader.readLine();
                if (header == null || !header.toLowerCase().contains("village")) {
                    targetFile.delete();
                    throw new IllegalStateException("Downloaded file failed schema validation: header missing 'village': " + header);
                }
            }
            return targetFile;
        } else {
            throw new java.io.IOException("HTTP " + response.statusCode() + " received from Supabase Storage for object: " + manifest.sourceCsvFileName());
        }
    }

    public boolean isApprovedState(String stateCode) {
        return APPROVED_PILOT_STATES.containsKey(stateCode);
    }

    public StateManifest getManifest(String stateCode) {
        return APPROVED_PILOT_STATES.get(stateCode);
    }

    public Map<String, StateManifest> getAllPilotStates() {
        return Collections.unmodifiableMap(APPROVED_PILOT_STATES);
    }

    public void setStorageBaseUrl(String storageBaseUrl) {
        this.storageBaseUrl = storageBaseUrl;
    }

    public void setStorageServiceKey(String storageServiceKey) {
        this.storageServiceKey = storageServiceKey;
    }
}
