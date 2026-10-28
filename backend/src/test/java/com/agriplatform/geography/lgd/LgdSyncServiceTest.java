package com.agriplatform.geography.lgd;

import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.geography.lgd.client.LgdApiOperations;
import com.agriplatform.geography.lgd.config.LgdProperties;
import com.agriplatform.geography.lgd.dto.*;
import com.agriplatform.geography.lgd.sync.LgdSyncResult;
import com.agriplatform.geography.lgd.sync.LgdSyncService;
import com.agriplatform.geography.repository.DistrictRepository;
import com.agriplatform.geography.repository.MandalRepository;
import com.agriplatform.geography.repository.StateRepository;
import com.agriplatform.geography.repository.VillageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link LgdSyncService}.
 *
 * <p>Uses Mockito to mock all external dependencies.
 * Tests verify: credential guard, idempotency, upsert, deactivation,
 * UUID stability, concurrency guard, partial failure, and missing parent handling.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LgdSyncService — Unit Tests")
class LgdSyncServiceTest {

    @Mock private LgdApiOperations apiClient;
    @Mock private StateRepository stateRepository;
    @Mock private DistrictRepository districtRepository;
    @Mock private MandalRepository mandalRepository;
    @Mock private VillageRepository villageRepository;

    private LgdProperties props;
    private LgdSyncService syncService;

    @BeforeEach
    void setUp() {
        props = new LgdProperties();
        syncService = new LgdSyncService(
                apiClient, props,
                stateRepository, districtRepository,
                mandalRepository, villageRepository);
    }

    // =========================================================================
    // CREDENTIAL GUARD
    // =========================================================================

    @Test
    @DisplayName("Initial sync returns error result when credentials not configured")
    void initialSync_noCredentials_returnsError() {
        // props has no clientId or clientSecret by default
        LgdSyncResult result = syncService.runInitialSync();

        assertThat(result.isOverallSuccess()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("LGD_CLIENT_ID"));
        verifyNoInteractions(apiClient);
    }

    @Test
    @DisplayName("Incremental sync returns error result when credentials not configured")
    void incrementalSync_noCredentials_returnsError() {
        LgdSyncResult result = syncService.runIncrementalSync("01-09-2026", "01-10-2026");

        assertThat(result.isOverallSuccess()).isFalse();
        assertThat(result.getErrors()).anyMatch(e -> e.contains("LGD_CLIENT_ID"));
        verifyNoInteractions(apiClient);
    }

    // =========================================================================
    // EMPTY RESPONSE HANDLING
    // =========================================================================

    @Test
    @DisplayName("Initial sync with empty states list from LGD adds warning and returns")
    void initialSync_emptyStateList_addsWarning() {
        configureCredentials();
        when(apiClient.fetchActiveStates()).thenReturn(Collections.emptyList());

        LgdSyncResult result = syncService.runInitialSync();

        assertThat(result.getWarnings()).anyMatch(w -> w.contains("no active states"));
        verify(apiClient, times(1)).fetchActiveStates();
        verifyNoMoreInteractions(apiClient);
    }

    // =========================================================================
    // STATE UPSERT
    // =========================================================================

    @Test
    @DisplayName("State with no existing record is inserted")
    void upsertState_newState_isInserted() {
        configureCredentials();

        LgdStateDto dto = makeStateDto(28, "Andhra Pradesh", true);
        when(apiClient.fetchActiveStates()).thenReturn(List.of(dto));
        // No hierarchy pages
        when(apiClient.fetchHierarchyPage(28, 1)).thenReturn(Collections.emptyList());
        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.empty());
        when(stateRepository.save(any(State.class))).thenAnswer(inv -> inv.getArgument(0));

        LgdSyncResult result = syncService.runInitialSync();

