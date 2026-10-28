package com.agriplatform.geography.lgd.sync;

import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.geography.lgd.client.LgdApiClient;
import com.agriplatform.geography.lgd.client.LgdApiOperations;
import com.agriplatform.geography.lgd.config.LgdProperties;
import com.agriplatform.geography.lgd.dto.*;
import com.agriplatform.geography.repository.DistrictRepository;
import com.agriplatform.geography.repository.MandalRepository;
import com.agriplatform.geography.repository.StateRepository;
import com.agriplatform.geography.repository.VillageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Production LGD synchronization service.
 *
 * <h2>Design Decisions</h2>
 * <ul>
 *   <li><b>LGD Code Policy:</b> LGD integer codes are stored as strings in the existing
 *       {@code code} column. This avoids schema changes while preserving authoritative identity.</li>
 *   <li><b>Identity key:</b> LGD integer code (not name) — names change (renamed districts).</li>
 *   <li><b>UUID stability:</b> Existing UUIDs are NEVER regenerated on update/deactivation.</li>
 *   <li><b>Deactivation:</b> Sets {@code is_active=false}; NEVER deletes rows.</li>
 *   <li><b>Transaction scope:</b> One transaction per state/page — failure in one state
 *       does not corrupt successfully completed states.</li>
 *   <li><b>Concurrency guard:</b> AtomicBoolean prevents overlapping scheduler runs.</li>
 *   <li><b>No global India transaction:</b> ~600k+ villages cannot fit in one TX safely.</li>
 * </ul>
 *
 * <h2>LGD APIs Used</h2>
 * <pre>
 *   Initial load  → POST /states/v1/status/active
 *                 → POST /stateDistrictSubdistBlockVillage/{sc}/page/{page}  (paginated)
 *   Incremental   → POST /states/v1/updatedFrom/{from}/updatedTo/{to}
 *                 → POST /state/{sc}/districts/v1/updatedFrom/{from}/updatedTo/{to}
 *                 → POST /state/{sc}/subdistricts/v2/updatedFrom/{from}/updatedTo/{to}
 *                 → POST /state/{sc}/villages/v2/updatedFrom/{from}/updatedTo/{to}
 * </pre>
 */
@Service
public class LgdSyncService implements LgdSyncOperations {

    private static final Logger log = LoggerFactory.getLogger(LgdSyncService.class);

    /** Guards against overlapping sync executions from scheduler or concurrent admin calls. */
    private final AtomicBoolean syncInProgress = new AtomicBoolean(false);

    private final LgdApiOperations apiClient;
    private final LgdProperties props;
    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final MandalRepository mandalRepository;
    private final VillageRepository villageRepository;
    private final TransactionTemplate transactionTemplate;

    public LgdSyncService(
            LgdApiOperations apiClient,
            LgdProperties props,
            StateRepository stateRepository,
            DistrictRepository districtRepository,
            MandalRepository mandalRepository,
            VillageRepository villageRepository) {
        this(apiClient, props, stateRepository, districtRepository, mandalRepository, villageRepository, null);
    }

