package com.agriplatform.geography.ingestion.service;

import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.geography.ingestion.model.ImportMode;
import com.agriplatform.geography.ingestion.model.ImportSummary;
import com.agriplatform.geography.ingestion.model.RawGeographyRecord;
import com.agriplatform.geography.ingestion.model.ValidationError;
import com.agriplatform.geography.ingestion.normalizer.GeographyNormalizer;
import com.agriplatform.geography.ingestion.source.GeographySource;
import com.agriplatform.geography.ingestion.validation.GeographyValidator;
import com.agriplatform.geography.repository.DistrictRepository;
import com.agriplatform.geography.repository.MandalRepository;
import com.agriplatform.geography.repository.StateRepository;
import com.agriplatform.geography.repository.VillageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Production-grade ingestion and synchronization engine for Indian geography reference data.
 *
 * <p>Enforces:
 * <ul>
 *   <li>Pre-validation before any database mutation.</li>
 *   <li>Strict hierarchy validation (State → District → Mandal → Village).</li>
 *   <li>Idempotent synchronization: repeated imports of the same dataset produce 0 duplicates and mark records UNCHANGED.</li>
 *   <li>In-place updates: attributes updated without regenerating UUIDs, preserving existing relational keys.</li>
 *   <li>Cache-accelerated resolution: eliminates N+1 queries during bulk import via per-parent prefetching.</li>
 *   <li>Batch persistence with partial-failure fallback: high-throughput JDBC batches with automatic single-row isolation on failure.</li>
 *   <li>Clear separation of DRY_RUN vs IMPORT modes.</li>
 * </ul>
 */
@Service
public class GeographyIngestionServiceImpl implements GeographyIngestionService {

    private static final Logger log = LoggerFactory.getLogger(GeographyIngestionServiceImpl.class);

    private static final int BATCH_SIZE = 500;

    private static final BigDecimal LAT_MIN = new BigDecimal("-90.000000");
    private static final BigDecimal LAT_MAX = new BigDecimal("90.000000");
    private static final BigDecimal LON_MIN = new BigDecimal("-180.000000");
    private static final BigDecimal LON_MAX = new BigDecimal("180.000000");

    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final MandalRepository mandalRepository;
    private final VillageRepository villageRepository;
    private final JdbcTemplate jdbcTemplate;

    public GeographyIngestionServiceImpl(
            StateRepository stateRepository,
            DistrictRepository districtRepository,
            MandalRepository mandalRepository,
            VillageRepository villageRepository) {
        this(stateRepository, districtRepository, mandalRepository, villageRepository, null);
    }

