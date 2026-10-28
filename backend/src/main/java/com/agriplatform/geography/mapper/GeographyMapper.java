package com.agriplatform.geography.mapper;

import com.agriplatform.geography.dto.DistrictDto;
import com.agriplatform.geography.dto.MandalDto;
import com.agriplatform.geography.dto.StateDto;
import com.agriplatform.geography.dto.VillageDto;
import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stateless mapper from geography JPA entities to their corresponding DTOs.
 *
 * <h2>Design decisions</h2>
 * <ul>
 *   <li>Manual mapping — no MapStruct or ModelMapper is added to avoid
 *       extra compile-time dependencies for a small, stable set of types.</li>
 *   <li>FK fields are extracted from the lazily-loaded proxy ID rather than
 *       navigating into the full parent object, preventing any unintended
 *       proxy initialization or N+1 queries.</li>
 *   <li>Parent objects are NOT nested in DTOs — references use only IDs.</li>
 * </ul>
 */
@Component
public class GeographyMapper {

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link State} entity to a {@link StateDto}.
     *
     * @param state the entity (must not be null)
     * @return the DTO representation
     */
    public StateDto toStateDto(State state) {
        return new StateDto(
                state.getId(),
                state.getName(),
                state.getCode(),
                state.isActive()
        );
    }

    /**
     * Maps a list of {@link State} entities to a list of {@link StateDto}s.
     *
     * @param states the entity list
     * @return the DTO list (preserves order)
     */
    public List<StateDto> toStateDtoList(List<State> states) {
        return states.stream()
                .map(this::toStateDto)
                .toList();
    }

    // -------------------------------------------------------------------------
    // District
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link District} entity to a {@link DistrictDto}.
     *
     * <p>The {@code stateId} is extracted by calling {@code getState().getId()}
     * which resolves the Hibernate proxy ID without triggering a full proxy
     * initialization (Hibernate resolves FK IDs without a SELECT when the
     * association is loaded via lazy proxy).
     *
     * @param district the entity (must not be null)
     * @return the DTO representation
     */
    public DistrictDto toDistrictDto(District district) {
        return new DistrictDto(
                district.getId(),
                district.getState().getId(),
                district.getName(),
                district.getCode(),
                district.isActive()
        );
    }

    /**
     * Maps a list of {@link District} entities to a list of {@link DistrictDto}s.
     */
    public List<DistrictDto> toDistrictDtoList(List<District> districts) {
        return districts.stream()
                .map(this::toDistrictDto)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Mandal
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link Mandal} entity to a {@link MandalDto}.
     *
     * @param mandal the entity (must not be null)
     * @return the DTO representation
     */
    public MandalDto toMandalDto(Mandal mandal) {
        return new MandalDto(
                mandal.getId(),
                mandal.getDistrict().getId(),
                mandal.getName(),
                mandal.getCode(),
                mandal.isActive()
        );
    }

    /**
     * Maps a list of {@link Mandal} entities to a list of {@link MandalDto}s.
     */
    public List<MandalDto> toMandalDtoList(List<Mandal> mandals) {
        return mandals.stream()
                .map(this::toMandalDto)
                .toList();
    }

    // -------------------------------------------------------------------------
    // Village
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link Village} entity to a {@link VillageDto}.
     *
     * @param village the entity (must not be null)
     * @return the DTO representation
     */
    public VillageDto toVillageDto(Village village) {
        return new VillageDto(
                village.getId(),
                village.getMandal().getId(),
                village.getName(),
                village.getCode(),
                village.getLatitude(),
                village.getLongitude(),
                village.isActive()
        );
    }

    /**
     * Maps a list of {@link Village} entities to a list of {@link VillageDto}s.
     */
    public List<VillageDto> toVillageDtoList(List<Village> villages) {
        return villages.stream()
                .map(this::toVillageDto)
                .toList();
    }
}
