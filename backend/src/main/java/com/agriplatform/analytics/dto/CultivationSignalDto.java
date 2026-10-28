package com.agriplatform.analytics.dto;

import java.util.Map;

/**
 * Deterministic decision-support signal derived from aggregated metrics and trends.
 *
 * <h2>Strict Decision-Support Contract</h2>
 * <p>Signals provide objective, explainable indicators (e.g. high crop concentration,
 * accelerating acreage growth). Signals NEVER issue prescriptive orders or autonomous
 * farming instructions ("Do not grow X", "Grow Y").
 */
public record CultivationSignalDto(
        String signalKey,
        String signalType,
        String title,
        String description,
        Map<String, Object> metricBasis
) {}
