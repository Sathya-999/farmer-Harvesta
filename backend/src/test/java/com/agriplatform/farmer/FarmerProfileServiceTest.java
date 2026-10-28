package com.agriplatform.farmer;

import com.agriplatform.common.exception.BadRequestException;
import com.agriplatform.common.exception.ConflictException;
import com.agriplatform.common.exception.ResourceNotFoundException;
import com.agriplatform.common.util.SecurityUtils;
import com.agriplatform.farmer.dto.CreateFarmerProfileRequestDto;
import com.agriplatform.farmer.dto.FarmerProfileResponseDto;
import com.agriplatform.farmer.dto.UpdateFarmerProfileRequestDto;
import com.agriplatform.farmer.entity.FarmerProfile;
import com.agriplatform.farmer.mapper.FarmerProfileMapper;
import com.agriplatform.farmer.repository.FarmerProfileRepository;
import com.agriplatform.farmer.service.FarmerProfileServiceImpl;
import com.agriplatform.geography.entity.District;
import com.agriplatform.geography.entity.Mandal;
import com.agriplatform.geography.entity.State;
import com.agriplatform.geography.entity.Village;
import com.agriplatform.geography.repository.VillageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FarmerProfile Service Business Logic Tests")
class FarmerProfileServiceTest {

    @Mock
    private FarmerProfileRepository farmerProfileRepository;

    @Mock
    private VillageRepository villageRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Spy
    private FarmerProfileMapper farmerProfileMapper = new FarmerProfileMapper();

    @InjectMocks
    private FarmerProfileServiceImpl farmerProfileService;

    private UUID userId;
    private State state;
    private District district;
    private Mandal mandal;
    private Village village;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        when(securityUtils.getAuthenticatedUserId()).thenReturn(userId);

        state = new State();
        state.setId(UUID.randomUUID());
        state.setName("Andhra Pradesh");
        state.setActive(true);

        district = new District();
        district.setId(UUID.randomUUID());
        district.setName("Chittoor");
        district.setState(state);
        district.setActive(true);

        mandal = new Mandal();
        mandal.setId(UUID.randomUUID());
        mandal.setName("Chittoor Urban");
        mandal.setDistrict(district);
        mandal.setActive(true);

