package com.agriplatform.geography.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code public.districts} table in Supabase.
 *
 * <h2>Schema (frozen — do not modify)</h2>
 * <pre>
 *   id          uuid        PK
 *   state_id    uuid        FK → states.id  NOT NULL
 *   name        text        NOT NULL
 *   code        text        nullable
 *   is_active   bool        NOT NULL
 *   created_at  timestamptz NOT NULL
 *   updated_at  timestamptz NOT NULL
 * </pre>
 *
 * <p>Relationship: Many districts belong to one state.
 * Uses {@code @ManyToOne(fetch = LAZY)} to avoid N+1 and eager-loading issues.
 */
@Entity
@Table(name = "districts", schema = "public")
public class District {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "state_id", nullable = false, updatable = false)
    private State state;

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

    public District() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public State getState() { return state; }
    public void setState(State state) { this.state = state; }

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
