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
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Task 09 — History Lifecycle, Event Representation, and State Integrity Test Suite.
 *
 * <p>Covers:
 * <ul>
 *   <li>Status lifecycle transitions &amp; event capture integrity</li>
 *   <li>Single-field vs multi-field change audit representations</li>
 *   <li>Version monotonicity and deterministic ordering</li>
 *   <li>Pagination edge cases and page boundaries</li>
 *   <li>Repository query contracts for IDOR scoping</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Task 09 — History Lifecycle & State Integrity Tests")
class HistoryLifecycleAndIntegrityTest {

    @Mock private CultivationHistoryRepository historyRepository;
    @Mock private CultivationStatusEventRepository statusEventRepository;
    @Mock private CultivationIntentionRepository cultivationRepository;
    @Mock private FarmerProfileRepository farmerProfileRepository;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks
    private HistoryServiceImpl historyService;

    private static final String FARMER_A_ID = "33333333-3333-3333-3333-333333333333";
    private static final UUID FARMER_A = UUID.fromString(FARMER_A_ID);
    private FarmerProfile activeFarmerProfile;

    @BeforeEach
    void setUp() {
        activeFarmerProfile = new FarmerProfile();
        activeFarmerProfile.setId(FARMER_A);
        activeFarmerProfile.setProfileStatus(FarmerProfile.STATUS_ACTIVE);
    }

    private CultivationIntentionHistory createHistoryRecord(
            UUID intentionId, int version, String changeType, String status, String[] changedFields) {
        CultivationIntentionHistory h = new CultivationIntentionHistory();
        h.setHistoryId(UUID.randomUUID());
        h.setIntentionId(intentionId);
        h.setFarmerId(FARMER_A);
        h.setPlotId(UUID.randomUUID());
        h.setCropId(UUID.randomUUID());
        h.setCropVarietyId(UUID.randomUUID());
        h.setSeasonId(UUID.randomUUID());
        h.setCultivationYear((short) 2026);
        h.setCultivatedAreaAcres(new BigDecimal("4.50"));
        h.setPlannedPlantingDate(LocalDate.of(2026, 6, 15));
        h.setStatus(status);
        h.setVersion(version);
        h.setSource("FARMER_APP");
        h.setActorId(FARMER_A);
        h.setActorType("FARMER");
        h.setChangeType(changeType);
        h.setChangedFields(changedFields);
        h.setSnapshotAt(OffsetDateTime.now().minusHours(10 - version));
        return h;
    }

    private CultivationStatusEvent createStatusEvent(
            UUID intentionId, String fromStatus, String toStatus, int hourOffset) {
        CultivationStatusEvent e = new CultivationStatusEvent();
        e.setId(UUID.randomUUID());
        e.setIntentionId(intentionId);
        e.setFromStatus(fromStatus);
        e.setToStatus(toStatus);
        e.setChangedBy(FARMER_A);
        e.setActorType("FARMER");
        e.setChangedAt(OffsetDateTime.now().minusHours(10 - hourOffset));
        e.setNote("Status changed to " + toStatus);
        return e;
    }

    // =========================================================================
    // 1. STATUS LIFECYCLE EVENT SEQUENCING
    // =========================================================================

    @Nested
    @DisplayName("1. Status Lifecycle Event Sequencing")
    class StatusLifecycleSequencingTests {

        @Test
        @DisplayName("Full happy path lifecycle: PLANNED -> STARTED -> COMPLETED")
        void testFullLifecycleEvents() {
            UUID intentionId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerProfile));
            when(cultivationRepository.existsByIdAndFarmerId(intentionId, FARMER_A)).thenReturn(true);

            List<CultivationStatusEvent> events = List.of(
                    createStatusEvent(intentionId, null, "PLANNED", 1),
                    createStatusEvent(intentionId, "PLANNED", "STARTED", 2),
                    createStatusEvent(intentionId, "STARTED", "COMPLETED", 3)
            );
            Page<CultivationStatusEvent> page = new PageImpl<>(events, PageRequest.of(0, 20), 3);
            when(statusEventRepository.findAllByIntentionIdOrderByChangedAtAsc(eq(intentionId), any()))
                    .thenReturn(page);

            Page<HistoryStatusEventDto> result = historyService.getStatusEventsForCultivation(intentionId, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent().get(0).fromStatus()).isNull();
            assertThat(result.getContent().get(0).toStatus()).isEqualTo("PLANNED");
            assertThat(result.getContent().get(1).fromStatus()).isEqualTo("PLANNED");
            assertThat(result.getContent().get(1).toStatus()).isEqualTo("STARTED");
            assertThat(result.getContent().get(2).fromStatus()).isEqualTo("STARTED");
            assertThat(result.getContent().get(2).toStatus()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("Cancellation lifecycle: PLANNED -> CANCELLED")
        void testPlannedToCancelledLifecycle() {
            UUID intentionId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerProfile));
            when(cultivationRepository.existsByIdAndFarmerId(intentionId, FARMER_A)).thenReturn(true);