    @Autowired
    public LgdSyncService(
            LgdApiOperations apiClient,
            LgdProperties props,
            StateRepository stateRepository,
            DistrictRepository districtRepository,
            MandalRepository mandalRepository,
            VillageRepository villageRepository,
            @Autowired(required = false) PlatformTransactionManager transactionManager) {
        this.apiClient = apiClient;
        this.props = props;
        this.stateRepository = stateRepository;
        this.districtRepository = districtRepository;
        this.mandalRepository = mandalRepository;
        this.villageRepository = villageRepository;
        this.transactionTemplate = transactionManager != null ? new TransactionTemplate(transactionManager) : null;
    }

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Performs a complete initial synchronization of all India geography from LGD.
     *
     * <p>Strategy:
     * <ol>
     *   <li>Fetch all active states (single call — ~36 records).</li>
     *   <li>Upsert all states.</li>
     *   <li>For each state, paginate through the full hierarchy API.</li>
     *   <li>Each state runs in its own @Transactional context.</li>
     *   <li>Partial failure: failed states are recorded; others continue.</li>
     * </ol>
     */
    public LgdSyncResult runInitialSync() {
        LgdSyncResult result = new LgdSyncResult(LgdSyncResult.SyncType.INITIAL);

        if (!props.areCredentialsConfigured()) {
            String msg = "LGD credentials not configured. Set LGD_CLIENT_ID and LGD_CLIENT_SECRET environment variables.";
            log.error("[LGD-SYNC] {}", msg);
            result.addError(msg);
            result.complete();
            return result;
        }

        if (!syncInProgress.compareAndSet(false, true)) {
            String msg = "LGD synchronization already in progress — skipping concurrent trigger.";
            log.warn("[LGD-SYNC] {}", msg);
            result.addWarning(msg);
            result.complete();
            return result;
        }

        log.info("[LGD-SYNC] ========== INITIAL SYNC STARTED ==========");
        try {
            // 1. Fetch and upsert all states
            List<LgdStateDto> states = apiClient.fetchActiveStates();
            log.info("[LGD-SYNC] Fetched {} states from LGD", states.size());

            if (states.isEmpty()) {
                result.addWarning("LGD returned no active states. Sync aborted.");
                return result;
            }

            // Build state code → UUID lookup for FK resolution in child records
            Map<Integer, UUID> stateCodeToUuid = new HashMap<>();

            for (LgdStateDto dto : states) {
                try {
                    UUID stateId = upsertState(dto, result);
                    if (dto.getStateCode() != null) {
                        stateCodeToUuid.put(dto.getStateCode(), stateId);
                    }
                } catch (Exception e) {
                    log.error("[LGD-SYNC] Failed to upsert state code={}: {}", dto.getStateCode(), e.getMessage());
                    result.addFailedStateCode(String.valueOf(dto.getStateCode()));
                }
            }

            // 2. For each state, sync the full hierarchy via paginated API
            for (LgdStateDto stateDto : states) {
                if (stateDto.getStateCode() == null) {
                    result.addWarning("Skipping state with null stateCode: " + stateDto.getStateNameEnglish());
                    continue;
                }
                int stateCode = stateDto.getStateCode();
                UUID stateUuid = stateCodeToUuid.get(stateCode);
                if (stateUuid == null) {
                    result.addWarning("No UUID found for stateCode=" + stateCode + " — skipping hierarchy sync");
                    continue;
                }

                try {
                    syncHierarchyForState(stateCode, stateUuid, result);
                } catch (Exception e) {
                    log.error("[LGD-SYNC] Hierarchy sync failed for stateCode={}: {}", stateCode, e.getMessage(), e);
                    result.addFailedStateCode(String.valueOf(stateCode));
                }
            }

        } catch (LgdApiClient.LgdAuthException e) {
            log.error("[LGD-SYNC] Authentication failure — credentials are invalid or not yet activated: {}", e.getMessage());
            result.addError("LGD authentication failure: " + e.getMessage());
        } catch (Exception e) {
            log.error("[LGD-SYNC] Unexpected error during initial sync", e);
            result.addError("Unexpected sync error: " + e.getMessage());
        } finally {
            syncInProgress.set(false);
            result.complete();
            log.info("[LGD-SYNC] ========== INITIAL SYNC COMPLETE: {} ==========", result);
        }

        return result;
    }