        village = new Village();
        village.setId(UUID.randomUUID());
        village.setName("Mapakshi");
        village.setMandal(mandal);
        village.setActive(true);
    }

    // =========================================================================
    // GET PROFILE
    // =========================================================================

    @Test
    @DisplayName("getProfile: returns profile when found")
    void testGetProfileSuccess() {
        FarmerProfile profile = new FarmerProfile();
        profile.setId(userId);
        profile.setFullName("Ramesh Kumar");
        profile.setHomeVillage(village);
        profile.setProfileStatus("ACTIVE");
        profile.setOnboardingCompleted(true);
        OffsetDateTime now = OffsetDateTime.now();
        profile.setCreatedAt(now);
        profile.setUpdatedAt(now);

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.of(profile));

        FarmerProfileResponseDto dto = farmerProfileService.getProfile();

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(userId);
        assertThat(dto.fullName()).isEqualTo("Ramesh Kumar");
        assertThat(dto.homeVillageId()).isEqualTo(village.getId());
        assertThat(dto.villageName()).isEqualTo("Mapakshi");
        assertThat(dto.mandalName()).isEqualTo("Chittoor Urban");
        assertThat(dto.districtName()).isEqualTo("Chittoor");
        assertThat(dto.stateName()).isEqualTo("Andhra Pradesh");
    }

    @Test
    @DisplayName("getProfile: throws ResourceNotFoundException when profile does not exist")
    void testGetProfileNotFound() {
        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> farmerProfileService.getProfile())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Farmer profile not found");
    }

    // =========================================================================
    // INIT PROFILE
    // =========================================================================

    @Test
    @DisplayName("initProfile: creates new profile when none exists")
    void testInitProfileWhenNoExistingProfile() {
        CreateFarmerProfileRequestDto req = new CreateFarmerProfileRequestDto(
                "Ramesh Kumar", village.getId(), mandal.getId(), district.getId(), state.getId()
        );

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.empty());
        when(villageRepository.findByIdWithHierarchy(village.getId())).thenReturn(Optional.of(village));
        when(farmerProfileRepository.saveAndFlush(any(FarmerProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FarmerProfileResponseDto dto = farmerProfileService.initProfile(req);

        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo(userId);
        assertThat(dto.fullName()).isEqualTo("Ramesh Kumar");
        assertThat(dto.onboardingCompleted()).isTrue();

        ArgumentCaptor<FarmerProfile> captor = ArgumentCaptor.forClass(FarmerProfile.class);
        verify(farmerProfileRepository).saveAndFlush(captor.capture());
        FarmerProfile saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(userId);
        assertThat(saved.getFullName()).isEqualTo("Ramesh Kumar");
        assertThat(saved.getHomeVillage()).isEqualTo(village);
        assertThat(saved.isOnboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("initProfile: completes onboarding on skeleton row from auth trigger")
    void testInitProfileOnSkeletonRow() {
        FarmerProfile skeleton = new FarmerProfile();
        skeleton.setId(userId);
        skeleton.setProfileStatus("ACTIVE");
        skeleton.setOnboardingCompleted(false);

        CreateFarmerProfileRequestDto req = new CreateFarmerProfileRequestDto(
                "Suresh Patel", village.getId(), null, null, null
        );

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.of(skeleton));
        when(villageRepository.findByIdWithHierarchy(village.getId())).thenReturn(Optional.of(village));
        when(farmerProfileRepository.saveAndFlush(any(FarmerProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FarmerProfileResponseDto dto = farmerProfileService.initProfile(req);

        assertThat(dto.fullName()).isEqualTo("Suresh Patel");
        assertThat(dto.onboardingCompleted()).isTrue();
        assertThat(skeleton.isOnboardingCompleted()).isTrue();
    }

    @Test
    @DisplayName("initProfile: throws ConflictException when already completed onboarding")
    void testInitProfileAlreadyCompletedThrowsConflict() {
        FarmerProfile existing = new FarmerProfile();
        existing.setId(userId);
        existing.setOnboardingCompleted(true);

        CreateFarmerProfileRequestDto req = new CreateFarmerProfileRequestDto(
                "Ramesh Kumar", village.getId(), null, null, null
        );

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> farmerProfileService.initProfile(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already been initialized");
    }

    @Test
    @DisplayName("initProfile: throws ResourceNotFoundException when village does not exist")
    void testInitProfileVillageNotFound() {
        UUID nonExistentVillageId = UUID.randomUUID();
        CreateFarmerProfileRequestDto req = new CreateFarmerProfileRequestDto(
                "Ramesh", nonExistentVillageId, null, null, null
        );

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.empty());
        when(villageRepository.findByIdWithHierarchy(nonExistentVillageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> farmerProfileService.initProfile(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Village not found");
    }

    @Test
    @DisplayName("initProfile: throws BadRequestException when village is inactive")
    void testInitProfileInactiveVillage() {
        village.setActive(false);
        CreateFarmerProfileRequestDto req = new CreateFarmerProfileRequestDto(
                "Ramesh", village.getId(), null, null, null
        );

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.empty());
        when(villageRepository.findByIdWithHierarchy(village.getId())).thenReturn(Optional.of(village));

        assertThatThrownBy(() -> farmerProfileService.initProfile(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Village is inactive");
    }

    @Test
    @DisplayName("initProfile: throws BadRequestException when mandal is inactive")
    void testInitProfileInactiveMandal() {
        mandal.setActive(false);
        CreateFarmerProfileRequestDto req = new CreateFarmerProfileRequestDto(
                "Ramesh", village.getId(), null, null, null
        );

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.empty());
        when(villageRepository.findByIdWithHierarchy(village.getId())).thenReturn(Optional.of(village));

        assertThatThrownBy(() -> farmerProfileService.initProfile(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inactive mandal");
    }

    @Test
    @DisplayName("initProfile: throws BadRequestException when district is inactive")
    void testInitProfileInactiveDistrict() {
        district.setActive(false);
        CreateFarmerProfileRequestDto req = new CreateFarmerProfileRequestDto(
                "Ramesh", village.getId(), null, null, null
        );

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.empty());
        when(villageRepository.findByIdWithHierarchy(village.getId())).thenReturn(Optional.of(village));

        assertThatThrownBy(() -> farmerProfileService.initProfile(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inactive district");
    }

    @Test
    @DisplayName("initProfile: throws BadRequestException when state is inactive")
    void testInitProfileInactiveState() {
        state.setActive(false);
        CreateFarmerProfileRequestDto req = new CreateFarmerProfileRequestDto(
                "Ramesh", village.getId(), null, null, null
        );

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.empty());
        when(villageRepository.findByIdWithHierarchy(village.getId())).thenReturn(Optional.of(village));

        assertThatThrownBy(() -> farmerProfileService.initProfile(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("inactive state");
    }

    @Test
    @DisplayName("initProfile: throws BadRequestException on mismatched mandalId")
    void testInitProfileMismatchedMandal() {
        UUID wrongMandalId = UUID.randomUUID();
        CreateFarmerProfileRequestDto req = new CreateFarmerProfileRequestDto(
                "Ramesh", village.getId(), wrongMandalId, null, null
        );

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.empty());
        when(villageRepository.findByIdWithHierarchy(village.getId())).thenReturn(Optional.of(village));

        assertThatThrownBy(() -> farmerProfileService.initProfile(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match village mandal");
    }

    @Test
    @DisplayName("initProfile: throws BadRequestException on mismatched districtId")
    void testInitProfileMismatchedDistrict() {
        UUID wrongDistrictId = UUID.randomUUID();
        CreateFarmerProfileRequestDto req = new CreateFarmerProfileRequestDto(
                "Ramesh", village.getId(), mandal.getId(), wrongDistrictId, null
        );

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.empty());
        when(villageRepository.findByIdWithHierarchy(village.getId())).thenReturn(Optional.of(village));

        assertThatThrownBy(() -> farmerProfileService.initProfile(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match village district");
    }

    @Test
    @DisplayName("initProfile: throws BadRequestException on mismatched stateId")
    void testInitProfileMismatchedState() {
        UUID wrongStateId = UUID.randomUUID();
        CreateFarmerProfileRequestDto req = new CreateFarmerProfileRequestDto(
                "Ramesh", village.getId(), mandal.getId(), district.getId(), wrongStateId
        );

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.empty());
        when(villageRepository.findByIdWithHierarchy(village.getId())).thenReturn(Optional.of(village));

        assertThatThrownBy(() -> farmerProfileService.initProfile(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not match village state");
    }

    // =========================================================================
    // UPDATE PROFILE & OPTIMISTIC CONCURRENCY
    // =========================================================================

    @Test
    @DisplayName("updateProfile: successfully updates when version matches")
    void testUpdateProfileSuccess() {
        OffsetDateTime originalTime = OffsetDateTime.now().minusMinutes(5);
        Long currentVersion = originalTime.toInstant().toEpochMilli();

        FarmerProfile profile = new FarmerProfile();
        profile.setId(userId);
        profile.setFullName("Old Name");
        profile.setHomeVillage(village);
        profile.setProfileStatus("ACTIVE");
        profile.setOnboardingCompleted(true);
        profile.setUpdatedAt(originalTime);

        UpdateFarmerProfileRequestDto req = new UpdateFarmerProfileRequestDto(
                "New Name", null, null, null, null, null, currentVersion
        );

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.of(profile));
        when(farmerProfileRepository.saveAndFlush(any(FarmerProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        FarmerProfileResponseDto dto = farmerProfileService.updateProfile(req);

        assertThat(dto.fullName()).isEqualTo("New Name");
        assertThat(profile.getFullName()).isEqualTo("New Name");
    }

    @Test
    @DisplayName("updateProfile: throws ConflictException when expectedVersion does not match (optimistic lock)")
    void testUpdateProfileVersionConflict() {
        OffsetDateTime currentTime = OffsetDateTime.now();
        Long currentVersion = currentTime.toInstant().toEpochMilli();
        Long staleVersion = currentVersion - 10000;

        FarmerProfile profile = new FarmerProfile();
        profile.setId(userId);
        profile.setFullName("Current Name");
        profile.setProfileStatus("ACTIVE");
        profile.setUpdatedAt(currentTime);

        UpdateFarmerProfileRequestDto req = new UpdateFarmerProfileRequestDto(
                "Stale Update", null, null, null, null, null, staleVersion
        );

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> farmerProfileService.updateProfile(req))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Optimistic lock conflict");
    }

    @Test
    @DisplayName("updateProfile: throws BadRequestException when profile is SUSPENDED")
    void testUpdateProfileSuspendedThrowsBadRequest() {
        OffsetDateTime now = OffsetDateTime.now();
        FarmerProfile profile = new FarmerProfile();
        profile.setId(userId);
        profile.setProfileStatus("SUSPENDED");
        profile.setUpdatedAt(now);

        UpdateFarmerProfileRequestDto req = new UpdateFarmerProfileRequestDto(
                "Update Name", null, null, null, null, null, now.toInstant().toEpochMilli()
        );

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> farmerProfileService.updateProfile(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("status is SUSPENDED");
    }

    @Test
    @DisplayName("updateProfile: throws BadRequestException when full name is blank")
    void testUpdateProfileBlankName() {
        OffsetDateTime now = OffsetDateTime.now();
        FarmerProfile profile = new FarmerProfile();
        profile.setId(userId);
        profile.setProfileStatus("ACTIVE");
        profile.setUpdatedAt(now);

        UpdateFarmerProfileRequestDto req = new UpdateFarmerProfileRequestDto(
                "   ", null, null, null, null, null, now.toInstant().toEpochMilli()
        );

        when(farmerProfileRepository.findByIdWithGeography(userId)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> farmerProfileService.updateProfile(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Full name cannot be blank");
    }
}
