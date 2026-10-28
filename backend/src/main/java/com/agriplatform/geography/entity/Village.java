package com.agriplatform.geography.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA entity mapping the {@code public.villages} table in Supabase.
 *
 * <h2>Schema (frozen — do not modify)</h2>
 * <pre>
 *   id          uuid        PK
 *   mandal_id   uuid        FK → mandals.id  NOT NULL
 *   name        text        NOT NULL
 *   code        text        nullable
 *   latitude    numeric     nullable
 *   longitude   numeric     nullable
 *   is_active   bool        NOT NULL
 *   created_at  timestamptz NOT NULL
 *   updated_at  timestamptz NOT NULL
 * </pre>
 *
 * <p>Relationship: Many villages belong to one mandal.
 * Uses {@code @ManyToOne(fetch = LAZY)} to avoid eager-loading and N+1.
 */
@Entity
@Table(name = "villages", schema = "public")
public class Village {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mandal_id", nullable = false, updatable = false)
    private Mandal mandal;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "code")
    private String code;

    /** Geographic latitude. Nullable. Postgres {@code numeric} → {@link BigDecimal}. */
    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    /** Geographic longitude. Nullable. Postgres {@code numeric} → {@link BigDecimal}. */
    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Village() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Mandal getMandal() { return mandal; }
    public void setMandal(Mandal mandal) { this.mandal = mandal; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }

    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
