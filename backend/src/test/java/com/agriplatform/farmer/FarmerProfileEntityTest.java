package com.agriplatform.farmer;

import com.agriplatform.farmer.entity.FarmerProfile;
import com.agriplatform.geography.entity.Village;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FarmerProfile Entity Unit Tests")
class FarmerProfileEntityTest {

    @Test
    @DisplayName("Default entity attributes and version calculation")
    void testDefaultAttributesAndVersion() {
        FarmerProfile profile = new FarmerProfile();
        UUID id = UUID.randomUUID();
        profile.setId(id);
        profile.setFullName("Ramesh Kumar");

        assertThat(profile.getId()).isEqualTo(id);
        assertThat(profile.getFullName()).isEqualTo("Ramesh Kumar");
        assertThat(profile.getProfileStatus()).isEqualTo(FarmerProfile.STATUS_ACTIVE);
        assertThat(profile.isOnboardingCompleted()).isFalse();
        assertThat(profile.getVersion()).isEqualTo(0L);

        OffsetDateTime now = OffsetDateTime.now();
        profile.setUpdatedAt(now);
        assertThat(profile.getVersion()).isEqualTo(now.toInstant().toEpochMilli());
    }

    @Test
    @DisplayName("Constructor initializes active status and onboarding flag")
    void testConstructorInitialization() {
        UUID id = UUID.randomUUID();
        Village village = new Village();
        village.setId(UUID.randomUUID());

        FarmerProfile profile = new FarmerProfile(id, "Suresh Patel", village);

        assertThat(profile.getId()).isEqualTo(id);
        assertThat(profile.getFullName()).isEqualTo("Suresh Patel");
        assertThat(profile.getHomeVillage()).isEqualTo(village);
        assertThat(profile.getProfileStatus()).isEqualTo("ACTIVE");
        assertThat(profile.isOnboardingCompleted()).isFalse();
    }
}
