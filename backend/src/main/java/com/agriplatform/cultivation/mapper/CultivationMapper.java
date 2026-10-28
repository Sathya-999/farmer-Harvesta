package com.agriplatform.cultivation.mapper;

import com.agriplatform.cultivation.dto.*;
import com.agriplatform.cultivation.entity.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class CultivationMapper {

    public CultivationResponseDto toDto(CultivationIntention intention) {
        if (intention == null) return null;

        return new CultivationResponseDto(
                intention.getId(),
                intention.getPlot() != null ? intention.getPlot().getId() : null,
                intention.getPlot() != null ? intention.getPlot().getLabel() : null,
                intention.getFarmerId(),
                intention.getCrop() != null ? intention.getCrop().getId() : null,
                intention.getCrop() != null ? intention.getCrop().getName() : null,
                intention.getCrop() != null ? intention.getCrop().getCategory() : null,
                intention.getCropVariety() != null ? intention.getCropVariety().getId() : null,
                intention.getCropVariety() != null ? intention.getCropVariety().getName() : null,
                intention.getSeason() != null ? intention.getSeason().getId() : null,
                intention.getSeason() != null ? intention.getSeason().getName() : null,
                intention.getCultivationYear(),
                intention.getCultivatedAreaAcres(),
                intention.getPlannedPlantingDate(),
                intention.getActualPlantingDate(),
                intention.getActualAreaAcres(),
                intention.getHarvestDate(),
                intention.getStatus(),
                intention.getStatusReason(),
                intention.getVersion(),
                intention.getSource(),
                intention.getCreatedAt(),
                intention.getUpdatedAt()
        );
    }

    public CultivationStatusEventDto toEventDto(CultivationStatusEvent event) {
        if (event == null) return null;

        return new CultivationStatusEventDto(
                event.getId(),
                event.getIntentionId(),
                event.getFromStatus(),
                event.getToStatus(),
                event.getChangedBy(),
                event.getActorType(),
                event.getChangedAt(),
                event.getNote()
        );
    }

    public CultivationHistoryDto toHistoryDto(CultivationIntentionHistory history) {
        if (history == null) return null;

        List<String> changedFieldsList = history.getChangedFields() != null
                ? Arrays.asList(history.getChangedFields())
                : Collections.emptyList();

        return new CultivationHistoryDto(
                history.getHistoryId(),
                history.getIntentionId(),
                history.getVersion(),
                history.getSnapshotAt(),
                history.getPlotId(),
                history.getFarmerId(),
                history.getCropId(),
                history.getCropVarietyId(),
                history.getSeasonId(),
                history.getCultivationYear(),
                history.getCultivatedAreaAcres(),
                history.getPlannedPlantingDate(),
                history.getActualPlantingDate(),
                history.getActualAreaAcres(),
                history.getHarvestDate(),
                history.getStatus(),
                history.getStatusReason(),
                history.getSource(),
                history.getActorId(),
                history.getActorType(),
                history.getChangeType(),
                changedFieldsList,
                history.getChangeReason()
        );
    }

    public CropResponseDto toCropDto(Crop crop) {
        if (crop == null) return null;
        return new CropResponseDto(
                crop.getId(),
                crop.getName(),
                crop.getCategory(),
                crop.isActive()
        );
    }

    public CropVarietyResponseDto toVarietyDto(CropVariety variety) {
        if (variety == null) return null;
        return new CropVarietyResponseDto(
                variety.getId(),
                variety.getCrop() != null ? variety.getCrop().getId() : null,
                variety.getName(),
                variety.isActive()
        );
    }

    public SeasonResponseDto toSeasonDto(Season season) {
        if (season == null) return null;
        return new SeasonResponseDto(
                season.getId(),
                season.getName(),
                season.getAnchorMonth(),
                season.getAnchorDay(),
                season.getDescription(),
                season.isActive()
        );
    }
}
