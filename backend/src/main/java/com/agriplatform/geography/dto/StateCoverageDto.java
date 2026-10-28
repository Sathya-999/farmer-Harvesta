package com.agriplatform.geography.dto;

/**
 * Administrative data coverage summary for a supported pilot state.
 */
public record StateCoverageDto(
        String stateCode,
        String stateName,
        int dbDistricts,
        int expectedDistricts,
        int dbMandals,
        int expectedMandals,
        int dbVillages,
        int expectedVillages,
        String coverageStatus,
        boolean syncRequired
) {}
