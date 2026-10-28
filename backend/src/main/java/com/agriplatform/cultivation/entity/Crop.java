package com.agriplatform.cultivation.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code public.crops} reference table in Supabase.
 *
 * <h2>Schema (frozen)</h2>
 * <pre>
 *   id         uuid        PK DEFAULT gen_random_uuid()
 *   name       text        NOT NULL UNIQUE (lower(btrim(name)))
 *   category   text        nullable
 *   is_active  boolean     NOT NULL DEFAULT true
 *   created_at timestamptz NOT NULL DEFAULT now()
 *   updated_at timestamptz NOT NULL DEFAULT now()
 * </pre>
 */
@Entity
@Table(name = "crops", schema = "public")
public class Crop {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category")
    private String category;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Crop() {
    }

    public Crop(UUID id, String name, String category, boolean isActive) {
        this.id = id;
        this.name = name;
        this.category = category;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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
