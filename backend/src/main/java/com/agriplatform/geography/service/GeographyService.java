package com.agriplatform.geography.service;

import com.agriplatform.geography.dto.DistrictDto;
import com.agriplatform.geography.dto.MandalDto;
import com.agriplatform.geography.dto.StateDto;
import com.agriplatform.geography.dto.VillageDto;

import java.util.List;
import java.util.UUID;

/**
 * Contract for the Geography / Reference-data service.
 *
 * <p>All geography queries are read-only reference data operations.
 * Defining an interface allows clean test-time mocking without requiring
 * Mockito's inline bytecode instrumentation (which has limitations on Java 25).
 */
public interface GeographyService {

    /**
     * Returns all active states ordered alphabetically by name.
     */
    List<StateDto> getAllActiveStates();

    /**
     * Returns all active districts for the given state.
     *
     * @param stateId the parent state UUID
     * @throws com.agriplatform.common.exception.ResourceNotFoundException
     *         if no state exists with the given ID
     */
    List<DistrictDto> getActiveDistrictsByState(UUID stateId);

    /**
     * Returns all active mandals for the given district.
     *
     * @param districtId the parent district UUID
     * @throws com.agriplatform.common.exception.ResourceNotFoundException
     *         if no district exists with the given ID
     */
    List<MandalDto> getActiveMandalsByDistrict(UUID districtId);

    /**
     * Returns all active villages for the given mandal.
     *
     * @param mandalId the parent mandal UUID
     * @throws com.agriplatform.common.exception.ResourceNotFoundException
     *         if no mandal exists with the given ID
     */
    List<VillageDto> getActiveVillagesByMandal(UUID mandalId);
}
