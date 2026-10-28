package com.agriplatform.cultivation.service;

import com.agriplatform.cultivation.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface CultivationService {

    CultivationResponseDto createCultivation(CreateCultivationRequestDto request, String idempotencyKey);

    CultivationResponseDto getCultivation(UUID id);

    Page<CultivationResponseDto> listCultivations(
            Pageable pageable,
            UUID plotId,
            UUID seasonId,
            Short cultivationYear,
            UUID cropId,
            String status);

    CultivationResponseDto updateCultivation(UUID id, UpdateCultivationRequestDto request);

    CultivationResponseDto updateCultivationStatus(UUID id, CultivationStatusUpdateRequestDto request);

    List<CultivationStatusEventDto> getCultivationEvents(UUID id);

    List<CultivationHistoryDto> getCultivationHistory(UUID id);

    List<CropResponseDto> listActiveCrops();

    List<CropVarietyResponseDto> listActiveVarietiesForCrop(UUID cropId);

    List<SeasonResponseDto> listActiveSeasons();
}
