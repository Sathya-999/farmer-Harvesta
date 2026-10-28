package com.agriplatform.cultivation.dto;

import java.util.UUID;

public record CropVarietyResponseDto(
    UUID id,
    UUID cropId,
    String name,
    boolean isActive
) {}