    /**
     * Performs incremental synchronization for a given date range.
     *
     * @param from start date in DD-MM-YYYY format
     * @param to   end date in DD-MM-YYYY format
     */
    public LgdSyncResult runIncrementalSync(String from, String to) {
        LgdSyncResult result = new LgdSyncResult(LgdSyncResult.SyncType.INCREMENTAL);

        if (!props.areCredentialsConfigured()) {
            String msg = "LGD credentials not configured. Set LGD_CLIENT_ID and LGD_CLIENT_SECRET environment variables.";
            log.error("[LGD-SYNC] {}", msg);
            result.addError(msg);
            result.complete();
            return result;
        }

        if (!syncInProgress.compareAndSet(false, true)) {
            String msg = "LGD synchronization already in progress — skipping concurrent trigger.";
            log.warn("[LGD-SYNC] {}", msg);
            result.addWarning(msg);
            result.complete();
            return result;
        }

        log.info("[LGD-SYNC] ========== INCREMENTAL SYNC STARTED: {} → {} ==========", from, to);
        try {
            // 1. Updated states
            List<LgdStateDto> updatedStates = apiClient.fetchStatesByDuration(from, to);
            log.info("[LGD-SYNC] {} states updated in range {} → {}", updatedStates.size(), from, to);
            for (LgdStateDto dto : updatedStates) {
                try {
                    upsertState(dto, result);
                } catch (Exception e) {
                    log.error("[LGD-SYNC] Failed incremental state upsert code={}: {}", dto.getStateCode(), e.getMessage());
                    result.addFailedStateCode(String.valueOf(dto.getStateCode()));
                }
            }

            // 2. For each known state, sync district/mandal/village deltas
            List<State> allStates = stateRepository.findAll();
            for (State state : allStates) {
                String lgdCodeStr = state.getCode();
                if (lgdCodeStr == null || lgdCodeStr.isBlank()) continue;

                int stateCode;
                try {
                    stateCode = Integer.parseInt(lgdCodeStr.trim());
                } catch (NumberFormatException e) {
                    result.addWarning("State id=" + state.getId() + " has non-integer code='" + lgdCodeStr + "' — skipping delta sync");
                    continue;
                }

                try {
                    if (transactionTemplate != null) {
                        transactionTemplate.executeWithoutResult(status ->
                                syncIncrementalForState(stateCode, state, from, to, result));
                    } else {
                        syncIncrementalForState(stateCode, state, from, to, result);
                    }
                } catch (Exception e) {
                    log.error("[LGD-SYNC] Incremental sync failed for stateCode={}: {}", stateCode, e.getMessage(), e);
                    result.addFailedStateCode(String.valueOf(stateCode));
                }
            }

        } catch (LgdApiClient.LgdAuthException e) {
            log.error("[LGD-SYNC] Authentication failure during incremental sync: {}", e.getMessage());
            result.addError("LGD authentication failure: " + e.getMessage());
        } catch (Exception e) {
            log.error("[LGD-SYNC] Unexpected error during incremental sync", e);
            result.addError("Unexpected incremental sync error: " + e.getMessage());
        } finally {
            syncInProgress.set(false);
            result.complete();
            log.info("[LGD-SYNC] ========== INCREMENTAL SYNC COMPLETE: {} ==========", result);
        }

        return result;
    }

    /**
     * Returns true if a sync is currently running.
     */
    public boolean isSyncInProgress() {
        return syncInProgress.get();
    }

    // =========================================================================
    // PRIVATE — HIERARCHY SYNC (per state, paginated)
    // =========================================================================

    /**
     * Paginates through the full hierarchy API for one state.
     * Each page is processed in its own @Transactional boundary.
     * Page termination: empty list returned by LGD (not documented explicitly in spec).
     */
    private void syncHierarchyForState(int stateCode, UUID stateUuid, LgdSyncResult result) {
        log.info("[LGD-SYNC] Syncing hierarchy for stateCode={}", stateCode);
        int page = 1;
        int consecutiveEmpty = 0;

        // Per-state session caches to resolve FKs without repeated DB queries
        Map<Integer, UUID> districtCodeToUuid = new HashMap<>();
        Map<Integer, UUID> mandalCodeToUuid   = new HashMap<>();

        while (true) {
            List<LgdHierarchyRowDto> rows = apiClient.fetchHierarchyPage(stateCode, page);
            result.incrementPagesProcessed();

            if (rows.isEmpty()) {
                // Spec does not define end-condition explicitly.
                // We stop on first empty page (conservative, safe termination).
                consecutiveEmpty++;
                if (consecutiveEmpty >= 1) {
                    log.info("[LGD-SYNC] State {} completed at page {} (empty response = end of data)", stateCode, page);
                    break;
                }
            } else {
                consecutiveEmpty = 0;
                if (transactionTemplate != null) {
                    transactionTemplate.executeWithoutResult(status ->
                            processHierarchyPage(stateCode, stateUuid, rows, districtCodeToUuid, mandalCodeToUuid, result));
                } else {
                    processHierarchyPage(stateCode, stateUuid, rows, districtCodeToUuid, mandalCodeToUuid, result);
                }
            }
            page++;
        }
    }

