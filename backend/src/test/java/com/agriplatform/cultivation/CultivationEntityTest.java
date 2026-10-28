package com.agriplatform.cultivation;

import com.agriplatform.cultivation.entity.*;
import com.agriplatform.plot.entity.Plot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task 08 — Cultivation Entity Tests")
class CultivationEntityTest {

    @Test
    @DisplayName("Season entity fields and defaults")
    void testSeasonEntity() {
        UUID id = UUID.randomUUID();
        Season season = new Season(id, "Kharif", (short) 6, (short) 1, "Monsoon crop season", true);

        assertThat(season.getId()).isEqualTo(id);
        assertThat(season.getName()).isEqualTo("Kharif");
        assertThat(season.getAnchorMonth()).isEqualTo((short) 6);
        assertThat(season.getAnchorDay()).isEqualTo((short) 1);
        assertThat(season.getDescription()).isEqualTo("Monsoon crop season");
        assertThat(season.isActive()).isTrue();
        assertThat(season.getCreatedAt()).isNotNull();
        assertThat(season.getUpdatedAt()).isNotNull();

        season.setActive(false);
        assertThat(season.isActive()).isFalse();
    }

    @Test
    @DisplayName("Crop entity fields and defaults")
    void testCropEntity() {
        UUID id = UUID.randomUUID();
        Crop crop = new Crop(id, "Paddy", "Cereals", true);

        assertThat(crop.getId()).isEqualTo(id);
        assertThat(crop.getName()).isEqualTo("Paddy");
        assertThat(crop.getCategory()).isEqualTo("Cereals");
        assertThat(crop.isActive()).isTrue();
        assertThat(crop.getCreatedAt()).isNotNull();
        assertThat(crop.getUpdatedAt()).isNotNull();

        crop.setName("Rice");
        crop.setCategory("Food Grains");
        assertThat(crop.getName()).isEqualTo("Rice");
        assertThat(crop.getCategory()).isEqualTo("Food Grains");
    }

    @Test
    @DisplayName("CropVariety entity fields and relationships")
    void testCropVarietyEntity() {
        Crop crop = new Crop(UUID.randomUUID(), "Cotton", "Fiber", true);
        UUID varietyId = UUID.randomUUID();
        CropVariety variety = new CropVariety(varietyId, crop, "Bt Cotton 2", true);

        assertThat(variety.getId()).isEqualTo(varietyId);
        assertThat(variety.getCrop()).isEqualTo(crop);
        assertThat(variety.getName()).isEqualTo("Bt Cotton 2");
        assertThat(variety.isActive()).isTrue();
    }

