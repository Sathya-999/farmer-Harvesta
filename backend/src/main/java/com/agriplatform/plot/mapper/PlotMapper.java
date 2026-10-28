package com.agriplatform.plot.mapper;

import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.plot.dto.PlotResponseDto;
import com.agriplatform.plot.entity.Plot;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper for converting {@link Plot} entities to {@link PlotResponseDto}.
 *
 * <h2>DTO Safety Invariant</h2>
 * <p>This mapper MUST NOT expose {@code Village.latitude} or {@code Village.longitude}
 * in the response. Village GPS coordinates are geography reference data and are not
 * plot GPS coordinates. Plot GPS is deferred to Beta V2.
 */
@Component
public class PlotMapper {

    /**
     * Converts a {@link Plot} entity to its response DTO, flattening the
     * geography hierarchy into top-level response fields.
     *
     * @param plot the entity to map (must not be null)
     * @return response DTO
     */
    public PlotResponseDto toDto(Plot plot) {
        if (plot == null) {
            return null;
        }

        UUID villageId = null;
        String villageName = null;
        String mandalName = null;
        String districtName = null;
        String stateName = null;

        Village village = plot.getVillage();
        if (village != null) {
            villageId = village.getId();
            villageName = village.getName();
            // NOTE: village.getLatitude() and village.getLongitude() are deliberately
            // NOT mapped here. They represent village centroid coordinates, NOT plot GPS.

            Mandal mandal = village.getMandal();
            if (mandal != null) {
                mandalName = mandal.getName();

                District district = mandal.getDistrict();
                if (district != null) {
                    districtName = district.getName();

                    State state = district.getState();
                    if (state != null) {
                        stateName = state.getName();
                    }
                }
            }
        }

        return new PlotResponseDto(
                plot.getId(),
                plot.getFarmerId(),
                villageId,
                villageName,
                mandalName,
                districtName,
                stateName,
                plot.getLabel(),
                plot.getTotalAreaAcres(),
                plot.isActive(),
                plot.getVersion(),
                plot.getCreatedAt(),
                plot.getUpdatedAt()
        );
    }
}
