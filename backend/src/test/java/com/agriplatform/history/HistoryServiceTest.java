package com.agriplatform.history;

import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.common.util.SecurityUtils;
import com.agriplatform.cultivation.entity.CultivationIntentionHistory;
import com.agriplatform.cultivation.entity.CultivationStatusEvent;
import com.agriplatform.cultivation.repository.CultivationHistoryRepository;
import com.agriplatform.cultivation.repository.CultivationIntentionRepository;
import com.agriplatform.cultivation.repository.CultivationStatusEventRepository;
import com.agriplatform.farmer.entity.FarmerProfile;
import com.agriplatform.farmer.repository.FarmerProfileRepository;
import com.agriplatform.history.dto.HistorySnapshotDto;
import com.agriplatform.history.dto.HistoryStatusEventDto;
import com.agriplatform.history.service.HistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Task 09 — HistoryService unit tests.
 *
 * <p>Tests the service independently of the DB using mocked repositories.
 * Covers all categories from the Task 09 test matrix.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Task 09 — HistoryServiceImpl Tests")
class HistoryServiceTest {

    @Mock private CultivationHistoryRepository historyRepository;
    @Mock private CultivationStatusEventRepository statusEventRepository;
    @Mock private CultivationIntentionRepository cultivationRepository;
    @Mock private FarmerProfileRepository farmerProfileRepository;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private HistoryServiceImpl historyService;

    private static final String FARMER_A_ID = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
    private static final String FARMER_B_ID = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
    private static final UUID FARMER_A = UUID.fromString(FARMER_A_ID);
    private static final UUID FARMER_B = UUID.fromString(FARMER_B_ID);

    private FarmerProfile activeFarmerA;

    @BeforeEach
    void setUp() {
        activeFarmerA = new FarmerProfile();
        activeFarmerA.setId(FARMER_A);
        activeFarmerA.setProfileStatus(FarmerProfile.STATUS_ACTIVE);
    }

    // =========================================================================
    // Helper factory methods
    // =========================================================================

    private CultivationIntentionHistory makeHistory(UUID intentionId, int version, String changeType) {
        CultivationIntentionHistory h = new CultivationIntentionHistory();
        h.setHistoryId(UUID.randomUUID());
        h.setIntentionId(intentionId);
        h.setFarmerId(FARMER_A);
        h.setVersion(version);
        h.setSnapshotAt(OffsetDateTime.now());
        h.setPlotId(UUID.randomUUID());
        h.setCropId(UUID.randomUUID());
        h.setSeasonId(UUID.randomUUID());
        h.setCultivationYear((short) 2026);
        h.setCultivatedAreaAcres(new BigDecimal("5.00"));
        h.setPlannedPlantingDate(LocalDate.of(2026, 6, 15));
        h.setStatus("PLANNED");
        h.setSource("FARMER_APP");
        h.setActorId(FARMER_A);
        h.setActorType("FARMER");
        h.setChangeType(changeType);
        h.setChangedFields(new String[]{"cultivatedAreaAcres"});
        return h;
    }

    private CultivationStatusEvent makeEvent(UUID intentionId, String from, String to) {
        CultivationStatusEvent e = new CultivationStatusEvent();
        e.setId(UUID.randomUUID());
        e.setIntentionId(intentionId);
        e.setFromStatus(from);
        e.setToStatus(to);
        e.setChangedBy(FARMER_A);
        e.setActorType("FARMER");
        e.setChangedAt(OffsetDateTime.now());
        return e;
    }

    // =========================================================================
    // A. SNAPSHOTS — get paginated list
    // =========================================================================

    @Nested
    @DisplayName("A. Snapshot Pagination Tests")
    class SnapshotPaginationTests {

