package com.agriplatform.farmer.entity;

import com.agriplatform.geography.entity.Village;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code public.farmer_profiles} table in Supabase.
 *
 * <h2>Schema (frozen — do not modify)</h2>
 * <pre>
 *   id                   uuid        PK (references auth.users(id) ON DELETE CASCADE)
 *   full_name            text        nullable (CHECK: full_name IS NULL OR btrim(full_name) <> '')
 *   home_village_id      uuid        FK → villages.id (ON DELETE SET NULL)
 *   profile_status       text        NOT NULL DEFAULT 'ACTIVE' (CHECK: IN ('ACTIVE', 'SUSPENDED', 'DELETED'))
 *   onboarding_completed bool        NOT NULL DEFAULT false
 *   created_at           timestamptz NOT NULL DEFAULT now()
 *   updated_at           timestamptz NOT NULL DEFAULT now()
 * </pre>
 *
 * <h2>Identity & Concurrency</h2>
 * <ul>
 *   <li>The entity ID matches the Supabase authenticated user UUID (from JWT {@code sub} claim).</li>
 *   <li>Optimistic concurrency is backed by {@link #getUpdatedAt()}, converted to epoch milliseconds
 *       as the version token.</li>
 * </ul>
 */
@Entity
@Table(name = "farmer_profiles", schema = "public")
public class FarmerProfile {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_SUSPENDED = "SUSPENDED";
    public static final String STATUS_DELETED = "DELETED";

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "full_name")
    private String fullName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_village_id")
    private Village homeVillage;

    @Column(name = "profile_status", nullable = false)
    private String profileStatus = STATUS_ACTIVE;

    @Column(name = "onboarding_completed", nullable = false)
    private boolean onboardingCompleted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public FarmerProfile() {}

    public FarmerProfile(UUID id, String fullName, Village homeVillage) {
        this.id = id;
        this.fullName = fullName;
        this.homeVillage = homeVillage;
        this.profileStatus = STATUS_ACTIVE;
        this.onboardingCompleted = false;
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (profileStatus == null) {
            profileStatus = STATUS_ACTIVE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    /**
     * Returns the optimistic locking version derived from {@code updated_at} epoch milliseconds.
     */
    public Long getVersion() {
        return updatedAt != null ? updatedAt.toInstant().toEpochMilli() : 0L;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Village getHomeVillage() { return homeVillage; }
    public void setHomeVillage(Village homeVillage) { this.homeVillage = homeVillage; }

    public String getProfileStatus() { return profileStatus; }
    public void setProfileStatus(String profileStatus) { this.profileStatus = profileStatus; }

    public boolean isOnboardingCompleted() { return onboardingCompleted; }
    public void setOnboardingCompleted(boolean onboardingCompleted) { this.onboardingCompleted = onboardingCompleted; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
