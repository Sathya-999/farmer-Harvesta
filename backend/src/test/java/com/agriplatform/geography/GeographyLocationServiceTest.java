package com.agriplatform.geography;

import com.agriplatform.geography.dto.LocationResolutionDto;
import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.geography.repository.VillageRepository;
import com.agriplatform.geography.service.GeographyLocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeographyLocationService Unit & Spatial Resolution Tests")
class GeographyLocationServiceTest {

    @Mock
    private VillageRepository villageRepository;

    private GeographyLocationService locationService;

    @BeforeEach
    void setUp() {
        locationService = new GeographyLocationService(villageRepository);
    }

    @Test
    @DisplayName("Input Validation: Missing null coordinates return UNRESOLVED advisory")
    void testNullCoordinates() {
        LocationResolutionDto res = locationService.resolveCandidateLocation(null, null);

        assertThat(res.confidence()).isEqualTo("UNRESOLVED");
        assertThat(res.candidateStateCode()).isNull();
        assertThat(res.candidateDistrictCode()).isNull();
        assertThat(res.candidateMandalCode()).isNull();
        assertThat(res.candidateVillageCode()).isNull();
        assertThat(res.manualConfirmationRequired()).isTrue();
        assertThat(res.isExactBoundary()).isFalse();
        assertThat(res.advisoryMessage()).contains("No coordinates provided");
    }

    @Test
    @DisplayName("Input Validation: Out of range coordinates return UNRESOLVED advisory")
    void testOutOfRangeCoordinates() {
        LocationResolutionDto res = locationService.resolveCandidateLocation(new BigDecimal("95.0"), new BigDecimal("200.0"));

        assertThat(res.confidence()).isEqualTo("UNRESOLVED");
        assertThat(res.candidateStateCode()).isNull();
        assertThat(res.manualConfirmationRequired()).isTrue();
        assertThat(res.isExactBoundary()).isFalse();
        assertThat(res.advisoryMessage()).contains("out of geographic bounds");
    }

    @Test
    @DisplayName("Pilot Boundary: Coordinates in North India return UNRESOLVED outside pilot")
    void testOutsidePilotRegion() {
        // New Delhi coordinates: 28.6139, 77.2090
        LocationResolutionDto res = locationService.resolveCandidateLocation(new BigDecimal("28.6139"), new BigDecimal("77.2090"));

        assertThat(res.confidence()).isEqualTo("UNRESOLVED");
        assertThat(res.candidateStateCode()).isNull();
        assertThat(res.advisoryMessage()).contains("outside the five-state pilot region");
    }

