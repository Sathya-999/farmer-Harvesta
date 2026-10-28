package com.agriplatform.cultivation.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code public.seasons} reference table in Supabase.
 *
 * <h2>Schema (frozen)</h2>
 * <pre>
 *   id           uuid        PK DEFAULT gen_random_uuid()
 *   name         text        NOT NULL UNIQUE (lower(btrim(name)))
 *   anchor_month smallint    nullable CHECK (anchor_month BETWEEN 1 AND 12)
 *   anchor_day   smallint    nullable CHECK (anchor_day BETWEEN 1 AND 31)
 *   description  text        nullable
 *   is_active    boolean     NOT NULL DEFAULT true
 *   created_at   timestamptz NOT NULL DEFAULT now()
 *   updated_at   timestamptz NOT NULL DEFAULT now()
 * </pre>
 */
@Entity
@Table(name = "seasons", schema = "public")
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "anchor_month")
    private Short anchorMonth;

    @Column(name = "anchor_day")
    private Short anchorDay;

    @Column(name = "description")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Season() {
    }

    public Season(UUID id, String name, Short anchorMonth, Short anchorDay, String description, boolean isActive) {
        this.id = id;
        this.name = name;
        this.anchorMonth = anchorMonth;
        this.anchorDay = anchorDay;
        this.description = description;
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

    public Short getAnchorMonth() {
        return anchorMonth;
    }

    public void setAnchorMonth(Short anchorMonth) {
        this.anchorMonth = anchorMonth;
    }

    public Short getAnchorDay() {
        return anchorDay;
    }

    public void setAnchorDay(Short anchorDay) {
        this.anchorDay = anchorDay;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