    @Autowired
    public GeographyIngestionServiceImpl(
            StateRepository stateRepository,
            DistrictRepository districtRepository,
            MandalRepository mandalRepository,
            VillageRepository villageRepository,
            @Autowired(required = false) JdbcTemplate jdbcTemplate) {
        this.stateRepository = stateRepository;
        this.districtRepository = districtRepository;
        this.mandalRepository = mandalRepository;
        this.villageRepository = villageRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Ingests geography data from the given source.
     *
     * <p><strong>Transaction strategy:</strong> This method intentionally has NO outer
     * {@code @Transactional} wrapper. A single transaction spanning thousands of rows
     * against a remote Supabase session-pooler connection causes the JDBC connection
     * to be held for tens of minutes, exceeding the HikariCP {@code max-lifetime} and
     * causing rollback errors on recycling.
     *
     * <p>Instead, reads are pre-fetched per parent (district/mandal), and writes are executed
     * in bounded batches of {@value #BATCH_SIZE}. If any batch fails, it falls back to
     * single-row commits so that only the failing rows are rejected, preserving partial-failure
     * recovery and restartability.
     */
    @Override
    public ImportSummary ingest(GeographySource source, ImportMode mode) {
        long startTime = System.currentTimeMillis();
        ImportSummary summary = new ImportSummary();
        summary.setMode(mode);
        summary.setSourceName(source.getSourceName());

        log.info("Starting geography ingestion: source='{}', mode={}", source.getSourceName(), mode);

        GeographyValidator validator = new GeographyValidator();
        validator.reset();

        // Session caches to prevent N+1 parent queries
        Map<String, State> stateCache = new HashMap<>();
        Map<String, District> districtCache = new HashMap<>();
        Map<String, Mandal> mandalCache = new HashMap<>();
        Map<String, Village> villageCache = new HashMap<>();

        Set<UUID> prefetchedDistricts = new HashSet<>();
        Set<UUID> prefetchedMandals = new HashSet<>();
        Set<UUID> processedMandalIds = new HashSet<>();

        List<Mandal> pendingMandalInserts = new ArrayList<>(BATCH_SIZE);
        List<Mandal> pendingMandalUpdates = new ArrayList<>(BATCH_SIZE);
        List<Village> pendingVillageInserts = new ArrayList<>(BATCH_SIZE);
        List<Village> pendingVillageUpdates = new ArrayList<>(BATCH_SIZE);

        try {
            Iterable<RawGeographyRecord> records = source.readRecords();

            for (RawGeographyRecord raw : records) {
                summary.incrementTotalRowsRead();

                // 1. Validation
                List<ValidationError> errors = validator.validateRecord(raw);
                boolean duplicate = validator.checkAndTrackDuplicates(raw, errors);

                if (!errors.isEmpty()) {
                    summary.incrementInvalidRowsCount();
                    for (ValidationError error : errors) {
                        summary.addError(error);
                    }
                    if (duplicate) {
                        summary.incrementDuplicateRowsCount();
                    }
                    continue; // Skip invalid row from processing
                }

                summary.incrementValidRowsCount();

                // 2. Normalization
                String normStateName = GeographyNormalizer.normalizeText(raw.getStateName());
                String normStateCode = GeographyNormalizer.normalizeCode(raw.getStateCode());
                boolean isActive = raw.getIsActive() == null || raw.getIsActive();

                // 3. Execution (Dry-run or Import)
                if (mode == ImportMode.DRY_RUN) {
                    simulateRecord(raw, normStateName, normStateCode, isActive, summary,
                            stateCache, districtCache, mandalCache, villageCache,
                            prefetchedDistricts, prefetchedMandals, processedMandalIds);
                } else {
                    processRecord(raw, normStateName, normStateCode, isActive, summary,
                            stateCache, districtCache, mandalCache, villageCache,
                            prefetchedDistricts, prefetchedMandals, processedMandalIds,
                            pendingMandalInserts, pendingMandalUpdates,
                            pendingVillageInserts, pendingVillageUpdates);
                }
            }

            // Flush remaining pending batches in IMPORT mode
            if (mode == ImportMode.IMPORT) {
                flushPendingMandalInserts(pendingMandalInserts, summary);
                flushPendingMandalUpdates(pendingMandalUpdates, summary);
                flushPendingVillageInserts(pendingVillageInserts, summary);
                flushPendingVillageUpdates(pendingVillageUpdates, summary);
            }

        } catch (Exception e) {
            log.error("Fatal error during geography ingestion from source '{}'", source.getSourceName(), e);
            summary.addError(new ValidationError(0, "SYSTEM", "source", source.getSourceName(), "Fatal read/parse error: " + e.getMessage()));
            summary.setSuccessful(false);
            if (mode == ImportMode.IMPORT) {
                try {
                    flushPendingMandalInserts(pendingMandalInserts, summary);
                    flushPendingMandalUpdates(pendingMandalUpdates, summary);
                    flushPendingVillageInserts(pendingVillageInserts, summary);
                    flushPendingVillageUpdates(pendingVillageUpdates, summary);
                } catch (Exception ignored) {}
            }
        } finally {
            try {
                source.close();
            } catch (Exception ignored) {}
        }

        long duration = System.currentTimeMillis() - startTime;
        summary.setDurationMs(duration);

        log.info("Completed geography ingestion: mode={}, duration={}ms, read={}, valid={}, invalid={}, duplicate={}, inserted={}, updated={}, unchanged={}",
                mode, duration, summary.getTotalRowsRead(), summary.getValidRowsCount(),
                summary.getInvalidRowsCount(), summary.getDuplicateRowsCount(),
                summary.getTotalInserted(), summary.getTotalUpdated(), summary.getTotalUnchanged());

        return summary;
    }

    private void simulateRecord(
            RawGeographyRecord raw,
            String normStateName,
            String normStateCode,
            boolean isActive,
            ImportSummary summary,
            Map<String, State> stateCache,
            Map<String, District> districtCache,
            Map<String, Mandal> mandalCache,
            Map<String, Village> villageCache,
            Set<UUID> prefetchedDistricts,
            Set<UUID> prefetchedMandals,
            Set<UUID> processedMandalIds) {

        // State simulation
        State state = resolveOrCreateState(normStateName, normStateCode, isActive, summary, stateCache, false);

        if (raw.hasDistrict()) {
            String normDistrictName = GeographyNormalizer.normalizeText(raw.getDistrictName());
            String normDistrictCode = GeographyNormalizer.normalizeCode(raw.getDistrictCode());
            District district = resolveOrCreateDistrict(state, normDistrictName, normDistrictCode, isActive, summary, districtCache, false);

            if (raw.hasMandal()) {
                if (prefetchedDistricts.add(district.getId())) {
                    prefetchMandalsForDistrict(district.getId(), mandalCache);
                }

                String normMandalName = GeographyNormalizer.normalizeText(raw.getMandalName());
                String normMandalCode = GeographyNormalizer.normalizeCode(raw.getMandalCode());
                Mandal mandal = resolveOrCreateMandal(district, normMandalName, normMandalCode, isActive, summary,
                        mandalCache, processedMandalIds, null, null, false);

                if (raw.hasVillage()) {
                    if (prefetchedMandals.add(mandal.getId())) {
                        prefetchVillagesForMandal(mandal.getId(), villageCache);
                    }

                    String normVillageName = GeographyNormalizer.normalizeText(raw.getVillageName());
                    String normVillageCode = GeographyNormalizer.normalizeCode(raw.getVillageCode());
                    BigDecimal lat = GeographyNormalizer.parseCoordinate(raw.getLatitude(), LAT_MIN, LAT_MAX, "latitude");
                    BigDecimal lon = GeographyNormalizer.parseCoordinate(raw.getLongitude(), LON_MIN, LON_MAX, "longitude");
                    simulateVillage(mandal, normVillageName, normVillageCode, lat, lon, isActive, summary, villageCache);
                }
            }
        }
    }

    private void processRecord(
            RawGeographyRecord raw,
            String normStateName,
            String normStateCode,
            boolean isActive,
            ImportSummary summary,
            Map<String, State> stateCache,
            Map<String, District> districtCache,
            Map<String, Mandal> mandalCache,
            Map<String, Village> villageCache,
            Set<UUID> prefetchedDistricts,
            Set<UUID> prefetchedMandals,
            Set<UUID> processedMandalIds,
            List<Mandal> pendingMandalInserts,
            List<Mandal> pendingMandalUpdates,
            List<Village> pendingVillageInserts,
            List<Village> pendingVillageUpdates) {

        // State upsert
        State state = resolveOrCreateState(normStateName, normStateCode, isActive, summary, stateCache, true);

        if (raw.hasDistrict()) {
            String normDistrictName = GeographyNormalizer.normalizeText(raw.getDistrictName());
            String normDistrictCode = GeographyNormalizer.normalizeCode(raw.getDistrictCode());
            District district = resolveOrCreateDistrict(state, normDistrictName, normDistrictCode, isActive, summary, districtCache, true);

            if (raw.hasMandal()) {
                if (prefetchedDistricts.add(district.getId())) {
                    prefetchMandalsForDistrict(district.getId(), mandalCache);
                }

                String normMandalName = GeographyNormalizer.normalizeText(raw.getMandalName());
                String normMandalCode = GeographyNormalizer.normalizeCode(raw.getMandalCode());
                Mandal mandal = resolveOrCreateMandal(district, normMandalName, normMandalCode, isActive, summary,
                        mandalCache, processedMandalIds, pendingMandalInserts, pendingMandalUpdates, true);

                if (raw.hasVillage()) {
                    if (prefetchedMandals.add(mandal.getId())) {
                        prefetchVillagesForMandal(mandal.getId(), villageCache);
                    }

                    String normVillageName = GeographyNormalizer.normalizeText(raw.getVillageName());
                    String normVillageCode = GeographyNormalizer.normalizeCode(raw.getVillageCode());
                    BigDecimal lat = GeographyNormalizer.parseCoordinate(raw.getLatitude(), LAT_MIN, LAT_MAX, "latitude");
                    BigDecimal lon = GeographyNormalizer.parseCoordinate(raw.getLongitude(), LON_MIN, LON_MAX, "longitude");
                    upsertVillage(mandal, normVillageName, normVillageCode, lat, lon, isActive, summary,
                            villageCache, pendingMandalInserts, pendingVillageInserts, pendingVillageUpdates);
                }
            }
        }
    }

    private void prefetchMandalsForDistrict(UUID districtId, Map<String, Mandal> mandalCache) {
        try {
            List<Mandal> existing = mandalRepository.findAllByDistrictId(districtId);
            if (existing != null) {
                for (Mandal m : existing) {
                    if (m.getCode() != null && !m.getCode().isBlank()) {
                        mandalCache.put(districtId + "::" + m.getCode().trim().toLowerCase(), m);
                        mandalCache.put(districtId + "::" + m.getCode().trim(), m);
                    }
                    if (m.getName() != null && !m.getName().isBlank()) {
                        mandalCache.put(districtId + "::" + m.getName().trim().toLowerCase(), m);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Mandal prefetch for district {} skipped: {}", districtId, ex.getMessage());
        }
    }

    private void prefetchVillagesForMandal(UUID mandalId, Map<String, Village> villageCache) {
        try {
            List<Village> existing = villageRepository.findAllByMandalId(mandalId);
            if (existing != null) {
                for (Village v : existing) {
                    if (v.getCode() != null && !v.getCode().isBlank()) {
                        villageCache.put(mandalId + "::" + v.getCode().trim().toLowerCase(), v);
                        villageCache.put(mandalId + "::" + v.getCode().trim(), v);
                    }
                    if (v.getName() != null && !v.getName().isBlank()) {
                        villageCache.put(mandalId + "::" + v.getName().trim().toLowerCase(), v);
                    }
                }
            }
        } catch (Exception ex) {
            log.debug("Village prefetch for mandal {} skipped: {}", mandalId, ex.getMessage());
        }
    }

    private State resolveOrCreateState(
            String name,
            String code,
            boolean isActive,
            ImportSummary summary,
            Map<String, State> cache,
            boolean persist) {

        String key = code != null ? code : name.toLowerCase();
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        Optional<State> existing = Optional.empty();
        if (code != null) {
            existing = stateRepository.findByCodeIgnoreCase(code);
        }
        if (existing.isEmpty() && name != null) {
            existing = stateRepository.findByNameIgnoreCase(name);
        }

        if (existing.isPresent()) {
            State s = existing.get();
            boolean changed = false;
            if (!Objects.equals(s.getName(), name)) {
                s.setName(name);
                changed = true;
            }
            if (code != null && !Objects.equals(s.getCode(), code)) {
                s.setCode(code);
                changed = true;
            }
            if (s.isActive() != isActive) {
                s.setActive(isActive);
                changed = true;
            }

            if (changed) {
                s.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                if (persist) {
                    s = stateRepository.save(s);
                }
                summary.incrementStatesUpdated();
            } else {
                summary.incrementStatesUnchanged();
            }
            cache.put(key, s);
            return s;
        }

        // New State
        State newState = new State();
        newState.setId(UUID.randomUUID());
        newState.setName(name);
        newState.setCode(code);
        newState.setActive(isActive);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        newState.setCreatedAt(now);
        newState.setUpdatedAt(now);

        if (persist) {
            newState = stateRepository.save(newState);
        }
        summary.incrementStatesInserted();
        cache.put(key, newState);
        return newState;
    }

    private District resolveOrCreateDistrict(
            State state,
            String name,
            String code,
            boolean isActive,
            ImportSummary summary,
            Map<String, District> cache,
            boolean persist) {

        String key = state.getId() + "::" + (code != null ? code : name.toLowerCase());
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        Optional<District> existing = Optional.empty();
        if (code != null) {
            existing = districtRepository.findByStateIdAndCodeIgnoreCase(state.getId(), code);
        }
        if (existing.isEmpty()) {
            existing = districtRepository.findByStateIdAndNameIgnoreCase(state.getId(), name);
        }

        if (existing.isPresent()) {
            District d = existing.get();
            boolean changed = false;
            if (!Objects.equals(d.getName(), name)) {
                d.setName(name);
                changed = true;
            }
            if (code != null && !Objects.equals(d.getCode(), code)) {
                d.setCode(code);
                changed = true;
            }
            if (d.isActive() != isActive) {
                d.setActive(isActive);
                changed = true;
            }

            if (changed) {
                d.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                if (persist) {
                    d = districtRepository.save(d);
                }
                summary.incrementDistrictsUpdated();
            } else {
                summary.incrementDistrictsUnchanged();
            }
            cache.put(key, d);
            return d;
        }

        // New District
        District newDistrict = new District();
        newDistrict.setId(UUID.randomUUID());
        newDistrict.setState(state);
        newDistrict.setName(name);
        newDistrict.setCode(code);
        newDistrict.setActive(isActive);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        newDistrict.setCreatedAt(now);
        newDistrict.setUpdatedAt(now);

        if (persist) {
            newDistrict = districtRepository.save(newDistrict);
        }
        summary.incrementDistrictsInserted();
        cache.put(key, newDistrict);
        return newDistrict;
    }

    private Mandal resolveOrCreateMandal(
            District district,
            String name,
            String code,
            boolean isActive,
            ImportSummary summary,
            Map<String, Mandal> cache,
            Set<UUID> processedMandalIds,
            List<Mandal> pendingMandalInserts,
            List<Mandal> pendingMandalUpdates,
            boolean persist) {

        String key = district.getId() + "::" + (code != null ? code : name.toLowerCase());
        Mandal m = cache.get(key);

        if (m != null) {
            if (processedMandalIds.add(m.getId())) {
                boolean changed = false;
                if (!Objects.equals(m.getName(), name)) {
                    m.setName(name);
                    changed = true;
                }
                if (code != null && !Objects.equals(m.getCode(), code)) {
                    m.setCode(code);
                    changed = true;
                }
                if (m.isActive() != isActive) {
                    m.setActive(isActive);
                    changed = true;
                }

                if (changed) {
                    m.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                    if (persist) {
                        if (jdbcTemplate != null && pendingMandalUpdates != null) {
                            pendingMandalUpdates.add(m);
                            if (pendingMandalUpdates.size() >= BATCH_SIZE) {
                                flushPendingMandalUpdates(pendingMandalUpdates, summary);
                            }
                        } else {
                            Mandal saved = mandalRepository.save(m);
                            if (saved != null) {
                                m = saved;
                            }
                        }
                    }
                    summary.incrementMandalsUpdated();
                } else {
                    summary.incrementMandalsUnchanged();
                }
            }
            return m;
        }

        Optional<Mandal> existing = Optional.empty();
        if (code != null) {
            existing = mandalRepository.findByDistrictIdAndCodeIgnoreCase(district.getId(), code);
        }
        if (existing.isEmpty() && name != null) {
            existing = mandalRepository.findByDistrictIdAndNameIgnoreCase(district.getId(), name);
        }

        if (existing.isPresent()) {
            m = existing.get();
            processedMandalIds.add(m.getId());
            boolean changed = false;
            if (!Objects.equals(m.getName(), name)) {
                m.setName(name);
                changed = true;
            }
            if (code != null && !Objects.equals(m.getCode(), code)) {
                m.setCode(code);
                changed = true;
            }
            if (m.isActive() != isActive) {
                m.setActive(isActive);
                changed = true;
            }

            if (changed) {
                m.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                if (persist) {
                    if (jdbcTemplate != null && pendingMandalUpdates != null) {
                        pendingMandalUpdates.add(m);
                        if (pendingMandalUpdates.size() >= BATCH_SIZE) {
                            flushPendingMandalUpdates(pendingMandalUpdates, summary);
                        }
                    } else {
                        Mandal saved = mandalRepository.save(m);
                        if (saved != null) {
                            m = saved;
                        }
                    }
                }
                summary.incrementMandalsUpdated();
            } else {
                summary.incrementMandalsUnchanged();
            }
            cache.put(key, m);
            return m;
        }

        // New Mandal
        Mandal newMandal = new Mandal();
        newMandal.setId(UUID.randomUUID());
        newMandal.setDistrict(district);
        newMandal.setName(name);
        newMandal.setCode(code);
        newMandal.setActive(isActive);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        newMandal.setCreatedAt(now);
        newMandal.setUpdatedAt(now);

        if (persist) {
            if (jdbcTemplate != null && pendingMandalInserts != null) {
                pendingMandalInserts.add(newMandal);
                if (pendingMandalInserts.size() >= BATCH_SIZE) {
                    flushPendingMandalInserts(pendingMandalInserts, summary);
                }
            } else {
                Mandal saved = mandalRepository.save(newMandal);
                if (saved != null) {
                    newMandal = saved;
                }
            }
        }
        summary.incrementMandalsInserted();
        cache.put(key, newMandal);
        processedMandalIds.add(newMandal.getId());
        return newMandal;
    }

    private void simulateVillage(
            Mandal mandal,
            String name,
            String code,
            BigDecimal lat,
            BigDecimal lon,
            boolean isActive,
            ImportSummary summary,
            Map<String, Village> cache) {

        String key = mandal.getId() + "::" + (code != null && !code.isBlank() ? code : (name != null ? name.toLowerCase() : ""));
        Optional<Village> existing = Optional.empty();

        if (cache.containsKey(key)) {
            existing = Optional.of(cache.get(key));
        } else if (jdbcTemplate == null) {
            // Fallback for mock unit tests without JdbcTemplate
            if (code != null && !code.isBlank()) {
                existing = villageRepository.findByMandalIdAndCodeIgnoreCase(mandal.getId(), code);
            }
            if (existing.isEmpty() && name != null) {
                existing = villageRepository.findByMandalIdAndNameIgnoreCase(mandal.getId(), name);
            }
        }

        if (existing.isPresent()) {
            Village v = existing.get();
            boolean changed = !Objects.equals(v.getName(), name)
                    || (code != null && !Objects.equals(v.getCode(), code))
                    || v.isActive() != isActive
                    || (lat != null && (v.getLatitude() == null || v.getLatitude().compareTo(lat) != 0))
                    || (lon != null && (v.getLongitude() == null || v.getLongitude().compareTo(lon) != 0));

            if (changed) {
                summary.incrementVillagesUpdated();
            } else {
                summary.incrementVillagesUnchanged();
            }
            cache.put(key, v);
        } else {
            summary.incrementVillagesInserted();
            Village dummy = new Village();
            dummy.setId(UUID.randomUUID());
            dummy.setMandal(mandal);
            dummy.setName(name);
            dummy.setCode(code);
            dummy.setActive(isActive);
            cache.put(key, dummy);
        }
    }

    private void upsertVillage(
            Mandal mandal,
            String name,
            String code,
            BigDecimal lat,
            BigDecimal lon,
            boolean isActive,
            ImportSummary summary,
            Map<String, Village> cache,
            List<Mandal> pendingMandalInserts,
            List<Village> pendingInserts,
            List<Village> pendingUpdates) {

        String key = mandal.getId() + "::" + (code != null && !code.isBlank() ? code : (name != null ? name.toLowerCase() : ""));
        Optional<Village> existing = Optional.empty();

        if (cache.containsKey(key)) {
            existing = Optional.of(cache.get(key));
        } else if (jdbcTemplate == null) {
            // Fallback for mock unit tests without JdbcTemplate
            if (code != null && !code.isBlank()) {
                existing = villageRepository.findByMandalIdAndCodeIgnoreCase(mandal.getId(), code);
            }
            if (existing.isEmpty() && name != null) {
                existing = villageRepository.findByMandalIdAndNameIgnoreCase(mandal.getId(), name);
            }
        }

        if (existing.isPresent()) {
            Village v = existing.get();
            boolean changed = false;

            if (!Objects.equals(v.getName(), name)) {
                v.setName(name);
                changed = true;
            }
            if (code != null && !Objects.equals(v.getCode(), code)) {
                v.setCode(code);
                changed = true;
            }
            if (v.isActive() != isActive) {
                v.setActive(isActive);
                changed = true;
            }
            if (lat != null && (v.getLatitude() == null || v.getLatitude().compareTo(lat) != 0)) {
                v.setLatitude(lat);
                changed = true;
            }
            if (lon != null && (v.getLongitude() == null || v.getLongitude().compareTo(lon) != 0)) {
                v.setLongitude(lon);
                changed = true;
            }

            if (changed) {
                v.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
                pendingUpdates.add(v);
                summary.incrementVillagesUpdated();
                if (pendingUpdates.size() >= BATCH_SIZE) {
                    flushPendingVillageUpdates(pendingUpdates, summary);
                }
            } else {
                summary.incrementVillagesUnchanged();
            }
            cache.put(key, v);
            return;
        }

        // New Village
        Village newVillage = new Village();
        newVillage.setId(UUID.randomUUID());
        newVillage.setMandal(mandal);
        newVillage.setName(name);
        newVillage.setCode(code);
        newVillage.setLatitude(lat);
        newVillage.setLongitude(lon);
        newVillage.setActive(isActive);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        newVillage.setCreatedAt(now);
        newVillage.setUpdatedAt(now);

        pendingInserts.add(newVillage);
        summary.incrementVillagesInserted();
        cache.put(key, newVillage);

        if (pendingInserts.size() >= BATCH_SIZE) {
            if (pendingMandalInserts != null) {
                flushPendingMandalInserts(pendingMandalInserts, summary);
            }
            flushPendingVillageInserts(pendingInserts, summary);
        }
    }

    private void flushPendingVillageInserts(List<Village> batch, ImportSummary summary) {
        if (batch.isEmpty()) {
            return;
        }
        try {
            if (jdbcTemplate != null) {
                String sql = "INSERT INTO public.villages (id, mandal_id, name, code, latitude, longitude, is_active, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Village v = batch.get(i);
                        ps.setObject(1, v.getId());
                        ps.setObject(2, v.getMandal().getId());
                        ps.setString(3, v.getName());
                        ps.setString(4, v.getCode());
                        ps.setBigDecimal(5, v.getLatitude());
                        ps.setBigDecimal(6, v.getLongitude());
                        ps.setBoolean(7, v.isActive());
                        ps.setObject(8, v.getCreatedAt());
                        ps.setObject(9, v.getUpdatedAt());
                    }

                    @Override
                    public int getBatchSize() {
                        return batch.size();
                    }
                });
            } else {
                for (Village v : batch) {
                    villageRepository.save(v);
                }
            }
        } catch (Exception ex) {
            log.warn("Batch insert failed ({}), falling back to individual inserts for partial-failure recovery", ex.getMessage());
            for (Village v : batch) {
                try {
                    if (jdbcTemplate != null) {
                        String sql = "INSERT INTO public.villages (id, mandal_id, name, code, latitude, longitude, is_active, created_at, updated_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                        jdbcTemplate.update(sql, v.getId(), v.getMandal().getId(), v.getName(), v.getCode(),
                                v.getLatitude(), v.getLongitude(), v.isActive(), v.getCreatedAt(), v.getUpdatedAt());
                    } else {
                        villageRepository.save(v);
                    }
                } catch (Exception rowEx) {
                    log.error("Failed to insert village {}: {}", v.getName(), rowEx.getMessage());
                    summary.incrementInvalidRowsCount();
                    summary.decrementVillagesInserted();
                    summary.addError(new ValidationError(0, "DATABASE", "village", v.getCode(), "Database insert error: " + rowEx.getMessage()));
                }
            }
        } finally {
            batch.clear();
        }
    }

    private void flushPendingVillageUpdates(List<Village> batch, ImportSummary summary) {
        if (batch.isEmpty()) {
            return;
        }
        try {
            if (jdbcTemplate != null) {
                String sql = "UPDATE public.villages SET name = ?, code = ?, latitude = ?, longitude = ?, is_active = ?, updated_at = ? WHERE id = ?";
                jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Village v = batch.get(i);
                        ps.setString(1, v.getName());
                        ps.setString(2, v.getCode());
                        ps.setBigDecimal(3, v.getLatitude());
                        ps.setBigDecimal(4, v.getLongitude());
                        ps.setBoolean(5, v.isActive());
                        ps.setObject(6, v.getUpdatedAt());
                        ps.setObject(7, v.getId());
                    }

                    @Override
                    public int getBatchSize() {
                        return batch.size();
                    }
                });
            } else {
                for (Village v : batch) {
                    villageRepository.save(v);
                }
            }
        } catch (Exception ex) {
            log.warn("Batch update failed ({}), falling back to individual updates for partial-failure recovery", ex.getMessage());
            for (Village v : batch) {
                try {
                    if (jdbcTemplate != null) {
                        String sql = "UPDATE public.villages SET name = ?, code = ?, latitude = ?, longitude = ?, is_active = ?, updated_at = ? WHERE id = ?";
                        jdbcTemplate.update(sql, v.getName(), v.getCode(), v.getLatitude(), v.getLongitude(),
                                v.isActive(), v.getUpdatedAt(), v.getId());
                    } else {
                        villageRepository.save(v);
                    }
                } catch (Exception rowEx) {
                    log.error("Failed to update village {}: {}", v.getName(), rowEx.getMessage());
                    summary.incrementInvalidRowsCount();
                    summary.decrementVillagesUpdated();
                    summary.addError(new ValidationError(0, "DATABASE", "village", v.getCode(), "Database update error: " + rowEx.getMessage()));
                }
            }
        } finally {
            batch.clear();
        }
    }

    private void flushPendingMandalInserts(List<Mandal> batch, ImportSummary summary) {
        if (batch.isEmpty()) {
            return;
        }
        try {
            if (jdbcTemplate != null) {
                String sql = "INSERT INTO public.mandals (id, district_id, name, code, is_active, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)";
                jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Mandal m = batch.get(i);
                        ps.setObject(1, m.getId());
                        ps.setObject(2, m.getDistrict().getId());
                        ps.setString(3, m.getName());
                        ps.setString(4, m.getCode());
                        ps.setBoolean(5, m.isActive());
                        ps.setObject(6, m.getCreatedAt());
                        ps.setObject(7, m.getUpdatedAt());
                    }

                    @Override
                    public int getBatchSize() {
                        return batch.size();
                    }
                });
            } else {
                for (Mandal m : batch) {
                    mandalRepository.save(m);
                }
            }
        } catch (Exception ex) {
            log.warn("Batch insert of mandals failed ({}), falling back to individual inserts for partial-failure recovery", ex.getMessage());
            for (Mandal m : batch) {
                try {
                    if (jdbcTemplate != null) {
                        String sql = "INSERT INTO public.mandals (id, district_id, name, code, is_active, created_at, updated_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
                        jdbcTemplate.update(sql, m.getId(), m.getDistrict().getId(), m.getName(), m.getCode(),
                                m.isActive(), m.getCreatedAt(), m.getUpdatedAt());
                    } else {
                        mandalRepository.save(m);
                    }
                } catch (Exception rowEx) {
                    log.error("Failed to insert mandal {}: {}", m.getName(), rowEx.getMessage());
                    summary.incrementInvalidRowsCount();
                    summary.decrementMandalsInserted();
                    summary.addError(new ValidationError(0, "DATABASE", "mandal", m.getCode(), "Database insert error: " + rowEx.getMessage()));
                }
            }
        } finally {
            batch.clear();
        }
    }

    private void flushPendingMandalUpdates(List<Mandal> batch, ImportSummary summary) {
        if (batch.isEmpty()) {
            return;
        }
        try {
            if (jdbcTemplate != null) {
                String sql = "UPDATE public.mandals SET name = ?, code = ?, is_active = ?, updated_at = ? WHERE id = ?";
                jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        Mandal m = batch.get(i);
                        ps.setString(1, m.getName());
                        ps.setString(2, m.getCode());
                        ps.setBoolean(3, m.isActive());
                        ps.setObject(4, m.getUpdatedAt());
                        ps.setObject(5, m.getId());
                    }

                    @Override
                    public int getBatchSize() {
                        return batch.size();
                    }
                });
            } else {
                for (Mandal m : batch) {
                    mandalRepository.save(m);
                }
            }
        } catch (Exception ex) {
            log.warn("Batch update of mandals failed ({}), falling back to individual updates for partial-failure recovery", ex.getMessage());
            for (Mandal m : batch) {
                try {
                    if (jdbcTemplate != null) {
                        String sql = "UPDATE public.mandals SET name = ?, code = ?, is_active = ?, updated_at = ? WHERE id = ?";
                        jdbcTemplate.update(sql, m.getName(), m.getCode(), m.isActive(), m.getUpdatedAt(), m.getId());
                    } else {
                        mandalRepository.save(m);
                    }
                } catch (Exception rowEx) {
                    log.error("Failed to update mandal {}: {}", m.getName(), rowEx.getMessage());
                    summary.incrementInvalidRowsCount();
                    summary.decrementMandalsUpdated();
                    summary.addError(new ValidationError(0, "DATABASE", "mandal", m.getCode(), "Database update error: " + rowEx.getMessage()));
                }
            }
        } finally {
            batch.clear();
        }
    }
}
