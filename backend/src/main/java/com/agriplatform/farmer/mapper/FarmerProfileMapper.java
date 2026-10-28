package com.agriplatform.farmer.mapper;

import com.agriplatform.farmer.dto.FarmerProfileResponseDto;
import com.agriplatform.farmer.entity.FarmerProfile;
import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mapper for converting {@link FarmerProfile} entities to {@link FarmerProfileResponseDto}.
 */
@Component
public class FarmerProfileMapper {

    /**
     * Converts a {@link FarmerProfile} entity to its response DTO representation,
     * flattening the geography hierarchy into top-level response fields.
     *
     * @param profile the entity to map
     * @return response DTO
     */
    public FarmerProfileResponseDto toDto(FarmerProfile profile) {
        if (profile == null) {
            return null;
        }

        UUID homeVillageId = null;
        String villageName = null;
        UUID mandalId = null;
        String mandalName = null;
        UUID districtId = null;
        String districtName = null;
        UUID stateId = null;
        String stateName = null;

        Village village = profile.getHomeVillage();
        if (village != null) {
            homeVillageId = village.getId();
            villageName = village.getName();

            Mandal mandal = village.getMandal();
            if (mandal != null) {
                mandalId = mandal.getId();
                mandalName = mandal.getName();

                District district = mandal.getDistrict();
                if (district != null) {
                    districtId = district.getId();
                    districtName = district.getName();

                    State state = district.getState();
                    if (state != null) {
                        stateId = state.getId();
                        stateName = state.getName();
                    }
                }
            }
        }

        return new FarmerProfileResponseDto(
                profile.getId(),
                profile.getFullName(),
                homeVillageId,
                villageName,
                mandalId,
                mandalName,
                districtId,
                districtName,
                stateId,
                stateName,
                profile.getProfileStatus(),
                profile.isOnboardingCompleted(),
                profile.getVersion(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
