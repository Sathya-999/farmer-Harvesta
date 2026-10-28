package com.agriplatform.analytics.dto;

/**
 * Factual, explainable analytical insight narrative explaining agricultural data context.
 *
 * <h2>Alpha Safety Policy</h2>
 * <p>Insights are strictly informational summaries of verified empirical data.
 * No autonomous decision-making or algorithmic crop allocation.
 */
public record AnalyticsInsightDto(
        String category,
        String summary,
        String factualNarrative
) {}