        @Test
        @DisplayName("Returns paginated snapshots for own cultivation — newest version first")
        void returns_paginated_snapshots_newest_first() {
            UUID cultivationId = UUID.randomUUID();
            var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "version"));
            var history = List.of(
                    makeHistory(cultivationId, 3, "STATUS_CHANGE"),
                    makeHistory(cultivationId, 2, "FIELD_UPDATE"),
                    makeHistory(cultivationId, 1, "STATUS_CHANGE")
            );
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_A)).thenReturn(true);
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(cultivationId, pageable))
                    .thenReturn(new PageImpl<>(history));

            Page<HistorySnapshotDto> page = historyService.getSnapshotsForCultivation(cultivationId, pageable);

            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getContent().get(0).version()).isEqualTo(3);
            assertThat(page.getContent().get(1).version()).isEqualTo(2);
            assertThat(page.getContent().get(2).version()).isEqualTo(1);
        }

        @Test
        @DisplayName("Returns empty page when cultivation has no history")
        void returns_empty_page_when_no_history() {
            UUID cultivationId = UUID.randomUUID();
            var pageable = PageRequest.of(0, 20);
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_A)).thenReturn(true);
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(cultivationId, pageable))
                    .thenReturn(Page.empty());

            Page<HistorySnapshotDto> page = historyService.getSnapshotsForCultivation(cultivationId, pageable);
            assertThat(page.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Respects page boundaries — returns second page correctly")
        void respects_page_boundaries() {
            UUID cultivationId = UUID.randomUUID();
            var pageable = PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "version"));
            var page2Content = List.of(makeHistory(cultivationId, 1, "STATUS_CHANGE"));
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_A)).thenReturn(true);
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(cultivationId, pageable))
                    .thenReturn(new PageImpl<>(page2Content, pageable, 3));

            Page<HistorySnapshotDto> page = historyService.getSnapshotsForCultivation(cultivationId, pageable);
            assertThat(page.getTotalElements()).isEqualTo(3);
            assertThat(page.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("IDOR: Farmer B cannot list Farmer A's snapshots — 404 returned")
        void idor_farmer_b_cannot_list_farmer_a_snapshots() {
            UUID cultivationId = UUID.randomUUID();
            var pageable = PageRequest.of(0, 20);
            FarmerProfile farmerB = new FarmerProfile();
            farmerB.setId(FARMER_B);
            farmerB.setProfileStatus(FarmerProfile.STATUS_ACTIVE);
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_B);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_B)).thenReturn(Optional.of(farmerB));
            // Farmer B querying Farmer A's cultivation → returns false (no cross-farmer)
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_B)).thenReturn(false);

            assertThatThrownBy(() -> historyService.getSnapshotsForCultivation(cultivationId, pageable))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("404 when cultivation does not exist at all")
        void returns_404_for_nonexistent_cultivation() {
            UUID cultivationId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_A)).thenReturn(false);

            assertThatThrownBy(() -> historyService.getSnapshotsForCultivation(cultivationId, PageRequest.of(0, 20)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("404 when farmer profile not found")
        void returns_404_when_farmer_not_found() {
            UUID cultivationId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> historyService.getSnapshotsForCultivation(cultivationId, PageRequest.of(0, 20)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("404 when farmer profile is deleted")
        void returns_404_when_farmer_deleted() {
            UUID cultivationId = UUID.randomUUID();
            FarmerProfile deleted = new FarmerProfile();
            deleted.setId(FARMER_A);
            deleted.setProfileStatus(FarmerProfile.STATUS_DELETED);
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(deleted));

            assertThatThrownBy(() -> historyService.getSnapshotsForCultivation(cultivationId, PageRequest.of(0, 20)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Suspended farmer can still READ history (read-only, no mutation risk)")
        void suspended_farmer_can_read_history() {
            UUID cultivationId = UUID.randomUUID();
            FarmerProfile suspended = new FarmerProfile();
            suspended.setId(FARMER_A);
            suspended.setProfileStatus(FarmerProfile.STATUS_SUSPENDED);
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(suspended));
            // Suspended farmer CAN read their own data (history is read-only)
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_A)).thenReturn(true);
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(eq(cultivationId), any()))
                    .thenReturn(Page.empty());

            // Should NOT throw — suspended farmers can read their own history
            Page<HistorySnapshotDto> page = historyService.getSnapshotsForCultivation(cultivationId, PageRequest.of(0, 20));
            assertThat(page.getContent()).isEmpty();
        }

        @Test
        @DisplayName("Snapshot correctly maps all fields from entity including changedFields array")
        void snapshot_dto_correctly_maps_changed_fields() {
            UUID cultivationId = UUID.randomUUID();
            var pageable = PageRequest.of(0, 20);
            CultivationIntentionHistory h = makeHistory(cultivationId, 2, "FIELD_UPDATE");
            h.setChangedFields(new String[]{"cultivatedAreaAcres", "plannedPlantingDate"});

            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_A)).thenReturn(true);
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(cultivationId, pageable))
                    .thenReturn(new PageImpl<>(List.of(h)));

            Page<HistorySnapshotDto> page = historyService.getSnapshotsForCultivation(cultivationId, pageable);
            HistorySnapshotDto dto = page.getContent().get(0);

            assertThat(dto.changeType()).isEqualTo("FIELD_UPDATE");
            assertThat(dto.changedFields()).containsExactly("cultivatedAreaAcres", "plannedPlantingDate");
            assertThat(dto.farmerId()).isEqualTo(FARMER_A);
            assertThat(dto.actorId()).isEqualTo(FARMER_A);
            assertThat(dto.actorType()).isEqualTo("FARMER");
        }

        @Test
        @DisplayName("Snapshot with null changedFields array maps to empty list")
        void snapshot_null_changed_fields_maps_to_empty_list() {
            UUID cultivationId = UUID.randomUUID();
            var pageable = PageRequest.of(0, 20);
            CultivationIntentionHistory h = makeHistory(cultivationId, 1, "STATUS_CHANGE");
            h.setChangedFields(null);

            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_A)).thenReturn(true);
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(cultivationId, pageable))
                    .thenReturn(new PageImpl<>(List.of(h)));

            Page<HistorySnapshotDto> page = historyService.getSnapshotsForCultivation(cultivationId, pageable);
            assertThat(page.getContent().get(0).changedFields()).isEmpty();
        }
    }

    // =========================================================================
    // B. SINGLE SNAPSHOT LOOKUP
    // =========================================================================

    @Nested
    @DisplayName("B. Single Snapshot Lookup Tests")
    class SingleSnapshotTests {

        @Test
        @DisplayName("Returns single snapshot by historyId for own cultivation")
        void returns_single_snapshot_correctly() {
            UUID cultivationId = UUID.randomUUID();
            CultivationIntentionHistory h = makeHistory(cultivationId, 2, "FIELD_UPDATE");
            UUID historyId = h.getHistoryId();

            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(historyRepository.findByHistoryIdAndIntentionIdAndFarmerId(historyId, cultivationId, FARMER_A))
                    .thenReturn(Optional.of(h));

            HistorySnapshotDto dto = historyService.getSnapshot(cultivationId, historyId);
            assertThat(dto.historyId()).isEqualTo(historyId);
            assertThat(dto.version()).isEqualTo(2);
        }

        @Test
        @DisplayName("404 when historyId belongs to a different cultivation")
        void returns_404_when_history_belongs_to_different_cultivation() {
            UUID correctCultivationId = UUID.randomUUID();
            UUID wrongCultivationId = UUID.randomUUID();
            UUID historyId = UUID.randomUUID();

            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(historyRepository.findByHistoryIdAndIntentionIdAndFarmerId(historyId, wrongCultivationId, FARMER_A))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> historyService.getSnapshot(wrongCultivationId, historyId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("IDOR: Farmer B cannot fetch Farmer A's snapshot by known historyId")
        void idor_farmer_b_cannot_access_farmer_a_snapshot() {
            UUID cultivationId = UUID.randomUUID();
            UUID historyId = UUID.randomUUID();
            FarmerProfile farmerB = new FarmerProfile();
            farmerB.setId(FARMER_B);
            farmerB.setProfileStatus(FarmerProfile.STATUS_ACTIVE);

            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_B);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_B)).thenReturn(Optional.of(farmerB));
            when(historyRepository.findByHistoryIdAndIntentionIdAndFarmerId(historyId, cultivationId, FARMER_B))
                    .thenReturn(Optional.empty()); // DB query scoped to Farmer B → no result

            assertThatThrownBy(() -> historyService.getSnapshot(cultivationId, historyId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("404 when historyId does not exist at all")
        void returns_404_for_nonexistent_history_id() {
            UUID cultivationId = UUID.randomUUID();
            UUID fakeHistoryId = UUID.randomUUID();

            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(historyRepository.findByHistoryIdAndIntentionIdAndFarmerId(fakeHistoryId, cultivationId, FARMER_A))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> historyService.getSnapshot(cultivationId, fakeHistoryId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // =========================================================================
    // C. STATUS EVENTS — paginated
    // =========================================================================

    @Nested
    @DisplayName("C. Status Event Pagination Tests")
    class StatusEventTests {

        @Test
        @DisplayName("Returns paginated events for own cultivation — chronological order")
        void returns_events_in_chronological_order() {
            UUID cultivationId = UUID.randomUUID();
            var pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "changedAt"));
            var events = List.of(
                    makeEvent(cultivationId, null, "PLANNED"),
                    makeEvent(cultivationId, "PLANNED", "STARTED"),
                    makeEvent(cultivationId, "STARTED", "COMPLETED")
            );
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_A)).thenReturn(true);
            when(statusEventRepository.findAllByIntentionIdOrderByChangedAtAsc(cultivationId, pageable))
                    .thenReturn(new PageImpl<>(events));

            Page<HistoryStatusEventDto> page = historyService.getStatusEventsForCultivation(cultivationId, pageable);
            assertThat(page.getContent()).hasSize(3);
            assertThat(page.getContent().get(0).fromStatus()).isNull();
            assertThat(page.getContent().get(0).toStatus()).isEqualTo("PLANNED");
            assertThat(page.getContent().get(1).toStatus()).isEqualTo("STARTED");
            assertThat(page.getContent().get(2).toStatus()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("Returns empty events for newly created cultivation (initial state only)")
        void returns_events_for_fresh_cultivation() {
            UUID cultivationId = UUID.randomUUID();
            var pageable = PageRequest.of(0, 20);
            // Even a brand new cultivation has 1 status event (PLANNED) from the DB trigger
            var events = List.of(makeEvent(cultivationId, null, "PLANNED"));
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_A)).thenReturn(true);
            when(statusEventRepository.findAllByIntentionIdOrderByChangedAtAsc(cultivationId, pageable))
                    .thenReturn(new PageImpl<>(events));

            Page<HistoryStatusEventDto> page = historyService.getStatusEventsForCultivation(cultivationId, pageable);
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).fromStatus()).isNull();
            assertThat(page.getContent().get(0).toStatus()).isEqualTo("PLANNED");
        }

        @Test
        @DisplayName("IDOR: Farmer B cannot list status events for Farmer A's cultivation")
        void idor_farmer_b_cannot_list_events_for_farmer_a() {
            UUID cultivationId = UUID.randomUUID();
            FarmerProfile farmerB = new FarmerProfile();
            farmerB.setId(FARMER_B);
            farmerB.setProfileStatus(FarmerProfile.STATUS_ACTIVE);
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_B);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_B)).thenReturn(Optional.of(farmerB));
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_B)).thenReturn(false);

            assertThatThrownBy(() -> historyService.getStatusEventsForCultivation(cultivationId, PageRequest.of(0, 20)))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Status event DTO carries actor attribution from DB trigger")
        void status_event_carries_correct_actor() {
            UUID cultivationId = UUID.randomUUID();
            var pageable = PageRequest.of(0, 20);
            CultivationStatusEvent e = makeEvent(cultivationId, null, "PLANNED");
            // Actor set by DB trigger from app.actor_id session context
            e.setChangedBy(FARMER_A);
            e.setActorType("FARMER");

            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_A)).thenReturn(true);
            when(statusEventRepository.findAllByIntentionIdOrderByChangedAtAsc(cultivationId, pageable))
                    .thenReturn(new PageImpl<>(List.of(e)));

            Page<HistoryStatusEventDto> page = historyService.getStatusEventsForCultivation(cultivationId, pageable);
            HistoryStatusEventDto dto = page.getContent().get(0);
            assertThat(dto.changedBy()).isEqualTo(FARMER_A);
            assertThat(dto.actorType()).isEqualTo("FARMER");
        }
    }

    // =========================================================================
    // D. IMMUTABILITY — no write operations exposed
    // =========================================================================

    @Nested
    @DisplayName("D. Immutability Tests")
    class ImmutabilityTests {

        @Test
        @DisplayName("HistoryServiceImpl has no save/delete methods — interface is read-only")
        void history_service_interface_is_read_only() {
            // Verify the HistoryService interface only exposes read methods
            var methods = com.agriplatform.history.service.HistoryService.class.getDeclaredMethods();
            for (var method : methods) {
                String name = method.getName();
                assertThat(name).as("Method '%s' should not be a write operation", name)
                        .doesNotContain("save", "delete", "update", "create", "insert", "remove");
            }
        }

        @Test
        @DisplayName("HistoryServiceImpl never calls save/delete on history repository")
        void history_service_never_writes_to_history_repository() {
            UUID cultivationId = UUID.randomUUID();
            var pageable = PageRequest.of(0, 20);
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_A)).thenReturn(true);
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(cultivationId, pageable))
                    .thenReturn(Page.empty());

            historyService.getSnapshotsForCultivation(cultivationId, pageable);

            // Verify absolutely no save/delete calls on the history repository
            verify(historyRepository, never()).save(any());
            verify(historyRepository, never()).saveAll(any());
            verify(historyRepository, never()).delete(any());
            verify(historyRepository, never()).deleteAll();
            verify(historyRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("HistoryServiceImpl never calls save/delete on status event repository")
        void history_service_never_writes_to_event_repository() {
            UUID cultivationId = UUID.randomUUID();
            var pageable = PageRequest.of(0, 20);
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_A)).thenReturn(true);
            when(statusEventRepository.findAllByIntentionIdOrderByChangedAtAsc(cultivationId, pageable))
                    .thenReturn(Page.empty());

            historyService.getStatusEventsForCultivation(cultivationId, pageable);

            verify(statusEventRepository, never()).save(any());
            verify(statusEventRepository, never()).saveAll(any());
            verify(statusEventRepository, never()).delete(any());
            verify(statusEventRepository, never()).deleteAll();
            verify(statusEventRepository, never()).deleteById(any());
        }
    }

    // =========================================================================
    // E. ACTOR ATTRIBUTION
    // =========================================================================

    @Nested
    @DisplayName("E. Actor Attribution Tests")
    class ActorAttributionTests {

        @Test
        @DisplayName("Snapshot actorId matches the farmer who performed the change (DB trigger value)")
        void snapshot_actor_matches_farmer_who_made_change() {
            UUID cultivationId = UUID.randomUUID();
            CultivationIntentionHistory h = makeHistory(cultivationId, 2, "FIELD_UPDATE");
            h.setActorId(FARMER_A);
            h.setActorType("FARMER");

            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_A)).thenReturn(true);
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(eq(cultivationId), any()))
                    .thenReturn(new PageImpl<>(List.of(h)));

            Page<HistorySnapshotDto> page = historyService.getSnapshotsForCultivation(cultivationId, PageRequest.of(0, 20));
            assertThat(page.getContent().get(0).actorId()).isEqualTo(FARMER_A);
            assertThat(page.getContent().get(0).actorType()).isEqualTo("FARMER");
        }

        @Test
        @DisplayName("Snapshot farmerId always reflects the record owner, not the reader")
        void snapshot_farmer_id_is_record_owner_not_reader() {
            // farmerId in history is Farmer A's (the owner), even when read by Farmer A
            UUID cultivationId = UUID.randomUUID();
            CultivationIntentionHistory h = makeHistory(cultivationId, 1, "STATUS_CHANGE");
            // farmerId in history row = FARMER_A (owner)
            h.setFarmerId(FARMER_A);

            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerA));
            when(cultivationRepository.existsByIdAndFarmerId(cultivationId, FARMER_A)).thenReturn(true);
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(eq(cultivationId), any()))
                    .thenReturn(new PageImpl<>(List.of(h)));

            Page<HistorySnapshotDto> page = historyService.getSnapshotsForCultivation(cultivationId, PageRequest.of(0, 20));
            assertThat(page.getContent().get(0).farmerId()).isEqualTo(FARMER_A);
        }
    }
}