        ArgumentCaptor<State> captor = ArgumentCaptor.forClass(State.class);
        verify(stateRepository).save(captor.capture());
        State saved = captor.getValue();
        assertThat(saved.getCode()).isEqualTo("28");
        assertThat(saved.getName()).isEqualTo("Andhra Pradesh");
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getId()).isNotNull();
        assertThat(result.getStatesInserted()).isEqualTo(1);
    }

    @Test
    @DisplayName("State already in DB with same data is skipped (idempotent)")
    void upsertState_existing_unchanged_isSkipped() {
        configureCredentials();

        State existing = makeStateEntity("28", "Andhra Pradesh", true);
        LgdStateDto dto = makeStateDto(28, "Andhra Pradesh", true);

        when(apiClient.fetchActiveStates()).thenReturn(List.of(dto));
        when(apiClient.fetchHierarchyPage(28, 1)).thenReturn(Collections.emptyList());
        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(existing));

        LgdSyncResult result = syncService.runInitialSync();

        verify(stateRepository, never()).save(any());
        assertThat(result.getStatesSkipped()).isEqualTo(1);
        assertThat(result.getStatesInserted()).isEqualTo(0);
        assertThat(result.getStatesUpdated()).isEqualTo(0);
    }

    @Test
    @DisplayName("Renamed state: name updated, UUID preserved")
    void upsertState_renamed_nameUpdated_uuidPreserved() {
        configureCredentials();

        UUID originalUuid = UUID.randomUUID();
        State existing = makeStateEntityWithId(originalUuid, "28", "Anantapur", true);
        LgdStateDto dto = makeStateDto(28, "Ananthapuramu", true); // renamed

        when(apiClient.fetchActiveStates()).thenReturn(List.of(dto));
        when(apiClient.fetchHierarchyPage(28, 1)).thenReturn(Collections.emptyList());
        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(existing));
        when(stateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        syncService.runInitialSync();

        ArgumentCaptor<State> captor = ArgumentCaptor.forClass(State.class);
        verify(stateRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(originalUuid); // UUID unchanged
        assertThat(captor.getValue().getName()).isEqualTo("Ananthapuramu");
    }

    @Test
    @DisplayName("Deactivated state: is_active set to false, not deleted")
    void upsertState_deactivated_setsActiveToFalse() {
        configureCredentials();

        State existing = makeStateEntity("28", "Andhra Pradesh", true);
        LgdStateDto dto = makeStateDto(28, "Andhra Pradesh", false); // deactivated

        when(apiClient.fetchActiveStates()).thenReturn(List.of(dto));
        when(apiClient.fetchHierarchyPage(28, 1)).thenReturn(Collections.emptyList());
        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(existing));
        when(stateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LgdSyncResult result = syncService.runInitialSync();

        verify(stateRepository).save(argThat(s -> !s.isActive()));
        verify(stateRepository, never()).delete(any());
        assertThat(result.getStatesDeactivated()).isEqualTo(1);
    }

    // =========================================================================
    // STATE WITH NULL CODE — SKIP
    // =========================================================================

    @Test
    @DisplayName("State with null stateCode is skipped with warning")
    void upsertState_nullStateCode_skipped() {
        configureCredentials();

        LgdStateDto dto = new LgdStateDto();
        dto.setStateNameEnglish("Unknown State");
        // stateCode is null

        when(apiClient.fetchActiveStates()).thenReturn(List.of(dto));

        LgdSyncResult result = syncService.runInitialSync();

        verify(stateRepository, never()).save(any());
        assertThat(result.getStatesSkipped()).isEqualTo(1);
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("null stateCode"));
    }

    // =========================================================================
    // CONCURRENCY GUARD
    // =========================================================================

    @Test
    @DisplayName("Second concurrent sync trigger is rejected with warning")
    void concurrentSync_secondTriggerRejected() throws Exception {
        configureCredentials();

        // Block apiClient call briefly using a slow mock
        when(apiClient.fetchActiveStates()).thenAnswer(inv -> {
            Thread.sleep(200);
            return Collections.emptyList();
        });

        // Start first sync in background
        Thread firstSync = new Thread(() -> syncService.runInitialSync());
        firstSync.start();
        Thread.sleep(50); // let first sync acquire the lock

        // Second trigger should be rejected
        LgdSyncResult second = syncService.runInitialSync();

        firstSync.join(2000);

        assertThat(second.getWarnings()).anyMatch(w -> w.contains("already in progress"));
        assertThat(second.isOverallSuccess()).isTrue(); // not an error — just skipped
    }

    // =========================================================================
    // INCREMENTAL SYNC
    // =========================================================================

    @Test
    @DisplayName("Incremental sync processes updated state correctly")
    void incrementalSync_updatedState_processed() {
        configureCredentials();

        LgdStateDto updatedState = makeStateDto(28, "Andhra Pradesh Renamed", true);
        State existing = makeStateEntity("28", "Andhra Pradesh", true);

        when(apiClient.fetchStatesByDuration("01-09-2026", "01-10-2026"))
                .thenReturn(List.of(updatedState));
        when(stateRepository.findAll()).thenReturn(List.of(existing));
        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(existing));
        when(stateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // No deltas at lower levels
        when(apiClient.fetchDistrictsByStateDuration(eq(28), anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        when(apiClient.fetchSubDistrictsByStateDuration(eq(28), anyString(), anyString()))
                .thenReturn(Collections.emptyList());
        when(apiClient.fetchVillagesByStateDuration(eq(28), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        LgdSyncResult result = syncService.runIncrementalSync("01-09-2026", "01-10-2026");

        assertThat(result.getStatesUpdated()).isEqualTo(1);
        assertThat(result.isOverallSuccess()).isTrue();
    }

    @Test
    @DisplayName("State with non-integer code column is skipped during incremental sync")
    void incrementalSync_nonIntegerCode_skipped() {
        configureCredentials();

        State badState = makeStateEntity("not-a-number", "Bad State", true);
        when(apiClient.fetchStatesByDuration(anyString(), anyString())).thenReturn(Collections.emptyList());
        when(stateRepository.findAll()).thenReturn(List.of(badState));

        LgdSyncResult result = syncService.runIncrementalSync("01-09-2026", "01-10-2026");

        assertThat(result.getWarnings()).anyMatch(w -> w.contains("non-integer code"));
        verify(apiClient, never()).fetchDistrictsByStateDuration(anyInt(), anyString(), anyString());
    }

    // =========================================================================
    // HIERARCHY PAGE PROCESSING (FULL LOAD)
    // =========================================================================

    @Test
    @DisplayName("Hierarchy page with District, Mandal, and Village inserts full hierarchy")
    void initialSync_hierarchyPage_insertsFullHierarchy() {
        configureCredentials();

        UUID stateUuid = UUID.randomUUID();
        State state = makeStateEntityWithId(stateUuid, "28", "Andhra Pradesh", true);

        LgdStateDto stateDto = makeStateDto(28, "Andhra Pradesh", true);
        when(apiClient.fetchActiveStates()).thenReturn(List.of(stateDto));
        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(state));
        when(stateRepository.findById(stateUuid)).thenReturn(Optional.of(state));

        // Page 1: full hierarchy row
        LgdHierarchyRowDto row = new LgdHierarchyRowDto();
        row.setStateCode(28);
        row.setStateNameEnglish("Andhra Pradesh");
        row.setDistrictCode(502);
        row.setDistrictNameEnglish("Ananthapuramu");
        row.setSubdistrictCode(5032);
        row.setSubdistrictNameEnglish("Atmakur");
        row.setVillageCode(597123);
        row.setVillageNameEnglish("Madakasira");

        when(apiClient.fetchHierarchyPage(28, 1)).thenReturn(List.of(row));
        when(apiClient.fetchHierarchyPage(28, 2)).thenReturn(Collections.emptyList()); // termination

        // Repositories
        UUID districtUuid = UUID.randomUUID();
        District district = makeDistrictEntity(districtUuid, "502", "Ananthapuramu", state, true);
        when(districtRepository.findByStateIdAndCodeIgnoreCase(stateUuid, "502")).thenReturn(Optional.empty());
        when(districtRepository.save(any(District.class))).thenReturn(district);
        when(districtRepository.getReferenceById(districtUuid)).thenReturn(district);

        UUID mandalUuid = UUID.randomUUID();
        Mandal mandal = makeMandalEntity(mandalUuid, "5032", "Atmakur", district, true);
        when(mandalRepository.findByDistrictIdAndCodeIgnoreCase(districtUuid, "5032")).thenReturn(Optional.empty());
        when(mandalRepository.save(any(Mandal.class))).thenReturn(mandal);
        when(mandalRepository.getReferenceById(mandalUuid)).thenReturn(mandal);

        when(villageRepository.findByMandalIdAndCodeIgnoreCase(mandalUuid, "597123")).thenReturn(Optional.empty());
        when(villageRepository.save(any(Village.class))).thenAnswer(inv -> inv.getArgument(0));

        LgdSyncResult result = syncService.runInitialSync();

        assertThat(result.isOverallSuccess()).isTrue();
        assertThat(result.getDistrictsInserted()).isEqualTo(1);
        assertThat(result.getMandalsInserted()).isEqualTo(1);
        assertThat(result.getVillagesInserted()).isEqualTo(1);
        assertThat(result.getPagesProcessed()).isEqualTo(2);

        verify(districtRepository).save(any(District.class));
        verify(mandalRepository).save(any(Mandal.class));
        verify(villageRepository).save(any(Village.class));
    }

    // =========================================================================
    // STATE FAILURE ISOLATION
    // =========================================================================

    @Test
    @DisplayName("Partial state failure isolates failed state and allows other states to succeed")
    void initialSync_stateFailureIsolation_partialSuccess() {
        configureCredentials();

        UUID state1Uuid = UUID.randomUUID();
        State state1 = makeStateEntityWithId(state1Uuid, "28", "State One", true);
        LgdStateDto stateDto1 = makeStateDto(28, "State One", true);

        UUID state2Uuid = UUID.randomUUID();
        State state2 = makeStateEntityWithId(state2Uuid, "29", "State Two", true);
        LgdStateDto stateDto2 = makeStateDto(29, "State Two", true);

        when(apiClient.fetchActiveStates()).thenReturn(List.of(stateDto1, stateDto2));
        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.of(state1));
        when(stateRepository.findByCodeIgnoreCase("29")).thenReturn(Optional.of(state2));

        // State 1 hierarchy succeeds (empty page immediately)
        when(apiClient.fetchHierarchyPage(28, 1)).thenReturn(Collections.emptyList());

        // State 2 hierarchy fails with network exception
        when(apiClient.fetchHierarchyPage(29, 1)).thenThrow(new RuntimeException("Connection reset"));

        LgdSyncResult result = syncService.runInitialSync();

        assertThat(result.isOverallSuccess()).isFalse();
        assertThat(result.getStatesFailed()).isEqualTo(1);
        assertThat(result.getFailedStateCodes()).containsExactly("29");
        // State 1 was processed
        assertThat(result.getStatesProcessed()).isEqualTo(2);
    }

    // =========================================================================
    // INCREMENTAL SYNC WITH MISSING PARENT
    // =========================================================================

    @Test
    @DisplayName("Incremental sync skips mandal with warning when parent district is missing")
    void incrementalSync_missingParentDistrict_skippedWithWarning() {
        configureCredentials();

        UUID stateUuid = UUID.randomUUID();
        State state = makeStateEntityWithId(stateUuid, "28", "Andhra Pradesh", true);

        when(apiClient.fetchStatesByDuration(anyString(), anyString())).thenReturn(Collections.emptyList());
        when(stateRepository.findAll()).thenReturn(List.of(state));
        when(apiClient.fetchDistrictsByStateDuration(eq(28), anyString(), anyString())).thenReturn(Collections.emptyList());

        // Mandal has districtCode 999 which does NOT exist in DB
        LgdSubDistrictDto mandalDto = new LgdSubDistrictDto();
        mandalDto.setSubDistrictCode(7001);
        mandalDto.setSubDistrictNameEnglish("Orphan Mandal");
        mandalDto.setDistrictCode(999);
        when(apiClient.fetchSubDistrictsByStateDuration(eq(28), anyString(), anyString()))
                .thenReturn(List.of(mandalDto));

        when(districtRepository.findByStateIdAndCodeIgnoreCase(stateUuid, "999")).thenReturn(Optional.empty());
        when(apiClient.fetchVillagesByStateDuration(eq(28), anyString(), anyString())).thenReturn(Collections.emptyList());

        LgdSyncResult result = syncService.runIncrementalSync("01-09-2026", "01-10-2026");

        assertThat(result.getMandalsSkipped()).isEqualTo(1);
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("Parent district code=999 not found"));
        verify(mandalRepository, never()).save(any());
    }

    @Test
    @DisplayName("Incremental sync skips village with warning when parent mandal is missing")
    void incrementalSync_missingParentMandal_skippedWithWarning() {
        configureCredentials();

        UUID stateUuid = UUID.randomUUID();
        State state = makeStateEntityWithId(stateUuid, "28", "Andhra Pradesh", true);

        when(apiClient.fetchStatesByDuration(anyString(), anyString())).thenReturn(Collections.emptyList());
        when(stateRepository.findAll()).thenReturn(List.of(state));
        when(apiClient.fetchDistrictsByStateDuration(eq(28), anyString(), anyString())).thenReturn(Collections.emptyList());
        when(apiClient.fetchSubDistrictsByStateDuration(eq(28), anyString(), anyString())).thenReturn(Collections.emptyList());

        // Village has subdistrictCode 888 which does NOT exist
        LgdVillageDto villageDto = new LgdVillageDto();
        villageDto.setVillageCode(8001);
        villageDto.setVillageNameEnglish("Orphan Village");
        villageDto.setSubDistrictCode(888);
        when(apiClient.fetchVillagesByStateDuration(eq(28), anyString(), anyString()))
                .thenReturn(List.of(villageDto));

        when(mandalRepository.findByDistrictStateIdAndCodeIgnoreCase(stateUuid, "888")).thenReturn(Optional.empty());

        LgdSyncResult result = syncService.runIncrementalSync("01-09-2026", "01-10-2026");

        assertThat(result.getVillagesSkipped()).isEqualTo(1);
        assertThat(result.getWarnings()).anyMatch(w -> w.contains("Parent mandal code=888 not found"));
        verify(villageRepository, never()).save(any());
    }

    @Test
    @DisplayName("Unicode names in Indian scripts are preserved without corruption")
    void upsertState_unicodePreserved() {
        configureCredentials();

        LgdStateDto dto = makeStateDto(28, "ఆంధ్రప్రదేశ్ / आंध्र प्रदेश", true);
        when(apiClient.fetchActiveStates()).thenReturn(List.of(dto));
        when(apiClient.fetchHierarchyPage(28, 1)).thenReturn(Collections.emptyList());
        when(stateRepository.findByCodeIgnoreCase("28")).thenReturn(Optional.empty());
        when(stateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LgdSyncResult result = syncService.runInitialSync();

        ArgumentCaptor<State> captor = ArgumentCaptor.forClass(State.class);
        verify(stateRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("ఆంధ్రప్రదేశ్ / आंध्र प्रदेश");
        assertThat(result.getStatesInserted()).isEqualTo(1);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private void configureCredentials() {
        props.setClientId("test-client-id");
        props.setClientSecret("test-client-secret");
        props.setRequestDelayMs(0); // no delay in tests
    }

    private LgdStateDto makeStateDto(int code, String name, boolean active) {
        LgdStateDto dto = new LgdStateDto();
        dto.setStateCode(code);
        dto.setStateNameEnglish(name);
        dto.setIsactive(active);
        return dto;
    }

    private State makeStateEntity(String code, String name, boolean active) {
        return makeStateEntityWithId(UUID.randomUUID(), code, name, active);
    }

    private State makeStateEntityWithId(UUID id, String code, String name, boolean active) {
        State s = new State();
        s.setId(id);
        s.setCode(code);
        s.setName(name);
        s.setActive(active);
        s.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        s.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return s;
    }

    private District makeDistrictEntity(UUID id, String code, String name, State state, boolean active) {
        District d = new District();
        d.setId(id);
        d.setCode(code);
        d.setName(name);
        d.setState(state);
        d.setActive(active);
        d.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        d.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return d;
    }

    private Mandal makeMandalEntity(UUID id, String code, String name, District district, boolean active) {
        Mandal m = new Mandal();
        m.setId(id);
        m.setCode(code);
        m.setName(name);
        m.setDistrict(district);
        m.setActive(active);
        m.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        m.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return m;
    }
}
