package com.agriplatform.history;

import com.agriplatform.history.dto.HistorySnapshotDto;
import com.agriplatform.history.dto.HistoryStatusEventDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task 09 — History DTO / Entity Tests.
 * Verifies that the DTO records hold values correctly and that
 * their fields are accessible with the right types.
 */
@DisplayName("Task 09 — History DTO Tests")
class HistoryDtoTest {

    // =========================================================================
    // A. HistorySnapshotDto
    // =========================================================================

    @Test
    @DisplayName("HistorySnapshotDto holds all fields correctly")
    void snapshot_dto_fields_are_accessible() {
        UUID historyId = UUID.randomUUID();
        UUID intentionId = UUID.randomUUID();
        UUID plotId = UUID.randomUUID();
        UUID farmerId = UUID.randomUUID();
        UUID cropId = UUID.randomUUID();
        UUID varietyId = UUID.randomUUID();
        UUID seasonId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        HistorySnapshotDto dto = new HistorySnapshotDto(
                historyId, intentionId, 2, now,
                plotId, farmerId, cropId, varietyId, seasonId, (short) 2026,
                new BigDecimal("5.00"), LocalDate.of(2026, 6, 15),
                LocalDate.of(2026, 7, 1), new BigDecimal("4.80"),
                LocalDate.of(2026, 10, 1), "STARTED", null, "FARMER_APP",
                actorId, "FARMER", "FIELD_UPDATE", List.of("cultivatedAreaAcres"), null
        );

        assertThat(dto.historyId()).isEqualTo(historyId);
        assertThat(dto.intentionId()).isEqualTo(intentionId);
        assertThat(dto.version()).isEqualTo(2);
        assertThat(dto.snapshotAt()).isEqualTo(now);
        assertThat(dto.plotId()).isEqualTo(plotId);
        assertThat(dto.farmerId()).isEqualTo(farmerId);
        assertThat(dto.cropId()).isEqualTo(cropId);
        assertThat(dto.cropVarietyId()).isEqualTo(varietyId);
        assertThat(dto.seasonId()).isEqualTo(seasonId);
        assertThat(dto.cultivationYear()).isEqualTo((short) 2026);
        assertThat(dto.cultivatedAreaAcres()).isEqualByComparingTo("5.00");
        assertThat(dto.plannedPlantingDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(dto.actualPlantingDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(dto.actualAreaAcres()).isEqualByComparingTo("4.80");
        assertThat(dto.harvestDate()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(dto.status()).isEqualTo("STARTED");
        assertThat(dto.statusReason()).isNull();
        assertThat(dto.source()).isEqualTo("FARMER_APP");
        assertThat(dto.actorId()).isEqualTo(actorId);
        assertThat(dto.actorType()).isEqualTo("FARMER");
        assertThat(dto.changeType()).isEqualTo("FIELD_UPDATE");
        assertThat(dto.changedFields()).containsExactly("cultivatedAreaAcres");
        assertThat(dto.changeReason()).isNull();
    }

    @Test
    @DisplayName("HistorySnapshotDto with null optional fields is valid")
    void snapshot_dto_nullable_fields() {
        HistorySnapshotDto dto = new HistorySnapshotDto(
                UUID.randomUUID(), UUID.randomUUID(), 1, OffsetDateTime.now(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                null, UUID.randomUUID(), (short) 2026,
                new BigDecimal("3.00"), LocalDate.of(2026, 6, 1),
                null, null, null,
                "PLANNED", null, "FARMER_APP",
                null, "FARMER", "STATUS_CHANGE", List.of(), null
        );

        assertThat(dto.cropVarietyId()).isNull();
        assertThat(dto.actualPlantingDate()).isNull();
        assertThat(dto.actualAreaAcres()).isNull();
        assertThat(dto.harvestDate()).isNull();
        assertThat(dto.actorId()).isNull();
        assertThat(dto.changedFields()).isEmpty();
    }

    @Test
    @DisplayName("HistorySnapshotDto changeType reflects actual DB trigger event types")
    void snapshot_dto_change_types() {
        // Valid change types as produced by the live DB trigger
        for (String changeType : List.of("STATUS_CHANGE", "FIELD_UPDATE")) {
            HistorySnapshotDto dto = new HistorySnapshotDto(
                    UUID.randomUUID(), UUID.randomUUID(), 1, OffsetDateTime.now(),
                    UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                    null, UUID.randomUUID(), (short) 2026,
                    new BigDecimal("3.00"), LocalDate.of(2026, 6, 1),
                    null, null, null,
                    "PLANNED", null, "FARMER_APP",
                    UUID.randomUUID(), "FARMER", changeType, List.of(), null
            );
            assertThat(dto.changeType()).isEqualTo(changeType);
        }
    }

    // =========================================================================
    // B. HistoryStatusEventDto
    // =========================================================================

    @Test
    @DisplayName("HistoryStatusEventDto holds all fields correctly")
    void status_event_dto_fields_are_accessible() {
        UUID eventId = UUID.randomUUID();
        UUID intentionId = UUID.randomUUID();
        UUID changedBy = UUID.randomUUID();
        OffsetDateTime changedAt = OffsetDateTime.now();

        HistoryStatusEventDto dto = new HistoryStatusEventDto(
                eventId, intentionId, null, "PLANNED", changedBy, "FARMER", changedAt, null
        );

        assertThat(dto.eventId()).isEqualTo(eventId);
        assertThat(dto.intentionId()).isEqualTo(intentionId);
        assertThat(dto.fromStatus()).isNull();
        assertThat(dto.toStatus()).isEqualTo("PLANNED");
        assertThat(dto.changedBy()).isEqualTo(changedBy);
        assertThat(dto.actorType()).isEqualTo("FARMER");
        assertThat(dto.changedAt()).isEqualTo(changedAt);
        assertThat(dto.note()).isNull();
    }

    @Test
    @DisplayName("HistoryStatusEventDto captures all valid lifecycle transitions")
    void status_event_dto_lifecycle_transitions() {
        // All valid state machine transitions per Task 08 contract
        Object[][] transitions = {
                {null,      "PLANNED"},
                {"PLANNED", "STARTED"},
                {"STARTED", "COMPLETED"},
                {"PLANNED", "CANCELLED"},
                {"STARTED", "CANCELLED"},
                {"STARTED", "ABANDONED"},
        };

        for (Object[] t : transitions) {
            HistoryStatusEventDto dto = new HistoryStatusEventDto(
                    UUID.randomUUID(), UUID.randomUUID(),
                    (String) t[0], (String) t[1],
                    UUID.randomUUID(), "FARMER", OffsetDateTime.now(), null
            );
            assertThat(dto.fromStatus()).isEqualTo((String) t[0]);
            assertThat(dto.toStatus()).isEqualTo((String) t[1]);
        }
    }
}
