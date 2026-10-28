package com.agriplatform.cultivation.dto;

import java.util.UUID;

public record SeasonResponseDto(
    UUID id,
    String name,
    Short anchorMonth,
    Short anchorDay,
    String description,
    boolean isActive
) {}