    @Test
    @DisplayName("CultivationIntention defaults, getters, setters, and terminal status checks")
    void testCultivationIntentionEntity() {
        CultivationIntention intention = new CultivationIntention();
        assertThat(intention.getStatus()).isEqualTo(CultivationIntention.STATUS_PLANNED);
        assertThat(intention.getVersion()).isEqualTo(1);
        assertThat(intention.getSource()).isEqualTo(CultivationIntention.SOURCE_FARMER_APP);
        assertThat(intention.isTerminal()).isFalse();

        UUID id = UUID.randomUUID();
        UUID farmerId = UUID.randomUUID();
        Plot plot = new Plot();
        plot.setId(UUID.randomUUID());
        Crop crop = new Crop(UUID.randomUUID(), "Wheat", "Cereal", true);
        CropVariety variety = new CropVariety(UUID.randomUUID(), crop, "Sharbati", true);
        Season season = new Season(UUID.randomUUID(), "Rabi", (short) 10, (short) 15, "Winter", true);

        intention.setId(id);
        intention.setPlot(plot);
        intention.setFarmerId(farmerId);
        intention.setCrop(crop);
        intention.setCropVariety(variety);
        intention.setSeason(season);
        intention.setCultivationYear((short) 2026);
        intention.setCultivatedAreaAcres(new BigDecimal("5.50"));
        intention.setPlannedPlantingDate(LocalDate.of(2026, 10, 15));
        intention.setActualPlantingDate(LocalDate.of(2026, 10, 20));
        intention.setActualAreaAcres(new BigDecimal("5.00"));
        intention.setHarvestDate(LocalDate.of(2027, 3, 15));
        intention.setStatusReason("Normal harvest");

        assertThat(intention.getId()).isEqualTo(id);
        assertThat(intention.getPlot()).isEqualTo(plot);
        assertThat(intention.getFarmerId()).isEqualTo(farmerId);
        assertThat(intention.getCrop()).isEqualTo(crop);
        assertThat(intention.getCropVariety()).isEqualTo(variety);
        assertThat(intention.getSeason()).isEqualTo(season);
        assertThat(intention.getCultivationYear()).isEqualTo((short) 2026);
        assertThat(intention.getCultivatedAreaAcres()).isEqualTo(new BigDecimal("5.50"));
        assertThat(intention.getPlannedPlantingDate()).isEqualTo(LocalDate.of(2026, 10, 15));
        assertThat(intention.getActualPlantingDate()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(intention.getActualAreaAcres()).isEqualTo(new BigDecimal("5.00"));
        assertThat(intention.getHarvestDate()).isEqualTo(LocalDate.of(2027, 3, 15));
        assertThat(intention.getStatusReason()).isEqualTo("Normal harvest");

        // Terminal state tests
        intention.setStatus(CultivationIntention.STATUS_STARTED);
        assertThat(intention.isTerminal()).isFalse();

        intention.setStatus(CultivationIntention.STATUS_COMPLETED);
        assertThat(intention.isTerminal()).isTrue();

        intention.setStatus(CultivationIntention.STATUS_CANCELLED);
        assertThat(intention.isTerminal()).isTrue();

        intention.setStatus(CultivationIntention.STATUS_ABANDONED);
        assertThat(intention.isTerminal()).isTrue();
    }

    @Test
    @DisplayName("CultivationStatusEvent entity fields")
    void testCultivationStatusEventEntity() {
        CultivationStatusEvent event = new CultivationStatusEvent();
        UUID id = UUID.randomUUID();
        UUID intentionId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();

        event.setId(id);
        event.setIntentionId(intentionId);
        event.setFromStatus(CultivationIntention.STATUS_PLANNED);
        event.setToStatus(CultivationIntention.STATUS_STARTED);
        event.setChangedBy(actorId);
        event.setActorType("FARMER");
        event.setChangedAt(now);
        event.setNote("Sowing commenced");

        assertThat(event.getId()).isEqualTo(id);
        assertThat(event.getIntentionId()).isEqualTo(intentionId);
        assertThat(event.getFromStatus()).isEqualTo(CultivationIntention.STATUS_PLANNED);
        assertThat(event.getToStatus()).isEqualTo(CultivationIntention.STATUS_STARTED);
        assertThat(event.getChangedBy()).isEqualTo(actorId);
        assertThat(event.getActorType()).isEqualTo("FARMER");
        assertThat(event.getChangedAt()).isEqualTo(now);
        assertThat(event.getNote()).isEqualTo("Sowing commenced");
    }

    @Test
    @DisplayName("CultivationIntentionHistory entity fields")
    void testCultivationIntentionHistoryEntity() {
        CultivationIntentionHistory history = new CultivationIntentionHistory();
        UUID historyId = UUID.randomUUID();
        UUID intentionId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        String[] changedFields = new String[]{"status", "actual_planting_date"};

        history.setHistoryId(historyId);
        history.setIntentionId(intentionId);
        history.setVersion(2);
        history.setSnapshotAt(now);
        history.setStatus(CultivationIntention.STATUS_STARTED);
        history.setChangeType("STATUS_CHANGE");
        history.setChangedFields(changedFields);
        history.setChangeReason("Sowing completed");

        assertThat(history.getHistoryId()).isEqualTo(historyId);
        assertThat(history.getIntentionId()).isEqualTo(intentionId);
        assertThat(history.getVersion()).isEqualTo(2);
        assertThat(history.getSnapshotAt()).isEqualTo(now);
        assertThat(history.getStatus()).isEqualTo(CultivationIntention.STATUS_STARTED);
        assertThat(history.getChangeType()).isEqualTo("STATUS_CHANGE");
        assertThat(history.getChangedFields()).containsExactly("status", "actual_planting_date");
        assertThat(history.getChangeReason()).isEqualTo("Sowing completed");
    }
}
