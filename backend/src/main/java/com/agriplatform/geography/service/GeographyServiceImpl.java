package com.agriplatform.geography.service;

import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.geography.dto.DistrictDto;
import com.agriplatform.geography.dto.MandalDto;
import com.agriplatform.geography.dto.StateDto;
import com.agriplatform.geography.dto.VillageDto;
import com.agriplatform.geography.mapper.GeographyMapper;
import com.agriplatform.geography.repository.DistrictRepository;
import com.agriplatform.geography.repository.MandalRepository;
import com.agriplatform.geography.repository.StateRepository;
import com.agriplatform.geography.repository.VillageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Default implementation of {@link GeographyService}.
 *
 * <h2>Responsibilities</h2>
 * <ul>
 *   <li>Fetch geography entities from their repositories</li>
 *   <li>Validate parent resources exist before returning children</li>
 *   <li>Map entities to DTOs via {@link GeographyMapper}</li>
 *   <li>Throw {@link ResourceNotFoundException} for invalid IDs (→ HTTP 404)</li>
 * </ul>
 *
 * <h2>Read-only transactions</h2>
 * <p>All methods use {@code @Transactional(readOnly = true)} to allow
 * Hibernate to skip dirty-checking and flush on these queries.
 */
@Service
@Transactional(readOnly = true)
public class GeographyServiceImpl implements GeographyService {

    private final StateRepository stateRepository;
    private final DistrictRepository districtRepository;
    private final MandalRepository mandalRepository;
    private final VillageRepository villageRepository;
    private final GeographyMapper geographyMapper;

    public GeographyServiceImpl(
            StateRepository stateRepository,
            DistrictRepository districtRepository,
            MandalRepository mandalRepository,
            VillageRepository villageRepository,
            GeographyMapper geographyMapper) {
        this.stateRepository = stateRepository;
        this.districtRepository = districtRepository;
        this.mandalRepository = mandalRepository;
        this.villageRepository = villageRepository;
        this.geographyMapper = geographyMapper;
    }

    private final java.util.concurrent.atomic.AtomicReference<List<StateDto>> stateCache =
            new java.util.concurrent.atomic.AtomicReference<>();

    // -------------------------------------------------------------------------
    // States
    // -------------------------------------------------------------------------

    @Override
    public List<StateDto> getAllActiveStates() {
        List<StateDto> cached = stateCache.get();
        if (cached != null) {
            return cached;
        }
        List<StateDto> states = geographyMapper.toStateDtoList(
                stateRepository.findByIsActiveTrueOrderByNameAsc()
        );
        stateCache.compareAndSet(null, states);
        return states;
    }

    // -------------------------------------------------------------------------
    // Districts
    // -------------------------------------------------------------------------

    @Override
    public List<DistrictDto> getActiveDistrictsByState(UUID stateId) {
        var districts = districtRepository.findActiveByStateId(stateId);
        if (districts.isEmpty() && !stateRepository.existsById(stateId)) {
            throw new ResourceNotFoundException("State not found: " + stateId);
        }
        return geographyMapper.toDistrictDtoList(districts);
    }

    // -------------------------------------------------------------------------
    // Mandals
    // -------------------------------------------------------------------------

    @Override
    public List<MandalDto> getActiveMandalsByDistrict(UUID districtId) {
        var mandals = mandalRepository.findActiveByDistrictId(districtId);
        if (mandals.isEmpty() && !districtRepository.existsById(districtId)) {
            throw new ResourceNotFoundException("District not found: " + districtId);
        }
        return geographyMapper.toMandalDtoList(mandals);
    }

    // -------------------------------------------------------------------------
    // Villages
    // -------------------------------------------------------------------------

    @Override
    public List<VillageDto> getActiveVillagesByMandal(UUID mandalId) {
        var villages = villageRepository.findActiveByMandalId(mandalId);
        if (villages.isEmpty() && !mandalRepository.existsById(mandalId)) {
            throw new ResourceNotFoundException("Mandal not found: " + mandalId);
        }
        return geographyMapper.toVillageDtoList(villages);
    }
}
