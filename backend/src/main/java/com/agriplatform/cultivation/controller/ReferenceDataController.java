package com.agriplatform.cultivation.controller;

import com.agriplatform.common.response.ApiResponse;
import com.agriplatform.cultivation.dto.CropResponseDto;
import com.agriplatform.cultivation.dto.CropVarietyResponseDto;
import com.agriplatform.cultivation.dto.SeasonResponseDto;
import com.agriplatform.cultivation.service.CultivationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for reference data: Crops, Varieties, and Seasons.
 */
@RestController
@RequestMapping("/api/v1")
public class ReferenceDataController {

    private final CultivationService cultivationService;

    public ReferenceDataController(CultivationService cultivationService) {
        this.cultivationService = cultivationService;
    }

    @GetMapping("/crops")
    public ResponseEntity<ApiResponse<List<CropResponseDto>>> listCrops() {
        List<CropResponseDto> crops = cultivationService.listActiveCrops();
        return ResponseEntity.ok(ApiResponse.success(crops));
    }

    @GetMapping("/crops/{cropId}/varieties")
    public ResponseEntity<ApiResponse<List<CropVarietyResponseDto>>> listVarieties(
            @PathVariable UUID cropId) {
        List<CropVarietyResponseDto> varieties = cultivationService.listActiveVarietiesForCrop(cropId);
        return ResponseEntity.ok(ApiResponse.success(varieties));
    }

    @GetMapping("/seasons")
    public ResponseEntity<ApiResponse<List<SeasonResponseDto>>> listSeasons() {
        List<SeasonResponseDto> seasons = cultivationService.listActiveSeasons();
        return ResponseEntity.ok(ApiResponse.success(seasons));
    }
}