    @Transactional
    protected void processHierarchyPage(
            int stateCode,
            UUID stateUuid,
            List<LgdHierarchyRowDto> rows,
            Map<Integer, UUID> districtCodeToUuid,
            Map<Integer, UUID> mandalCodeToUuid,
            LgdSyncResult result) {

        State stateRef = stateRepository.findById(stateUuid)
                .orElseThrow(() -> new IllegalStateException("State not found for UUID=" + stateUuid));

        for (LgdHierarchyRowDto row : rows) {
            try {
                processHierarchyRow(row, stateRef, districtCodeToUuid, mandalCodeToUuid, result);
            } catch (Exception e) {
                log.warn("[LGD-SYNC] Skipping row in state={}: {}", stateCode, e.getMessage());
                result.addWarning("Row skipped in state=" + stateCode + ": " + e.getMessage());
            }
        }
    }

    private void processHierarchyRow(
            LgdHierarchyRowDto row,
            State stateRef,
            Map<Integer, UUID> districtCodeToUuid,
            Map<Integer, UUID> mandalCodeToUuid,
            LgdSyncResult result) {

        // District
        Integer distCode = row.getDistrictCode();
        if (distCode != null && row.getDistrictNameEnglish() != null) {
            result.incrementDistrictsProcessed();
            UUID distUuid = districtCodeToUuid.computeIfAbsent(distCode, dc -> {
                District d = upsertDistrictByCode(dc, row.getDistrictNameEnglish(), stateRef, true, result);
                return d.getId();
            });

            // Mandal / Sub-district
            Integer sdCode = row.getSubdistrictCode();
            if (sdCode != null && row.getSubdistrictNameEnglish() != null) {
                result.incrementMandalsProcessed();
                District distRef = districtRepository.getReferenceById(distUuid);
                UUID mandalUuid = mandalCodeToUuid.computeIfAbsent(sdCode, sc -> {
                    Mandal m = upsertMandalByCode(sc, row.getSubdistrictNameEnglish(), distRef, true, result);
                    return m.getId();
                });

                // Village
                Integer vCode = row.getVillageCode();
                if (vCode != null && row.getVillageNameEnglish() != null) {
                    result.incrementVillagesProcessed();
                    Mandal mandalRef = mandalRepository.getReferenceById(mandalUuid);
                    upsertVillageByCode(vCode, row.getVillageNameEnglish(), mandalRef, true, result);
                }
            }
        }
    }

    // =========================================================================
    // PRIVATE — INCREMENTAL SYNC (per state)
    // =========================================================================

