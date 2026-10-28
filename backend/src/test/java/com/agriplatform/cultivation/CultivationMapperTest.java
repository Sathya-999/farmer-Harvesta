package com.agriplatform.cultivation;

import com.agriplatform.cultivation.dto.*;
import com.agriplatform.cultivation.entity.*;
import com.agriplatform.cultivation.mapper.CultivationMapper;
import com.agriplatform.plot.entity.Plot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Task 08 — Cultivation Mapper Tests")
class CultivationMapperTest {

    private final CultivationMapper mapper = new CultivationMapper();

    @Test
    @DisplayName("Maps full CultivationIntention to CultivationResponseDto")
    void testToDtoFull() {
        Plot plot = new Plot();
        plot.setId(UUID.randomUUID());
        plot.setLabel("North Acre");

        Crop crop = new Crop(UUID.randomUUID(), "Paddy", "Cereals", true);
        CropVariety variety = new CropVariety(UUID.randomUUID(), crop, "Basmati 370", true);
        Season season = new Season(UUID.randomUUID(), "Kharif", (short) 6, (short) 1, "Monsoon", true);

        CultivationIntention ci = new CultivationIntention();
        ci.setId(UUID.randomUUID());
        ci.setPlot(plot);
        ci.setFarmerId(UUID.randomUUID());
        ci.setCrop(crop);
        ci.setCropVariety(variety);
        ci.setSeason(season);
        ci.setCultivationYear((short) 2026);
        ci.setCultivatedAreaAcres(new BigDecimal("4.50"));
        ci.setPlannedPlantingDate(LocalDate.of(2026, 6, 15));
        ci.setActualPlantingDate(LocalDate.of(2026, 6, 20));
        ci.setActualAreaAcres(new BigDecimal("4.25"));
        ci.setHarvestDate(LocalDate.of(2026, 11, 10));
        ci.setStatus(CultivationIntention.STATUS_COMPLETED);
        ci.setStatusReason("Harvest done");
        ci.setVersion(3);
        ci.setSource(CultivationIntention.SOURCE_FARMER_APP);
        OffsetDateTime now = OffsetDateTime.now();
        ci.setCreatedAt(now);
        ci.setUpdatedAt(now);

        CultivationResponseDto dto = mapper.toDto(ci);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(ci.getId());
        assertThat(dto.plotId()).isEqualTo(plot.getId());
        assertThat(dto.plotLabel()).isEqualTo("North Acre");
        assertThat(dto.farmerId()).isEqualTo(ci.getFarmerId());
        assertThat(dto.cropId()).isEqualTo(crop.getId());
        assertThat(dto.cropName()).isEqualTo("Paddy");
        assertThat(dto.cropCategory()).isEqualTo("Cereals");
        assertThat(dto.cropVarietyId()).isEqualTo(variety.getId());
        assertThat(dto.cropVarietyName()).isEqualTo("Basmati 370");
        assertThat(dto.seasonId()).isEqualTo(season.getId());
        assertThat(dto.seasonName()).isEqualTo("Kharif");
        assertThat(dto.cultivationYear()).isEqualTo((short) 2026);
        assertThat(dto.cultivatedAreaAcres()).isEqualTo(new BigDecimal("4.50"));
        assertThat(dto.plannedPlantingDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(dto.actualPlantingDate()).isEqualTo(LocalDate.of(2026, 6, 20));
        assertThat(dto.actualAreaAcres()).isEqualTo(new BigDecimal("4.25"));
        assertThat(dto.harvestDate()).isEqualTo(LocalDate.of(2026, 11, 10));
        assertThat(dto.status()).isEqualTo("COMPLETED");
        assertThat(dto.statusReason()).isEqualTo("Harvest done");
        assertThat(dto.version()).isEqualTo(3);
        assertThat(dto.source()).isEqualTo("FARMER_APP");
        assertThat(dto.createdAt()).isEqualTo(now);
        assertThat(dto.updatedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("Maps CultivationIntention with null variety and null actual fields")
    void testToDtoNullFields() {
        Plot plot = new Plot();
        plot.setId(UUID.randomUUID());
        Crop crop = new Crop(UUID.randomUUID(), "Cotton", "Fiber", true);
        Season season = new Season(UUID.randomUUID(), "Kharif", (short) 6, (short) 1, null, true);

        CultivationIntention ci = new CultivationIntention();
        ci.setId(UUID.randomUUID());
        ci.setPlot(plot);
        ci.setFarmerId(UUID.randomUUID());
        ci.setCrop(crop);
        ci.setCropVariety(null);
        ci.setSeason(season);
        ci.setCultivationYear((short) 2026);
        ci.setCultivatedAreaAcres(new BigDecimal("2.00"));
        ci.setPlannedPlantingDate(LocalDate.of(2026, 6, 1));
        ci.setStatus(CultivationIntention.STATUS_PLANNED);
        ci.setVersion(1);

        CultivationResponseDto dto = mapper.toDto(ci);
        assertThat(dto).isNotNull();
        assertThat(dto.cropVarietyId()).isNull();
        assertThat(dto.cropVarietyName()).isNull();
        assertThat(dto.actualPlantingDate()).isNull();
        assertThat(dto.actualAreaAcres()).isNull();
        assertThat(dto.harvestDate()).isNull();
        assertThat(dto.status()).isEqualTo("PLANNED");
    }

    @Test
    @DisplayName("Maps null entity returns null")
    void testToDtoNull() {
        assertThat(mapper.toDto(null)).isNull();
        assertThat(mapper.toEventDto(null)).isNull();
        assertThat(mapper.toHistoryDto(null)).isNull();
        assertThat(mapper.toCropDto(null)).isNull();
        assertThat(mapper.toVarietyDto(null)).isNull();
        assertThat(mapper.toSeasonDto(null)).isNull();
    }

    @Test
    @DisplayName("Maps CultivationStatusEvent to CultivationStatusEventDto")
    void testToEventDto() {
        CultivationStatusEvent event = new CultivationStatusEvent();
        event.setId(UUID.randomUUID());
        event.setIntentionId(UUID.randomUUID());
        event.setFromStatus("PLANNED");
        event.setToStatus("STARTED");
        event.setChangedBy(UUID.randomUUID());
        event.setActorType("FARMER");
        OffsetDateTime now = OffsetDateTime.now();
        event.setChangedAt(now);
        event.setNote("Sowing started");

        CultivationStatusEventDto dto = mapper.toEventDto(event);
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(event.getId());
        assertThat(dto.intentionId()).isEqualTo(event.getIntentionId());
        assertThat(dto.fromStatus()).isEqualTo("PLANNED");
        assertThat(dto.toStatus()).isEqualTo("STARTED");
        assertThat(dto.changedBy()).isEqualTo(event.getChangedBy());
        assertThat(dto.actorType()).isEqualTo("FARMER");
        assertThat(dto.changedAt()).isEqualTo(now);
        assertThat(dto.note()).isEqualTo("Sowing started");
    }

    @Test
    @DisplayName("Maps CultivationIntentionHistory to CultivationHistoryDto")
    void testToHistoryDto() {
        CultivationIntentionHistory history = new CultivationIntentionHistory();
        history.setHistoryId(UUID.randomUUID());
        history.setIntentionId(UUID.randomUUID());
        history.setVersion(1);
        OffsetDateTime now = OffsetDateTime.now();
        history.setSnapshotAt(now);
        history.setPlotId(UUID.randomUUID());
        history.setFarmerId(UUID.randomUUID());
        history.setCropId(UUID.randomUUID());
        history.setSeasonId(UUID.randomUUID());
        history.setCultivationYear((short) 2026);
        history.setCultivatedAreaAcres(new BigDecimal("3.00"));
        history.setPlannedPlantingDate(LocalDate.of(2026, 6, 1));
        history.setStatus("PLANNED");
        history.setSource("FARMER_APP");
        history.setActorType("FARMER");
        history.setChangeType("FIELD_UPDATE");
        history.setChangedFields(new String[]{"cultivated_area_acres"});

        CultivationHistoryDto dto = mapper.toHistoryDto(history);
        assertThat(dto).isNotNull();
        assertThat(dto.historyId()).isEqualTo(history.getHistoryId());
        assertThat(dto.changedFields()).containsExactly("cultivated_area_acres");
        assertThat(dto.changeType()).isEqualTo("FIELD_UPDATE");
    }

    @Test
    @DisplayName("Maps reference data entities")
    void testReferenceMappers() {
        Crop crop = new Crop(UUID.randomUUID(), "Maize", "Cereal", true);
        CropResponseDto cropDto = mapper.toCropDto(crop);
        assertThat(cropDto.name()).isEqualTo("Maize");

        CropVariety variety = new CropVariety(UUID.randomUUID(), crop, "Hybrid 1", true);
        CropVarietyResponseDto varDto = mapper.toVarietyDto(variety);
        assertThat(varDto.name()).isEqualTo("Hybrid 1");
        assertThat(varDto.cropId()).isEqualTo(crop.getId());

        Season season = new Season(UUID.randomUUID(), "Zaid", (short) 3, (short) 1, "Summer", true);
        SeasonResponseDto ssnDto = mapper.toSeasonDto(season);
        assertThat(ssnDto.name()).isEqualTo("Zaid");
        assertThat(ssnDto.anchorMonth()).isEqualTo((short) 3);
    }
}