    @Test
    @DisplayName("Honest State Fallback: Telangana resolves State 36 but NO false Adilabad district")
    void testTelanganaHonestFallbackNoFalseDistrict() {
        // Hyderabad coordinates: 17.3850, 78.4867
        when(villageRepository.findActiveWithinBoundingBox(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        LocationResolutionDto res = locationService.resolveCandidateLocation(new BigDecimal("17.3850"), new BigDecimal("78.4867"));

        assertThat(res.candidateStateCode()).isEqualTo("36");
        assertThat(res.candidateStateName()).isEqualTo("Telangana");
        assertThat(res.confidence()).isEqualTo("STATE_APPROXIMATE");

        // CRITICAL DEFECT FIX: Must NOT assign Adilabad to coordinates in Hyderabad!
        assertThat(res.candidateDistrictCode()).isNull();
        assertThat(res.candidateDistrictName()).isNull();
        assertThat(res.candidateMandalCode()).isNull();
        assertThat(res.candidateVillageCode()).isNull();
        assertThat(res.manualConfirmationRequired()).isTrue();
        assertThat(res.isExactBoundary()).isFalse();
        assertThat(res.advisoryMessage()).contains("District and village coordinates are not yet mapped");
    }

    @Test
    @DisplayName("Honest State Fallback: Kerala resolves State 32 but NO false Kottayam district")
    void testKeralaHonestFallbackNoFalseDistrict() {
        // Kochi coordinates: 9.9312, 76.2673
        when(villageRepository.findActiveWithinBoundingBox(any(), any(), any(), any()))
                .thenReturn(Collections.emptyList());

        LocationResolutionDto res = locationService.resolveCandidateLocation(new BigDecimal("9.9312"), new BigDecimal("76.2673"));

        assertThat(res.candidateStateCode()).isEqualTo("32");
        assertThat(res.candidateStateName()).isEqualTo("Kerala");
        assertThat(res.confidence()).isEqualTo("STATE_APPROXIMATE");

        // CRITICAL DEFECT FIX: Must NOT assign Kottayam across all of Kerala!
        assertThat(res.candidateDistrictCode()).isNull();
        assertThat(res.candidateDistrictName()).isNull();
        assertThat(res.candidateMandalCode()).isNull();
        assertThat(res.candidateVillageCode()).isNull();
    }

    @Test
    @DisplayName("Evidence-Based Matching: Resolves nearest village centroid within 5km threshold")
    void testNearestVillageCentroidMatchWithinThreshold() {
        // Query coordinate
        BigDecimal queryLat = new BigDecimal("14.500000");
        BigDecimal queryLon = new BigDecimal("77.450000");

        // Mock village located ~1.2 km away
        State state = new State();
        state.setId(UUID.randomUUID());
        state.setCode("28");
        state.setName("Andhra Pradesh");

        District dist = new District();
        dist.setId(UUID.randomUUID());
        dist.setState(state);
        dist.setCode("502");
        dist.setName("Ananthapuramu");

        Mandal mandal = new Mandal();
        mandal.setId(UUID.randomUUID());
        mandal.setDistrict(dist);
        mandal.setCode("5032");
        mandal.setName("Atmakur");

        Village village = new Village();
        village.setId(UUID.randomUUID());
        village.setMandal(mandal);
        village.setCode("597123");
        village.setName("Thopudurthi");
        village.setLatitude(new BigDecimal("14.508000"));
        village.setLongitude(new BigDecimal("77.455000"));
        village.setActive(true);

        when(villageRepository.findActiveWithinBoundingBox(any(), any(), any(), any()))
                .thenReturn(List.of(village));

        LocationResolutionDto res = locationService.resolveCandidateLocation(queryLat, queryLon);

        assertThat(res.confidence()).isEqualTo("NEAREST_VILLAGE_CENTROID");
        assertThat(res.candidateStateCode()).isEqualTo("28");
        assertThat(res.candidateStateName()).isEqualTo("Andhra Pradesh");
        assertThat(res.candidateDistrictCode()).isEqualTo("502");
        assertThat(res.candidateDistrictName()).isEqualTo("Ananthapuramu");
        assertThat(res.candidateMandalCode()).isEqualTo("5032");
        assertThat(res.candidateMandalName()).isEqualTo("Atmakur");
        assertThat(res.candidateVillageCode()).isEqualTo("597123");
        assertThat(res.candidateVillageName()).isEqualTo("Thopudurthi");
        assertThat(res.manualConfirmationRequired()).isTrue();
        assertThat(res.isExactBoundary()).isFalse();
        assertThat(res.advisoryMessage()).contains("Candidate village suggested");
    }

    @Test
    @DisplayName("Threshold Enforcement: Village beyond 5km is rejected as candidate")
    void testVillageBeyondThresholdRejected() {
        BigDecimal queryLat = new BigDecimal("14.500000");
        BigDecimal queryLon = new BigDecimal("77.450000");

        // Village located ~15 km away (> 5km threshold)
        State state = new State();
        state.setCode("28");
        state.setName("Andhra Pradesh");
        District dist = new District();
        dist.setState(state);
        dist.setCode("502");
        dist.setName("Ananthapuramu");
        Mandal mandal = new Mandal();
        mandal.setDistrict(dist);
        mandal.setCode("5032");
        mandal.setName("Atmakur");

        Village village = new Village();
        village.setMandal(mandal);
        village.setCode("597123");
        village.setName("FarAwayVillage");
        village.setLatitude(new BigDecimal("14.620000")); // ~13.3 km away
        village.setLongitude(new BigDecimal("77.520000"));
        village.setActive(true);

        when(villageRepository.findActiveWithinBoundingBox(any(), any(), any(), any()))
                .thenReturn(List.of(village));

        LocationResolutionDto res = locationService.resolveCandidateLocation(queryLat, queryLon);

        // Must reject far-away village and fall back to state-level
        assertThat(res.confidence()).isEqualTo("STATE_APPROXIMATE");
        assertThat(res.candidateVillageCode()).isNull();
        assertThat(res.candidateDistrictCode()).isNull();
    }

    @Test
    @DisplayName("Distance Calculation: Haversine distance matches known geodesic distances")
    void testHaversineDistanceCalculation() {
        // Hyderabad (Charminar: 17.3616, 78.4747) to Secunderabad (Clock Tower: 17.4399, 78.4983)
        double distanceKm = GeographyLocationService.haversineDistanceKm(17.3616, 78.4747, 17.4399, 78.4983);
        // Known geodesic distance is approximately 9.0 km
        assertThat(distanceKm).isBetween(8.5, 9.5);
    }
}