    @Transactional
    protected void syncIncrementalForState(
            int stateCode, State state, String from, String to, LgdSyncResult result) {

        // Districts delta
        List<LgdDistrictDto> districts = apiClient.fetchDistrictsByStateDuration(stateCode, from, to);
        for (LgdDistrictDto dto : districts) {
            if (dto.getDistrictCode() == null) continue;
            result.incrementDistrictsProcessed();
            upsertDistrictByCode(dto.getDistrictCode(), dto.getDistrictNameEnglish(),
                    state, dto.isEffectivelyActive(), result);
        }

        // Mandals delta
        List<LgdSubDistrictDto> mandals = apiClient.fetchSubDistrictsByStateDuration(stateCode, from, to);
        for (LgdSubDistrictDto dto : mandals) {
            if (dto.getSubDistrictCode() == null || dto.getDistrictCode() == null) {
                result.addWarning("Skipping mandal with null code or districtCode in stateCode=" + stateCode);
                continue;
            }
            result.incrementMandalsProcessed();
            String distCodeStr = String.valueOf(dto.getDistrictCode());
            Optional<District> distOpt = districtRepository.findByStateIdAndCodeIgnoreCase(state.getId(), distCodeStr);
            if (distOpt.isEmpty()) {
                result.addWarning("Parent district code=" + dto.getDistrictCode()
                        + " not found for mandal=" + dto.getSubDistrictCode() + " — skipping");
                result.incrementMandalsSkipped();
                continue;
            }
            upsertMandalByCode(dto.getSubDistrictCode(), dto.getSubDistrictNameEnglish(),
                    distOpt.get(), dto.isEffectivelyActive(), result);
        }

        // Villages delta
        List<LgdVillageDto> villages = apiClient.fetchVillagesByStateDuration(stateCode, from, to);
        for (LgdVillageDto dto : villages) {
            if (dto.getVillageCode() == null || dto.getSubDistrictCode() == null) {
                result.addWarning("Skipping village with null code or subDistrictCode in stateCode=" + stateCode);
                continue;
            }
            result.incrementVillagesProcessed();
            String mandalCodeStr = String.valueOf(dto.getSubDistrictCode());
            Optional<Mandal> mandalOpt = mandalRepository.findByDistrictStateIdAndCodeIgnoreCase(state.getId(), mandalCodeStr);
            if (mandalOpt.isEmpty()) {
                result.addWarning("Parent mandal code=" + dto.getSubDistrictCode()
                        + " not found for village=" + dto.getVillageCode() + " — skipping");
                result.incrementVillagesSkipped();
                continue;
            }
            upsertVillageByCode(dto.getVillageCode(), dto.getVillageNameEnglish(),
                    mandalOpt.get(), dto.isEffectivelyActive(), result);
        }
    }

    // =========================================================================
    // PRIVATE — UPSERT PRIMITIVES
    // =========================================================================

    /**
     * Upserts a State by LGD stateCode (stored in {@code code} column).
     * Uses code as primary identity key — name changes are applied in-place.
     * UUID is preserved on update.
     */
    @Transactional
    protected UUID upsertState(LgdStateDto dto, LgdSyncResult result) {
        if (dto.getStateCode() == null) {
            result.addWarning("State with null stateCode skipped: " + dto.getStateNameEnglish());
            result.incrementStatesSkipped();
            return null;
        }

        String codeStr = String.valueOf(dto.getStateCode());
        String name = normalize(dto.getStateNameEnglish());
        if (name == null || name.isBlank()) {
            result.addWarning("State code=" + dto.getStateCode() + " has blank name — skipping");
            result.incrementStatesSkipped();
            return null;
        }

        result.incrementStatesProcessed();
        boolean active = dto.isEffectivelyActive();

        Optional<State> existing = stateRepository.findByCodeIgnoreCase(codeStr);
        if (existing.isPresent()) {
            State s = existing.get();
            boolean changed = false;
            if (!Objects.equals(s.getName(), name)) { s.setName(name); changed = true; }
            if (s.isActive() != active) { s.setActive(active); changed = true; }
            if (changed) {
                s.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                stateRepository.save(s);
                if (!active) result.incrementStatesDeactivated();
                else         result.incrementStatesUpdated();
            } else {
                result.incrementStatesSkipped();
            }
            return s.getId();
        }

        State newState = new State();
        newState.setId(UUID.randomUUID());
        newState.setCode(codeStr);
        newState.setName(name);
        newState.setActive(active);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        newState.setCreatedAt(now);
        newState.setUpdatedAt(now);
        stateRepository.save(newState);
        result.incrementStatesInserted();
        return newState.getId();
    }

