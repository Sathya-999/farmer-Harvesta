package com.agriplatform.cultivation.dto;

import java.util.UUID;

public record CropResponseDto(
    UUID id,
    String name,
    String category,
    boolean isActive
) {}