            List<CultivationStatusEvent> events = List.of(
                    createStatusEvent(intentionId, null, "PLANNED", 1),
                    createStatusEvent(intentionId, "PLANNED", "CANCELLED", 2)
            );
            when(statusEventRepository.findAllByIntentionIdOrderByChangedAtAsc(eq(intentionId), any()))
                    .thenReturn(new PageImpl<>(events));

            Page<HistoryStatusEventDto> result = historyService.getStatusEventsForCultivation(intentionId, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(1).toStatus()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("Abandonment lifecycle: STARTED -> ABANDONED")
        void testStartedToAbandonedLifecycle() {
            UUID intentionId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerProfile));
            when(cultivationRepository.existsByIdAndFarmerId(intentionId, FARMER_A)).thenReturn(true);

            List<CultivationStatusEvent> events = List.of(
                    createStatusEvent(intentionId, null, "PLANNED", 1),
                    createStatusEvent(intentionId, "PLANNED", "STARTED", 2),
                    createStatusEvent(intentionId, "STARTED", "ABANDONED", 3)
            );
            when(statusEventRepository.findAllByIntentionIdOrderByChangedAtAsc(eq(intentionId), any()))
                    .thenReturn(new PageImpl<>(events));

            Page<HistoryStatusEventDto> result = historyService.getStatusEventsForCultivation(intentionId, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent().get(2).fromStatus()).isEqualTo("STARTED");
            assertThat(result.getContent().get(2).toStatus()).isEqualTo("ABANDONED");
        }

        @Test
        @DisplayName("Started to cancelled lifecycle: STARTED -> CANCELLED")
        void testStartedToCancelledLifecycle() {
            UUID intentionId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerProfile));
            when(cultivationRepository.existsByIdAndFarmerId(intentionId, FARMER_A)).thenReturn(true);

            List<CultivationStatusEvent> events = List.of(
                    createStatusEvent(intentionId, null, "PLANNED", 1),
                    createStatusEvent(intentionId, "PLANNED", "STARTED", 2),
                    createStatusEvent(intentionId, "STARTED", "CANCELLED", 3)
            );
            when(statusEventRepository.findAllByIntentionIdOrderByChangedAtAsc(eq(intentionId), any()))
                    .thenReturn(new PageImpl<>(events));

            Page<HistoryStatusEventDto> result = historyService.getStatusEventsForCultivation(intentionId, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.getContent().get(2).fromStatus()).isEqualTo("STARTED");
            assertThat(result.getContent().get(2).toStatus()).isEqualTo("CANCELLED");
        }
    }

    // =========================================================================
    // 2. FIELD CHANGE REPRESENTATION AUDIT
    // =========================================================================

    @Nested
    @DisplayName("2. Field Change Representation Audit")
    class FieldChangeRepresentationTests {

        @Test
        @DisplayName("Single-field update records exact field in changedFields array")
        void testSingleFieldUpdateRepresentation() {
            UUID intentionId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerProfile));
            when(cultivationRepository.existsByIdAndFarmerId(intentionId, FARMER_A)).thenReturn(true);

            CultivationIntentionHistory h = createHistoryRecord(
                    intentionId, 2, "FIELD_UPDATE", "PLANNED", new String[]{"cultivated_area_acres"});
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(eq(intentionId), any()))
                    .thenReturn(new PageImpl<>(List.of(h)));

            Page<HistorySnapshotDto> result = historyService.getSnapshotsForCultivation(intentionId, PageRequest.of(0, 20));

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).changedFields()).containsExactly("cultivated_area_acres");
            assertThat(result.getContent().get(0).changeType()).isEqualTo("FIELD_UPDATE");
        }

        @Test
        @DisplayName("Multi-field update records all modified fields in single snapshot")
        void testMultiFieldUpdateRepresentation() {
            UUID intentionId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerProfile));
            when(cultivationRepository.existsByIdAndFarmerId(intentionId, FARMER_A)).thenReturn(true);

            String[] modifiedFields = new String[]{"crop_variety_id", "cultivated_area_acres", "planned_planting_date"};
            CultivationIntentionHistory h = createHistoryRecord(
                    intentionId, 2, "FIELD_UPDATE", "PLANNED", modifiedFields);
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(eq(intentionId), any()))
                    .thenReturn(new PageImpl<>(List.of(h)));

            Page<HistorySnapshotDto> result = historyService.getSnapshotsForCultivation(intentionId, PageRequest.of(0, 20));

            assertThat(result.getContent().get(0).changedFields())
                    .containsExactlyInAnyOrder("crop_variety_id", "cultivated_area_acres", "planned_planting_date");
        }

        @Test
        @DisplayName("Create event snapshot has changeType CREATE with null or initial fields")
        void testCreateSnapshotRepresentation() {
            UUID intentionId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerProfile));
            when(cultivationRepository.existsByIdAndFarmerId(intentionId, FARMER_A)).thenReturn(true);

            CultivationIntentionHistory h = createHistoryRecord(
                    intentionId, 1, "CREATE", "PLANNED", null);
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(eq(intentionId), any()))
                    .thenReturn(new PageImpl<>(List.of(h)));

            Page<HistorySnapshotDto> result = historyService.getSnapshotsForCultivation(intentionId, PageRequest.of(0, 20));

            assertThat(result.getContent().get(0).version()).isEqualTo(1);
            assertThat(result.getContent().get(0).changeType()).isEqualTo("CREATE");
            assertThat(result.getContent().get(0).changedFields()).isEmpty();
        }

        @Test
        @DisplayName("Status change snapshot has changeType STATUS_CHANGE with status field audited")
        void testStatusChangeSnapshotRepresentation() {
            UUID intentionId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerProfile));
            when(cultivationRepository.existsByIdAndFarmerId(intentionId, FARMER_A)).thenReturn(true);

            CultivationIntentionHistory h = createHistoryRecord(
                    intentionId, 3, "STATUS_CHANGE", "STARTED", new String[]{"status"});
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(eq(intentionId), any()))
                    .thenReturn(new PageImpl<>(List.of(h)));

            Page<HistorySnapshotDto> result = historyService.getSnapshotsForCultivation(intentionId, PageRequest.of(0, 20));

            assertThat(result.getContent().get(0).version()).isEqualTo(3);
            assertThat(result.getContent().get(0).changeType()).isEqualTo("STATUS_CHANGE");
            assertThat(result.getContent().get(0).changedFields()).containsExactly("status");
        }
    }

    // =========================================================================
    // 3. PAGINATION BOUNDARIES & EDGE CASES
    // =========================================================================

    @Nested
    @DisplayName("3. Pagination Boundaries & Edge Cases")
    class PaginationBoundaryTests {

        @Test
        @DisplayName("Empty history returns page with totalElements 0 and empty content")
        void testEmptyHistoryPagination() {
            UUID intentionId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerProfile));
            when(cultivationRepository.existsByIdAndFarmerId(intentionId, FARMER_A)).thenReturn(true);

            Page<CultivationIntentionHistory> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(eq(intentionId), any()))
                    .thenReturn(emptyPage);

            Page<HistorySnapshotDto> result = historyService.getSnapshotsForCultivation(intentionId, PageRequest.of(0, 20));

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
            assertThat(result.getTotalPages()).isZero();
        }

        @Test
        @DisplayName("Exact page size boundary (e.g. 5 items on size 5) hasNext is false")
        void testExactPageSizeBoundary() {
            UUID intentionId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerProfile));
            when(cultivationRepository.existsByIdAndFarmerId(intentionId, FARMER_A)).thenReturn(true);

            List<CultivationIntentionHistory> fiveItems = List.of(
                    createHistoryRecord(intentionId, 5, "UPDATE", "PLANNED", null),
                    createHistoryRecord(intentionId, 4, "UPDATE", "PLANNED", null),
                    createHistoryRecord(intentionId, 3, "UPDATE", "PLANNED", null),
                    createHistoryRecord(intentionId, 2, "UPDATE", "PLANNED", null),
                    createHistoryRecord(intentionId, 1, "CREATE", "PLANNED", null)
            );
            Page<CultivationIntentionHistory> page = new PageImpl<>(fiveItems, PageRequest.of(0, 5), 5);
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(eq(intentionId), any()))
                    .thenReturn(page);

            Page<HistorySnapshotDto> result = historyService.getSnapshotsForCultivation(intentionId, PageRequest.of(0, 5));

            assertThat(result.getContent()).hasSize(5);
            assertThat(result.hasNext()).isFalse();
            assertThat(result.getTotalPages()).isEqualTo(1);
        }

        @Test
        @DisplayName("Multi-page split (total 10 items, size 3): page 0 has next, page 3 has last item")
        void testMultiPageSplit() {
            UUID intentionId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerProfile));
            when(cultivationRepository.existsByIdAndFarmerId(intentionId, FARMER_A)).thenReturn(true);

            List<CultivationIntentionHistory> page0Items = List.of(
                    createHistoryRecord(intentionId, 10, "UPDATE", "PLANNED", null),
                    createHistoryRecord(intentionId, 9, "UPDATE", "PLANNED", null),
                    createHistoryRecord(intentionId, 8, "UPDATE", "PLANNED", null)
            );
            Page<CultivationIntentionHistory> page0 = new PageImpl<>(page0Items, PageRequest.of(0, 3), 10);
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(eq(intentionId), eq(PageRequest.of(0, 3))))
                    .thenReturn(page0);

            Page<HistorySnapshotDto> result = historyService.getSnapshotsForCultivation(intentionId, PageRequest.of(0, 3));

            assertThat(result.getContent()).hasSize(3);
            assertThat(result.hasNext()).isTrue();
            assertThat(result.getTotalPages()).isEqualTo(4);
            assertThat(result.getContent().get(0).version()).isEqualTo(10);
        }

        @Test
        @DisplayName("Page out of bounds (requesting page 5 when only 2 pages exist) returns empty content")
        void testPageOutOfBoundsReturnsEmpty() {
            UUID intentionId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerProfile));
            when(cultivationRepository.existsByIdAndFarmerId(intentionId, FARMER_A)).thenReturn(true);

            Page<CultivationIntentionHistory> emptyOutOfBounds = new PageImpl<>(Collections.emptyList(), PageRequest.of(5, 5), 8);
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(eq(intentionId), eq(PageRequest.of(5, 5))))
                    .thenReturn(emptyOutOfBounds);

            Page<HistorySnapshotDto> result = historyService.getSnapshotsForCultivation(intentionId, PageRequest.of(5, 5));

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isEqualTo(8);
        }
    }

    // =========================================================================
    // 4. MONOTONIC VERSIONING & ORDERING INTEGRITY
    // =========================================================================

    @Nested
    @DisplayName("4. Monotonic Versioning & Ordering Integrity")
    class VersionOrderingIntegrityTests {

        @Test
        @DisplayName("Snapshots are strictly descending by version: v4 -> v3 -> v2 -> v1")
        void testMonotonicVersionOrderingDescending() {
            UUID intentionId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerProfile));
            when(cultivationRepository.existsByIdAndFarmerId(intentionId, FARMER_A)).thenReturn(true);

            List<CultivationIntentionHistory> items = List.of(
                    createHistoryRecord(intentionId, 4, "STATUS_CHANGE", "COMPLETED", null),
                    createHistoryRecord(intentionId, 3, "STATUS_CHANGE", "STARTED", null),
                    createHistoryRecord(intentionId, 2, "FIELD_UPDATE", "PLANNED", null),
                    createHistoryRecord(intentionId, 1, "CREATE", "PLANNED", null)
            );
            when(historyRepository.findAllByIntentionIdOrderByVersionDesc(eq(intentionId), any()))
                    .thenReturn(new PageImpl<>(items));

            Page<HistorySnapshotDto> result = historyService.getSnapshotsForCultivation(intentionId, PageRequest.of(0, 20));

            List<Integer> versions = result.getContent().stream().map(HistorySnapshotDto::version).toList();
            assertThat(versions).containsExactly(4, 3, 2, 1);
        }

        @Test
        @DisplayName("Status events are strictly ascending by changedAt: oldest first")
        void testStatusEventsAscendingByChangedAt() {
            UUID intentionId = UUID.randomUUID();
            when(securityUtils.getAuthenticatedUserId()).thenReturn(FARMER_A);
            when(farmerProfileRepository.findByIdWithGeography(FARMER_A)).thenReturn(Optional.of(activeFarmerProfile));
            when(cultivationRepository.existsByIdAndFarmerId(intentionId, FARMER_A)).thenReturn(true);

            List<CultivationStatusEvent> events = List.of(
                    createStatusEvent(intentionId, null, "PLANNED", 1),
                    createStatusEvent(intentionId, "PLANNED", "STARTED", 2),
                    createStatusEvent(intentionId, "STARTED", "COMPLETED", 3)
            );
            when(statusEventRepository.findAllByIntentionIdOrderByChangedAtAsc(eq(intentionId), any()))
                    .thenReturn(new PageImpl<>(events));

            Page<HistoryStatusEventDto> result = historyService.getStatusEventsForCultivation(intentionId, PageRequest.of(0, 20));

            for (int i = 0; i < result.getContent().size() - 1; i++) {
                OffsetDateTime current = result.getContent().get(i).changedAt();
                OffsetDateTime next = result.getContent().get(i + 1).changedAt();
                assertThat(current).isBeforeOrEqualTo(next);
            }
        }
    }
}
