package com.agriplatform.geography.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code public.mandals} table in Supabase.
 *
 * <h2>Schema (frozen — do not modify)</h2>
 * <pre>
 *   id           uuid        PK
 *   district_id  uuid        FK → districts.id  NOT NULL
 *   name         text        NOT NULL
 *   code         text        nullable
 *   is_active    bool        NOT NULL
 *   created_at   timestamptz NOT NULL
 *   updated_at   timestamptz NOT NULL
 * </pre>
 *
 * <p>Relationship: Many mandals belong to one district.
 * Uses {@code @ManyToOne(fetch = LAZY)} to avoid eager-loading and N+1.
 */
@Entity
@Table(name = "mandals", schema = "public")
public class Mandal {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "district_id", nullable = false, updatable = false)
    private District district;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code")
    private String code;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Mandal() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public District getDistrict() { return district; }
    public void setDistrict(District district) { this.district = district; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
