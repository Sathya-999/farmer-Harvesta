package com.agriplatform.cultivation.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code public.crop_varieties} reference table in Supabase.
 *
 * <h2>Schema (frozen)</h2>
 * <pre>
 *   id         uuid        PK DEFAULT gen_random_uuid()
 *   crop_id    uuid        NOT NULL FK → crops(id) ON DELETE RESTRICT
 *   name       text        NOT NULL
 *   is_active  boolean     NOT NULL DEFAULT true
 *   created_at timestamptz NOT NULL DEFAULT now()
 *   updated_at timestamptz NOT NULL DEFAULT now()
 *   UNIQUE (crop_id, lower(btrim(name)))
 *   UNIQUE (id, crop_id)
 * </pre>
 */
@Entity
@Table(name = "crop_varieties", schema = "public")
public class CropVariety {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public CropVariety() {
    }

    public CropVariety(UUID id, Crop crop, String name, boolean isActive) {
        this.id = id;
        this.crop = crop;
        this.name = name;
        this.isActive = isActive;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Crop getCrop() {
        return crop;
    }

    public void setCrop(Crop crop) {
        this.crop = crop;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
