package com.agriplatform.geography.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code public.states} table in Supabase.
 *
 * <h2>Schema (frozen — do not modify)</h2>
 * <pre>
 *   id         uuid        PK
 *   name       text        NOT NULL
 *   code       text        NOT NULL
 *   is_active  bool        NOT NULL
 *   created_at timestamptz NOT NULL
 *   updated_at timestamptz NOT NULL
 * </pre>
 *
 * <p>This entity is read-only. No INSERT / UPDATE / DELETE is performed
 * through this entity. {@code ddl-auto=validate} verifies the mapping
 * matches the live schema at startup in the dev profile.
 *
 * <p>Explicit getters/setters are used instead of Lombok for compatibility
 * with Java 25 (Lombok annotation processing issues with Java 25).
 */
@Entity
@Table(name = "states", schema = "public")
public class State {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public State() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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