    private District upsertDistrictByCode(
            int lgdDistrictCode, String nameEnglish,
            State state, boolean active, LgdSyncResult result) {

        String codeStr = String.valueOf(lgdDistrictCode);
        String name = normalize(nameEnglish);
        if (name == null || name.isBlank()) {
            result.addWarning("District code=" + lgdDistrictCode + " has blank name — using code as name");
            name = codeStr;
        }

        Optional<District> existing = districtRepository.findByStateIdAndCodeIgnoreCase(state.getId(), codeStr);
        if (existing.isPresent()) {
            District d = existing.get();
            boolean changed = false;
            if (!Objects.equals(d.getName(), name)) { d.setName(name); changed = true; }
            if (d.isActive() != active)              { d.setActive(active); changed = true; }
            if (changed) {
                d.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                d = districtRepository.save(d);
                if (!active) result.incrementDistrictsDeactivated();
                else         result.incrementDistrictsUpdated();
            } else {
                result.incrementDistrictsSkipped();
            }
            return d;
        }

        District d = new District();
        d.setId(UUID.randomUUID());
        d.setState(state);
        d.setCode(codeStr);
        d.setName(name);
        d.setActive(active);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        d.setCreatedAt(now);
        d.setUpdatedAt(now);
        d = districtRepository.save(d);
        result.incrementDistrictsInserted();
        return d;
    }

    private Mandal upsertMandalByCode(
            int lgdMandalCode, String nameEnglish,
            District district, boolean active, LgdSyncResult result) {

        String codeStr = String.valueOf(lgdMandalCode);
        String name = normalize(nameEnglish);
        if (name == null || name.isBlank()) {
            name = codeStr;
        }

        Optional<Mandal> existing = mandalRepository.findByDistrictIdAndCodeIgnoreCase(district.getId(), codeStr);
        if (existing.isPresent()) {
            Mandal m = existing.get();
            boolean changed = false;
            if (!Objects.equals(m.getName(), name)) { m.setName(name); changed = true; }
            if (m.isActive() != active)              { m.setActive(active); changed = true; }
            if (changed) {
                m.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                m = mandalRepository.save(m);
                if (!active) result.incrementMandalsDeactivated();
                else         result.incrementMandalsUpdated();
            } else {
                result.incrementMandalsSkipped();
            }
            return m;
        }

        Mandal m = new Mandal();
        m.setId(UUID.randomUUID());
        m.setDistrict(district);
        m.setCode(codeStr);
        m.setName(name);
        m.setActive(active);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        m.setCreatedAt(now);
        m.setUpdatedAt(now);
        m = mandalRepository.save(m);
        result.incrementMandalsInserted();
        return m;
    }

    private void upsertVillageByCode(
            int lgdVillageCode, String nameEnglish,
            Mandal mandal, boolean active, LgdSyncResult result) {

        String codeStr = String.valueOf(lgdVillageCode);
        String name = normalize(nameEnglish);
        if (name == null || name.isBlank()) {
            name = codeStr;
        }

        Optional<Village> existing = villageRepository.findByMandalIdAndCodeIgnoreCase(mandal.getId(), codeStr);
        if (existing.isPresent()) {
            Village v = existing.get();
            boolean changed = false;
            if (!Objects.equals(v.getName(), name)) { v.setName(name); changed = true; }
            if (v.isActive() != active)              { v.setActive(active); changed = true; }
            // lat/lon remain NULL — LGD does not provide coordinates
            if (changed) {
                v.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                villageRepository.save(v);
                if (!active) result.incrementVillagesDeactivated();
                else         result.incrementVillagesUpdated();
            } else {
                result.incrementVillagesSkipped();
            }
            return;
        }

        Village v = new Village();
        v.setId(UUID.randomUUID());
        v.setMandal(mandal);
        v.setCode(codeStr);
        v.setName(name);
        v.setActive(active);
        v.setLatitude(null);   // LGD provides no coordinates
        v.setLongitude(null);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        v.setCreatedAt(now);
        v.setUpdatedAt(now);
        villageRepository.save(v);
        result.incrementVillagesInserted();
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    private static String normalize(String s) {
        if (s == null) return null;
        // Trim, collapse multiple spaces, preserve Unicode (LGD local names)
        return s.strip().replaceAll("\\s{2,}", " ");
    }
}
