package com.agriplatform.geography.dto;

import java.math.BigDecimal;

/**
 * Result of GPS-assisted administrative location resolution.
 *
 * <p>Always flagged as an advisory candidate requiring farmer confirmation.
 */
public record LocationResolutionDto(
        BigDecimal latitude,
        BigDecimal longitude,
        String candidateStateCode,
        String candidateStateName,
        String candidateDistrictCode,
        String candidateDistrictName,
        String candidateMandalCode,
        String candidateMandalName,
        String candidateVillageCode,
        String candidateVillageName,
        String confidence,
        boolean manualConfirmationRequired,
        boolean isExactBoundary,
        String advisoryMessage
) {
    public static LocationResolutionDto createSuggestion(
            BigDecimal lat, BigDecimal lon,
            String stateCode, String stateName,
            String distCode, String distName,
            String mandalCode, String mandalName,
            String villageCode, String villageName,
            String advisory) {
        return createResolved(
                lat, lon,
                stateCode, stateName,
                distCode, distName,
                mandalCode, mandalName,
                villageCode, villageName,
                "SUGGESTION",
                advisory
        );
    }

    public static LocationResolutionDto createResolved(
            BigDecimal lat, BigDecimal lon,
            String stateCode, String stateName,
            String distCode, String distName,
            String mandalCode, String mandalName,
            String villageCode, String villageName,
            String confidence,
            String advisory) {
        return new LocationResolutionDto(
                lat, lon,
                stateCode, stateName,
                distCode, distName,
                mandalCode, mandalName,
                villageCode, villageName,
                confidence,
                true,
                false,
                advisory
        );
    }
}
